package com.visitor.ai;

import com.visitor.common.constant.AppointmentStatus;
import com.visitor.entity.Appointment;
import com.visitor.mapper.AppointmentMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * 自研 TF-IDF 向量知识库 v2.0 — 全 JDK 实现，零外部依赖。
 * 增量 IDF + 读写锁 + 分词缓存 + 动态阈值 + TopK 堆 + 手机号/姓名索引。
 */
@Slf4j
@Component
public class RiskVectorStore {

    private volatile double phoneMatchWeight = 0.5;
    private volatile double nameMatchWeight = 0.2;
    private volatile double companyFuzzyWeight = 0.1;
    // 阈值下限提高，过滤 2-gram 通用词产生的低质量假阳性匹配
    private static final double BASE_THRESHOLD = 0.15;
    private static final double THRESHOLD_SCALE = 0.01;
    private static final double MIN_THRESHOLD = 0.10;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile List<RiskDoc> docs = Collections.emptyList();
    private final ConcurrentHashMap<String, Double> idf = new ConcurrentHashMap<>();
    private volatile int docCount = 0;
    private final ConcurrentHashMap<String, List<Integer>> phoneIndex = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<Integer>> nameIndex = new ConcurrentHashMap<>();
    // LRU 分词缓存：accessOrder=true，超限时淘汰最旧条目而非全清
    private final Map<String, List<String>> tokenCache = java.util.Collections.synchronizedMap(
            new LinkedHashMap<>(256, 0.75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<String, List<String>> eldest) {
                    return size() > TOKEN_CACHE_MAX;
                }
            });
    private static final int TOKEN_CACHE_MAX = 5000;
    private final ConcurrentHashMap<String, Integer> docFreq = new ConcurrentHashMap<>();
    private final AppointmentMapper appointmentMapper;

    public RiskVectorStore(AppointmentMapper appointmentMapper) { this.appointmentMapper = appointmentMapper; }

    @PostConstruct public void init() { reload(); }

    public void setWeights(double pw, double nw, double cw) { phoneMatchWeight = pw; nameMatchWeight = nw; companyFuzzyWeight = cw; }
    public int size() { return docCount; }
    public double getPhoneMatchWeight() { return phoneMatchWeight; }
    public double getNameMatchWeight() { return nameMatchWeight; }
    public double getCompanyFuzzyWeight() { return companyFuzzyWeight; }

    // === 增量新增 ===
    public void add(Appointment a) {
        String text = buildText(a);
        List<String> tokens = tokenizeCached(text);
        if (tokens.isEmpty()) return;
        lock.writeLock().lock();
        try {
            Map<String, Double> vector = computeTfIdfIncremental(tokens);
            int newN = docCount + 1;
            Set<String> ut = new HashSet<>(tokens);
            for (String t : ut) { int nd = docFreq.merge(t, 1, Integer::sum); idf.put(t, Math.log(1.0 + newN / (1.0 + nd))); }
            RiskDoc doc = new RiskDoc(a.getId(), a.getVisitorName(), a.getVisitorPhone(), a.getCompany(), a.getPurpose(), a.getRemark(), vector);
            List<RiskDoc> nd = new ArrayList<>(docs); nd.add(doc); docs = nd; docCount = newN;
            if (a.getVisitorPhone() != null && !a.getVisitorPhone().isBlank()) phoneIndex.computeIfAbsent(a.getVisitorPhone(), k -> new ArrayList<>()).add(docs.size() - 1);
            if (a.getVisitorName() != null && !a.getVisitorName().isBlank()) nameIndex.computeIfAbsent(a.getVisitorName(), k -> new ArrayList<>()).add(docs.size() - 1);
            log.info("[向量库] 增量新增 appointmentId={} 总数={}", a.getId(), docCount);
        } finally { lock.writeLock().unlock(); }
    }

    public void reload() {
        lock.writeLock().lock();
        try {
            List<Appointment> rejected = appointmentMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Appointment>()
                            .eq(Appointment::getStatus, AppointmentStatus.REJECTED).orderByDesc(Appointment::getId));
            if (rejected.isEmpty()) { docs = Collections.emptyList(); idf.clear(); docFreq.clear(); phoneIndex.clear(); nameIndex.clear(); docCount = 0; return; }
            List<RiskDoc> nd = new ArrayList<>(rejected.size()); docFreq.clear(); idf.clear(); phoneIndex.clear(); nameIndex.clear();
            List<List<String>> allTokens = new ArrayList<>();
            for (Appointment a : rejected) { List<String> toks = tokenizeCached(buildText(a)); allTokens.add(toks); for (String t : new HashSet<>(toks)) docFreq.merge(t, 1, Integer::sum); }
            int N = rejected.size();
            for (int i = 0; i < allTokens.size(); i++) {
                Appointment a = rejected.get(i);
                Map<String, Double> v = computeTfIdfRebuild(allTokens.get(i), N);
                nd.add(new RiskDoc(a.getId(), a.getVisitorName(), a.getVisitorPhone(), a.getCompany(), a.getPurpose(), a.getRemark(), v));
                if (a.getVisitorPhone() != null && !a.getVisitorPhone().isBlank()) phoneIndex.computeIfAbsent(a.getVisitorPhone(), k -> new ArrayList<>()).add(i);
                if (a.getVisitorName() != null && !a.getVisitorName().isBlank()) nameIndex.computeIfAbsent(a.getVisitorName(), k -> new ArrayList<>()).add(i);
            }
            docs = nd; docCount = N;
            log.info("[向量库] 重建完成：{}条", docCount);
        } finally { lock.writeLock().unlock(); }
    }

    // === 检索 ===
    public List<SearchResult> search(Appointment query, int topK) {
        long startNs = System.nanoTime();
        lock.readLock().lock();
        try {
            List<RiskDoc> snap = docs;
            if (snap.isEmpty()) return Collections.emptyList();
            String qText = buildText(query);
            Map<String, Double> qVector = computeTfIdfIncremental(tokenizeCached(qText));
            double th = computeDynamicThreshold();
            PriorityQueue<SearchResult> pq = new PriorityQueue<>(topK, (a, b) -> Double.compare(a.similarity, b.similarity));
            int checked = 0, skipped = 0; double tSim = 0;
            for (RiskDoc doc : snap) {
                checked++;
                if (!hasIntersect(qVector, doc.vector())) { skipped++; continue; }
                double sim = cosineSimilarity(qVector, doc.vector());
                // 加权融合：bonus 越大越接近 1.0，保证 sim 始终 ∈ [0,1]，避免 >1 的语义错误
                double bonus = 0;
                if (query.getVisitorPhone() != null && query.getVisitorPhone().equals(doc.visitorPhone())) bonus += phoneMatchWeight;
                if (query.getVisitorName() != null && query.getVisitorName().equals(doc.visitorName())) bonus += nameMatchWeight;
                if (query.getCompany() != null && doc.company() != null && !query.getCompany().equals(doc.company()) && (doc.company().contains(query.getCompany()) || query.getCompany().contains(doc.company()))) bonus += companyFuzzyWeight;
                sim = sim + bonus * (1.0 - sim);
                if (sim < th) continue;
                SearchResult sr = new SearchResult(doc, sim);
                if (pq.size() < topK) { pq.offer(sr); tSim += sim; }
                else if (sim > pq.peek().similarity()) { tSim -= pq.poll().similarity(); pq.offer(sr); tSim += sim; }
            }
            List<SearchResult> results = new ArrayList<>(pq);
            results.sort((a, b) -> Double.compare(b.similarity, a.similarity));
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            double avg = results.isEmpty() ? 0 : tSim / results.size();
            log.info("[向量库] 检索 耗时={}ms 命中={} 扫描={} 跳过={} 平均sim={:.3f} 阈值={:.3f}", ms, results.size(), checked, skipped, avg, th);
            return results;
        } finally { lock.readLock().unlock(); }
    }

    public List<RiskDoc> findByPhone(String phone) {
        if (phone == null || phone.isBlank()) return Collections.emptyList();
        lock.readLock().lock();
        try {
            List<Integer> idxs = phoneIndex.get(phone.trim());
            if (idxs == null || idxs.isEmpty()) return Collections.emptyList();
            List<RiskDoc> snap = docs; List<RiskDoc> r = new ArrayList<>();
            for (int i : idxs) if (i < snap.size()) r.add(snap.get(i));
            return r;
        } finally { lock.readLock().unlock(); }
    }

    public List<RiskDoc> findByName(String name) {
        if (name == null || name.isBlank()) return Collections.emptyList();
        lock.readLock().lock();
        try {
            List<Integer> idxs = nameIndex.get(name.trim());
            if (idxs == null || idxs.isEmpty()) return Collections.emptyList();
            List<RiskDoc> snap = docs; List<RiskDoc> r = new ArrayList<>();
            for (int i : idxs) if (i < snap.size()) r.add(snap.get(i));
            return r;
        } finally { lock.readLock().unlock(); }
    }

    // === 内部方法 ===
    private double computeDynamicThreshold() { int N = docCount; if (N <= 1) return MIN_THRESHOLD; return Math.max(MIN_THRESHOLD, BASE_THRESHOLD - Math.log10(N) * THRESHOLD_SCALE); }
    private boolean hasIntersect(Map<String, Double> v1, Map<String, Double> v2) {
        if (v1.isEmpty() || v2.isEmpty()) return false;
        Map<String, Double> sm = v1.size() < v2.size() ? v1 : v2, lg = v1.size() < v2.size() ? v2 : v1;
        for (String t : sm.keySet()) if (lg.containsKey(t)) return true;
        return false;
    }
    private Map<String, Double> computeTfIdfIncremental(List<String> tokens) {
        if (tokens.isEmpty()) return Collections.emptyMap();
        Map<String, Integer> tf = new HashMap<>(); for (String t : tokens) tf.merge(t, 1, Integer::sum);
        Map<String, Double> v = new HashMap<>();
        for (Map.Entry<String, Integer> e : tf.entrySet()) v.put(e.getKey(), e.getValue() * idf.getOrDefault(e.getKey(), 0.0));
        return l2Norm(v);
    }
    private Map<String, Double> computeTfIdfRebuild(List<String> tokens, int N) {
        if (tokens.isEmpty()) return Collections.emptyMap();
        Map<String, Integer> tf = new HashMap<>(); for (String t : tokens) tf.merge(t, 1, Integer::sum);
        Map<String, Double> v = new HashMap<>();
        for (Map.Entry<String, Integer> e : tf.entrySet()) { double iv = Math.log(1.0 + N / (1.0 + docFreq.getOrDefault(e.getKey(), 0))); v.put(e.getKey(), e.getValue() * iv); idf.put(e.getKey(), iv); }
        return l2Norm(v);
    }
    private Map<String, Double> l2Norm(Map<String, Double> v) {
        if (v.isEmpty()) return v;
        double n = Math.sqrt(v.values().stream().mapToDouble(d -> d * d).sum());
        if (n > 0) v.replaceAll((k, val) -> val / n);
        return v;
    }
    private double cosineSimilarity(Map<String, Double> v1, Map<String, Double> v2) {
        Map<String, Double> sm = v1.size() < v2.size() ? v1 : v2, lg = v1.size() < v2.size() ? v2 : v1;
        double dot = 0;
        for (Map.Entry<String, Double> e : sm.entrySet()) { Double o = lg.get(e.getKey()); if (o != null) dot += e.getValue() * o; }
        return dot;
    }
    private String buildText(Appointment a) {
        StringBuilder sb = new StringBuilder();
        if (a.getVisitorName() != null) sb.append(a.getVisitorName());
        if (a.getCompany() != null) sb.append(' ').append(a.getCompany());
        if (a.getPurpose() != null) sb.append(' ').append(a.getPurpose());
        if (a.getVisitorPhone() != null) sb.append(" PHONE:").append(a.getVisitorPhone());
        if (a.getVisitorName() != null) sb.append(" NAME:").append(a.getVisitorName());
        return sb.toString();
    }
    public List<String> tokenizeCached(String text) {
        if (text == null || text.isBlank()) return Collections.emptyList();
        List<String> c = tokenCache.get(text); if (c != null) return c;
        List<String> toks = tokenize(text); tokenCache.put(text, toks); return toks;
    }
    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        for (String p : text.split("\\s+")) {
            if (p.startsWith("PHONE:") || p.startsWith("NAME:")) { tokens.add(p); continue; }
            for (int i = 0; i < p.length() - 1; i++) tokens.add(p.substring(i, i + 2));
            if (p.length() == 1) tokens.add(p);
        }
        return tokens;
    }

    public record RiskDoc(Integer appointmentId, String visitorName, String visitorPhone, String company, String purpose, String remark, Map<String, Double> vector) {}
    public record SearchResult(RiskDoc doc, double similarity) {}
}
