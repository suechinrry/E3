package com.visitor.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DeepSeek 大模型客户端 v2.0 — AI 话术生成 + 风险评估。
 *
 * 话术生成：Few-Shot Prompt + Redis/内存缓存(12h) + 按事由场景模板mock + 鲁棒JSON解析
 * 风险评估：RAG上下文预处理(≤3条) + 硬规则优先 + 429/超时分类容错 + 24h缓存
 */
@Slf4j
@Service
public class DeepSeekClient {

    // === 场景模板库 ===
    private static final List<String> LOCATIONS = List.of(
            "一楼大厅接待区", "二楼会议室A", "三楼贵宾室", "四楼洽谈室", "五楼总裁办公室");

    private static final Map<String, GreetingTemplate> SCENARIO_TEMPLATES = new LinkedHashMap<>();
    static {
        SCENARIO_TEMPLATES.put("签约", new GreetingTemplate(
                "尊敬的%s%s代表，感谢贵司信任并莅临签约。%s总已在%s恭候，祝合作圆满成功。",
                "请安排%s在%s主宾位就座，准备签约文件及签字笔。",
                "请前台在大屏展示欢迎词，提前调试投影设备。"));
        SCENARIO_TEMPLATES.put("合作", new GreetingTemplate(
                "热烈欢迎%s%s莅临我司洽谈合作。%s总期待与贵方深入交流。",
                "请安排%s在%s就座，准备PPT演示设备。",
                "请提前5分钟通知%s总到会议室迎接。"));
        SCENARIO_TEMPLATES.put("技术", new GreetingTemplate(
                "欢迎%s%s来访进行技术交流，我司技术团队已做好准备。",
                "请安排%s在%s就座，提前调试演示设备。",
                "确认白板笔、投影仪可用。"));
        SCENARIO_TEMPLATES.put("default", new GreetingTemplate(
                "欢迎%s%s莅临我司，%s将接待您。请前往%s与%s见面。",
                "请安排%s在%s就座，准备茶水接待。",
                "来访人员信息已通知被访人，请前台准备访客证。"));
    }

    private record GreetingTemplate(String greeting, String seat, String notes) {}

    // === 精简 Prompt（适配推理模型，无 Few-Shot 示例避免推理消耗 token） ===
    private static final String GREETING_PROMPT_TEMPLATE = """
        你是企业前台接待助手。直接输出纯JSON，不要任何解释和分析。
        为以下访客生成商务正式接待话术：
        访客：%s | 单位：%s | 事由：%s | 被访人：%s
        输出格式：{"greeting":"欢迎语≤80字","seatSuggestion":"座位建议≤60字","notes":"注意事项≤40字"}""";

    private static final String RISK_PROMPT = """
        企业风控助手：判断新预约风险，只输出纯JSON不解释。
        high：手机号匹配历史拒访/事由推销骚扰且单位不明
        medium：有相似案例但无精确匹配 low：无相似记录
        不要仅凭单位事由文本相同判high
        【新预约】姓名：%s | 手机：%s | 单位：%s | 事由：%s | 被访人：%s
        【相似案例】%s
        输出：{"riskLevel":"low|medium|high","riskScore":0-100,"reason":"据案例#1等情况，说明理由≤40字"}""";

    // === 字段 ===
    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final WebClient webClient;
    private final Duration callTimeout;
    private final GreetingCacheService cacheService;
    private final ConcurrentHashMap<String, RiskCacheEntry> riskCache = new ConcurrentHashMap<>();
    private static final long RISK_CACHE_TTL_MS = 24L * 60 * 60 * 1000;
    private static final int MAX_SIMILAR_CASES = 3;
    private static final int MAX_CASE_CHARS = 120;

    private static class RiskCacheEntry {
        final Map<String, Object> result;
        final long expireAt;
        RiskCacheEntry(Map<String, Object> r) { this.result = r; this.expireAt = System.currentTimeMillis() + RISK_CACHE_TTL_MS; }
        boolean expired() { return System.currentTimeMillis() > expireAt; }
    }

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = new com.fasterxml.jackson.databind.ObjectMapper();

    public DeepSeekClient(@Value("${deepseek.api-key}") String apiKey,
                          @Value("${deepseek.api-url}") String apiUrl,
                          @Value("${deepseek.model}") String model,
                          @Value("${deepseek.greeting.timeout-seconds:30}") int timeoutSec,
                          GreetingCacheService cacheService) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
        this.callTimeout = Duration.ofSeconds(timeoutSec);
        this.cacheService = cacheService;
        this.webClient = WebClient.create();
    }

    // ==================== 话术生成 ====================

    @SuppressWarnings("unchecked")
    public Map<String, String> generateGreeting(String visitorName, String company,
                                                 String purpose, String hostName) {
        long startNs = System.nanoTime();
        String nName = visitorName != null ? visitorName : "访客";
        String nComp = company != null ? company : "";
        String nPurp = purpose != null ? purpose : "来访";
        String nHost = hostName != null ? hostName : "未知";
        String cacheKey = cacheService.buildCacheKey(nName, nComp, nPurp, nHost);

        Map<String, String> cached = cacheService.get(cacheKey);
        if (cached != null) {
            cached.put("source", "cache");
            log.info("[话术] 缓存命中 visitor={} 耗时={}ms", nName, (System.nanoTime()-startNs)/1_000_000);
            return cached;
        }

        if (apiKey != null && !apiKey.isBlank()) {
            Map<String, String> ai = callAiGreeting(nName, nComp, nPurp, nHost);
            if (ai != null) {
                cacheService.put(cacheKey, ai);
                ai.put("source", "ai");
                log.info("[话术] AI生成成功 visitor={} 耗时={}ms", nName, (System.nanoTime()-startNs)/1_000_000);
                return ai;
            }
        }

        Map<String, String> mock = scenarioMock(nName, nComp, nPurp, nHost);
        mock.put("source", "mock");
        log.info("[话术] Mock模板生成 visitor={} 耗时={}ms", nName, (System.nanoTime()-startNs)/1_000_000);
        return mock;
    }

    private Map<String, String> callAiGreeting(String name, String company, String purpose, String host) {
        String prompt = String.format(GREETING_PROMPT_TEMPLATE, name, company, purpose, host);
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
            body.put("temperature", 0.7); body.put("max_tokens", 4096);
            Map<String, Object> resp = webClient.post().uri(apiUrl)
                    .header("Authorization", "Bearer " + apiKey).header("Content-Type", "application/json")
                    .bodyValue(body).retrieve().bodyToMono(Map.class)
                    .timeout(callTimeout).onErrorResume(e -> { log.error("[话术] HTTP异常: {}", e.getMessage()); return Mono.empty(); }).block();
            if (resp != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) resp.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Object msgObj = choices.get(0).get("message");
                    if (msgObj instanceof Map) {
                        String content = (String) ((Map<?, ?>) msgObj).get("content");
                        // 推理模型：content可能为空（token被reasoning_content耗尽），检查reasoning_content
                        if (content == null || content.isBlank()) {
                            Object reasoning = ((Map<?, ?>) msgObj).get("reasoning_content");
                            log.warn("[话术] content为空！推理模型token不足，reasoning_content首100字: {}",
                                    reasoning != null ? String.valueOf(reasoning).substring(0, Math.min(100, String.valueOf(reasoning).length())) : "null");
                        } else {
                            log.info("[话术] AI返回原始内容(len={}): {}", content.length(),
                                    content.substring(0, Math.min(200, content.length())));
                        }
                        Map<String, String> parsed = parseRobustJson(content);
                        if (parsed != null) return parsed;
                        log.warn("[话术] JSON解析失败，原始内容首200字: {}", content != null ? content.substring(0, Math.min(200, content.length())) : "null");
                    } else {
                        log.warn("[话术] message字段类型异常: {}", msgObj != null ? msgObj.getClass().getName() : "null");
                    }
                } else {
                    log.warn("[话术] API返回空choices");
                }
            } else {
                log.warn("[话术] API返回null响应");
            }
        } catch (Exception e) { log.error("[话术] 异常 name={} err={}", name, e.getMessage()); }
        return null;
    }

    private Map<String, String> scenarioMock(String name, String company, String purpose, String host) {
        GreetingTemplate t = SCENARIO_TEMPLATES.get("default");
        for (Map.Entry<String, GreetingTemplate> e : SCENARIO_TEMPLATES.entrySet()) {
            if (purpose != null && purpose.contains(e.getKey())) { t = e.getValue(); break; }
        }
        String loc = LOCATIONS.get(new Random().nextInt(LOCATIONS.size()));
        Map<String, String> r = new LinkedHashMap<>();
        r.put("greeting", String.format(t.greeting, company, name, host, loc));
        r.put("seatSuggestion", String.format(t.seat, name, loc));
        r.put("notes", String.format(t.notes, host));
        return r;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseRobustJson(String raw) {
        if (raw == null || raw.isBlank()) { log.warn("[话术] 解析失败: 内容为空"); return null; }
        String c = raw.replaceAll("(?i)```json|```", "").trim();
        // 推理模型可能先输出思考文本再输出JSON → 取最后一个 {"greeting" 位置
        int s = c.lastIndexOf("{\"greeting\"");
        if (s < 0) s = c.lastIndexOf("{\n\"greeting\"");
        if (s < 0) s = c.lastIndexOf('{');
        int e = c.lastIndexOf('}');
        if (s < 0 || e <= s) { log.warn("[话术] 解析失败: 未找到有效JSON括号 s={} e={}", s, e); return null; }
        String json = c.substring(s, e + 1);
        log.info("[话术] 提取JSON(len={}): {}", json.length(), json.substring(0, Math.min(300, json.length())));
        try {
            Map<String, String> m = MAPPER.readValue(json, Map.class);
            Map<String, String> r = new LinkedHashMap<>();
            r.put("greeting", m.getOrDefault("greeting", ""));
            r.put("seatSuggestion", m.getOrDefault("seatSuggestion", ""));
            r.put("notes", m.getOrDefault("notes", ""));
            if (r.get("greeting").isBlank()) { log.warn("[话术] 解析后greeting为空"); }
            return r;
        } catch (Exception ex) { log.warn("[话术] JSON反序列化异常: {}", ex.getMessage()); return null; }
    }

    public Map<String, String> regenerateGreeting(String n, String c, String p, String h) {
        cacheService.evict(cacheService.buildCacheKey(n, c, p, h));
        return generateGreeting(n, c, p, h);
    }

    // ==================== 风险评估 ====================

    @SuppressWarnings("unchecked")
    public Map<String, Object> assessRisk(com.visitor.entity.Appointment a, String similarCases, double avgSim) {
        long startNs = System.nanoTime();
        String name = a.getVisitorName(), phone = a.getVisitorPhone(), company = a.getCompany(), purpose = a.getPurpose();
        String preprocessed = preprocessCases(similarCases);
        int matchCount = countMatches(similarCases);

        // 缓存键包含访客姓名+手机号，避免不同访客同单位同事误命中他人缓存
        String ck = "risk:" + n(name) + "|" + n(phone) + "|" + n(company) + "|" + n(purpose) + "|" + matchCount;
        RiskCacheEntry ce = riskCache.get(ck);
        if (ce != null && !ce.expired()) {
            Map<String, Object> r = new LinkedHashMap<>(ce.result);
            r.put("source", "cache"); r.put("matchCaseCount", matchCount);
            log.info("[风险] 缓存命中 visitor={} matchCount={} 耗时={}ms", name, matchCount, (System.nanoTime()-startNs)/1_000_000);
            return r;
        }
        if (ce != null) riskCache.remove(ck);

        // 硬规则已由 AppointmentService 在 RAG 检索结果中检查，此处仅做 AI/ruleBased 评估

        if (apiKey == null || apiKey.isBlank()) {
            Map<String, Object> r = ruleBased(matchCount, avgSim);
            r.put("source", "mock"); r.put("matchCaseCount", matchCount);
            log.info("[风险] 规则兜底 visitor={} matchCount={}", name, matchCount);
            return r;
        }

        Map<String, Object> ai = callRiskAi(a, preprocessed);
        Map<String, Object> result;
        if (ai != null) { result = normalizeRisk(ai); result.put("source", "ai"); }
        else { result = ruleBased(matchCount, avgSim); result.put("source", "rule_fallback"); }
        result.put("matchCaseCount", matchCount);
        riskCache.put(ck, new RiskCacheEntry(new LinkedHashMap<>(result)));
        log.info("[风险] 评估完成 visitor={} level={} source={} 耗时={}ms", name, result.get("riskLevel"), result.get("source"), (System.nanoTime()-startNs)/1_000_000);
        return result;
    }

    private String preprocessCases(String raw) {
        if (raw == null || raw.isBlank()) return "空";
        String[] cases = raw.split("被拒预约#");
        StringBuilder sb = new StringBuilder(); int n = 0;
        for (String c : cases) {
            if (c.isBlank() || n >= MAX_SIMILAR_CASES) continue;
            String cl = c.replaceAll("\\s+", " ").replace("相似度", "sim").trim();
            if (cl.length() > MAX_CASE_CHARS) cl = cl.substring(0, MAX_CASE_CHARS) + "...";
            sb.append("#").append(n + 1).append(": ").append(cl).append("\n"); n++;
        }
        return n == 0 ? "空" : sb.toString().trim();
    }

    private int countMatches(String raw) { return raw == null || raw.isBlank() ? 0 : raw.split("被拒预约#").length - 1; }

    private Map<String, Object> callRiskAi(com.visitor.entity.Appointment a, String p) {
        String prompt = String.format(RISK_PROMPT, a.getVisitorName(), a.getVisitorPhone(), a.getCompany(), a.getPurpose(), a.getHostName(), p);
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model); body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
            body.put("temperature", 0.3); body.put("max_tokens", 2048);
            Map<String, Object> resp = webClient.post().uri(apiUrl)
                    .header("Authorization", "Bearer " + apiKey).header("Content-Type", "application/json")
                    .bodyValue(body).retrieve()
                    .onStatus(s -> s.value() == 429, r -> { throw new RuntimeException("RATE_LIMITED"); })
                    .bodyToMono(Map.class).timeout(callTimeout)
                    .onErrorResume(e -> { log.warn("[风险] HTTP异常({})→降级", e.getMessage() != null && e.getMessage().contains("RATE_LIMITED") ? "429" : "异常"); return Mono.empty(); })
                    .block();
            if (resp != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) resp.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Object msgObj = choices.get(0).get("message");
                    if (msgObj instanceof Map) {
                        String content = (String) ((Map<?, ?>) msgObj).get("content");
                        if (content == null || content.isBlank()) {
                            Object reasoning = ((Map<?, ?>) msgObj).get("reasoning_content");
                            log.warn("[风险] content为空！reasoning_content首80字: {}",
                                    reasoning != null ? String.valueOf(reasoning).substring(0, Math.min(80, String.valueOf(reasoning).length())) : "null");
                        }
                        Map<String, Object> parsed = parseRiskJson(content);
                        if (parsed != null) return parsed;
                        log.warn("[风险] JSON解析失败，content首200字: {}", content != null ? content.substring(0, Math.min(200, content.length())) : "null");
                    } else {
                        log.warn("[风险] message类型异常: {}", msgObj != null ? msgObj.getClass().getName() : "null");
                    }
                } else {
                    log.warn("[风险] API返回空choices");
                }
            } else {
                log.warn("[风险] API返回null响应");
            }
        } catch (Exception e) { log.error("[风险] 异常: {}", e.getMessage()); }
        return null;
    }

    private Map<String, Object> ruleBased(int matchCount, double avgSim) {
        Map<String, Object> r = new LinkedHashMap<>();
        // 规则兜底不自动判 high——仅基于文本相似度无法区分真伪（2-gram 通用词假阳性），
        // 真正的 high 应由硬规则（手机号/姓名精确匹配）或 AI 语义判断决定。
        if (matchCount >= 2) {
            int score = 40 + (int) Math.round(avgSim * 25);
            r.put("riskLevel", "medium"); r.put("riskScore", Math.min(65, score));
            r.put("reason", String.format("规则兜底：%d条被拒记录相似（平均相似度%.2f），建议被访人关注。", matchCount, avgSim));
        } else if (matchCount == 1) {
            int score = 30 + (int) Math.round(avgSim * 20);
            r.put("riskLevel", "medium"); r.put("riskScore", Math.min(55, score));
            r.put("reason", String.format("规则兜底：1条相似被拒记录（相似度%.2f），可正常审批。", avgSim));
        } else {
            r.put("riskLevel", "low"); r.put("riskScore", 12);
            r.put("reason", "规则兜底：无相似被拒记录，风险较低，正常流转。");
        }
        return r;
    }

    private Map<String, Object> normalizeRisk(Map<String, Object> raw) {
        Map<String, Object> r = new LinkedHashMap<>();
        String lv = String.valueOf(raw.getOrDefault("riskLevel", "low")).toLowerCase();
        r.put("riskLevel", lv.contains("high") ? "high" : lv.contains("medium") || lv.contains("mid") ? "medium" : "low");
        try { r.put("riskScore", Math.max(0, Math.min(100, Integer.parseInt(String.valueOf(raw.getOrDefault("riskScore", "0")))))); }
        catch (NumberFormatException e) { r.put("riskScore", 0); }
        r.put("reason", String.valueOf(raw.getOrDefault("reason", "")));
        return r;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseRiskJson(String content) {
        try {
            if (content == null || content.isBlank()) { log.warn("[风险] 解析失败: 内容为空"); return null; }
            String c = content.replaceAll("(?i)```json|```", "").trim();
            // 推理模型可能先输出分析再输出JSON → 取最后一个 {"riskLevel" 位置
            int s = c.lastIndexOf("{\"riskLevel\"");
            if (s < 0) s = c.lastIndexOf("{\n\"riskLevel\"");
            if (s < 0) s = c.lastIndexOf('{');
            int e = c.lastIndexOf('}');
            if (s < 0 || e <= s) { log.warn("[风险] 解析失败: 未找到有效JSON s={} e={}", s, e); return null; }
            log.info("[风险] AI返回JSON(len={}): {}", e - s + 1, c.substring(s, Math.min(s + 300, c.length())));
            return MAPPER.readValue(c.substring(s, e + 1), Map.class);
        } catch (Exception ex) { log.warn("[风险] JSON反序列化异常: {}", ex.getMessage()); return null; }
    }

    public void evictRiskCache(String name, String phone, String company, String purpose, int matchCount) {
        riskCache.remove("risk:" + n(name) + "|" + n(phone) + "|" + n(company) + "|" + n(purpose) + "|" + matchCount);
    }

    /** 清除全部风险缓存（拒绝预约/重建向量库后调用，保证后续评估基于最新知识库） */
    public void clearAllRiskCache() {
        int n = riskCache.size();
        riskCache.clear();
        log.info("[风险] 已清除全部风险缓存（{}条）", n);
    }

    private static String n(String s) { return s == null ? "" : s.trim().replaceAll("\\s+", ""); }
}
