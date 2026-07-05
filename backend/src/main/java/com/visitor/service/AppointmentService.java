package com.visitor.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.visitor.ai.DeepSeekClient;
import com.visitor.ai.RiskVectorStore;
import com.visitor.common.PageResult;
import com.visitor.common.constant.AppointmentStatus;
import com.visitor.common.exception.BusinessException;
import com.visitor.entity.Appointment;
import com.visitor.entity.Greeting;
import com.visitor.entity.RiskAssessment;
import com.visitor.entity.User;
import com.visitor.mapper.AppointmentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class AppointmentService extends ServiceImpl<AppointmentMapper, Appointment> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final GreetingService greetingService;
    private final DeepSeekClient deepSeekClient;
    private final UserNotificationService userNotificationService;
    private final RiskVectorStore riskVectorStore;
    private final RiskAssessmentService riskAssessmentService;
    private final UserService userService;

    public AppointmentService(GreetingService greetingService,
                              DeepSeekClient deepSeekClient,
                              UserNotificationService userNotificationService,
                              RiskVectorStore riskVectorStore,
                              RiskAssessmentService riskAssessmentService,
                              UserService userService) {
        this.greetingService = greetingService;
        this.deepSeekClient = deepSeekClient;
        this.userNotificationService = userNotificationService;
        this.riskVectorStore = riskVectorStore;
        this.riskAssessmentService = riskAssessmentService;
        this.userService = userService;
    }

    public PageResult<Appointment> pageMy(int userId, int page, int size, String status) {
        LambdaQueryWrapper<Appointment> qw = new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getVisitorId, userId)
                .eq(!"all".equals(status), Appointment::getStatus, status)
                .orderByDesc(Appointment::getId);
        IPage<Appointment> p = page(new Page<>(page, size), qw);
        return new PageResult<>(p.getRecords(), p.getTotal(), page, size);
    }

    public PageResult<Appointment> pageHost(int hostId, int page, int size, String status,
                                             LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<Appointment> qw = new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getHostId, hostId)
                .eq(!"all".equals(status), Appointment::getStatus, status)
                .orderByDesc(Appointment::getId);
        if (startDate != null) qw.ge(Appointment::getStartTime, startDate.atStartOfDay());
        if (endDate != null) qw.le(Appointment::getStartTime, endDate.plusDays(1).atStartOfDay());
        IPage<Appointment> p = page(new Page<>(page, size), qw);
        return new PageResult<>(p.getRecords(), p.getTotal(), page, size);
    }

    public List<Appointment> pendingList(int hostId) {
        return lambdaQuery()
                .eq(Appointment::getHostId, hostId)
                .eq(Appointment::getStatus, AppointmentStatus.PENDING)
                .orderByDesc(Appointment::getId)
                .list();
    }

    /**
     * 审批预约（核心事务：状态变更 + 发通知，同一事务保证一致性）。
     * AI 话术生成可能耗时，放在事务外异步触发。
     *
     * @param operatorId 操作人用户ID
     * @param role        操作人角色（host 仅能审批来访自己的预约）
     */
    @Transactional
    public void approve(Integer id, String status, String remark, Integer operatorId, String role) {
        Appointment a = getById(id);
        if (a == null) throw new BusinessException("预约不存在");
        if (!AppointmentStatus.PENDING.equals(a.getStatus())) {
            throw new BusinessException("当前状态不可审批");
        }
        // 权限校验：host 只能审批 hostId == 自己 的预约
        if ("host".equals(role) && !operatorId.equals(a.getHostId())) {
            throw new BusinessException("您只能审批来访自己的预约");
        }
        // 高风险锁定：host 不可审批高风险预约（需管理员处理）
        if ("host".equals(role) && "approved".equals(status)) {
            com.visitor.entity.RiskAssessment ra = riskAssessmentService.lambdaQuery()
                    .eq(com.visitor.entity.RiskAssessment::getAppointmentId, id)
                    .one();
            if (ra != null && "high".equals(ra.getRiskLevel())) {
                throw new BusinessException("该预约被AI评估为高风险，需管理员审核，您暂时无法审批通过。");
            }
        }

        a.setStatus(status);
        a.setRemark(remark);
        updateById(a);

        // 给访客发送审批结果通知（仅系统注册访客），与审批同事务
        if (a.getVisitorId() != null) {
            if (AppointmentStatus.APPROVED.equals(status)) {
                String content = String.format(
                        "您预约的【%s】已被%s审核通过，预计到访时间：%s。",
                        a.getPurpose(), a.getHostName(), a.getStartTime());
                userNotificationService.createNotification(
                        a.getVisitorId(), "approved", "预约审批通过", content, id);
            } else if (AppointmentStatus.REJECTED.equals(status)) {
                String reason = remark != null && !remark.isBlank() ? remark : "未说明原因";
                String content = String.format(
                        "您预约的【%s】已被%s拒绝，原因：%s。",
                        a.getPurpose(), a.getHostName(), reason);
                userNotificationService.createNotification(
                        a.getVisitorId(), "rejected", "预约被拒绝", content, id);
            }
        }

        // 预约被拒绝时，将记录加入风险向量库（供后续新预约 RAG 检索）
        // 并清除风险缓存，确保后续评估基于最新知识库
        if (AppointmentStatus.REJECTED.equals(status)) {
            riskVectorStore.add(a);
            deepSeekClient.clearAllRiskCache();
            log.info("预约被拒，已加入向量库并清除风险缓存: appointmentId={}", id);
        }
    }

    /**
     * 校验时间冲突：被访人在同一时间段是否已有 approved/pending 预约
     */
    public void checkTimeConflict(Integer hostId, LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return;
        long count = lambdaQuery()
                .eq(Appointment::getHostId, hostId)
                .in(Appointment::getStatus, AppointmentStatus.PENDING, AppointmentStatus.APPROVED)
                .lt(Appointment::getStartTime, end)
                .and(qw -> qw.isNull(Appointment::getEndTime)
                        .or().gt(Appointment::getEndTime, start))
                .count();
        if (count > 0) {
            throw new BusinessException("该时间段被访人已有预约，请选择其他时间");
        }
    }

    /**
     * 查询即将开始的预约（用于定时提醒）
     */
    public List<Appointment> findUpcoming(LocalDateTime now, LocalDateTime within) {
        return lambdaQuery()
                .eq(Appointment::getStatus, AppointmentStatus.APPROVED)
                .ge(Appointment::getStartTime, now)
                .lt(Appointment::getStartTime, within)
                .list();
    }

    /**
     * 生成 AI 迎接话术（异步执行，失败不影响审批结果）。
     * 已存在话术则跳过，避免重复生成。
     */
    @org.springframework.scheduling.annotation.Async("aiExecutor")
    public void generateGreetingIfNeeded(Integer appointmentId) {
        try {
            Greeting exist = greetingService.lambdaQuery()
                    .eq(Greeting::getAppointmentId, appointmentId).one();
            if (exist != null && "completed".equals(exist.getStatus())) {
                return;
            }
            Appointment a = getById(appointmentId);
            if (a == null) return;
            Map<String, String> aiResult = deepSeekClient.generateGreeting(
                    a.getVisitorName(), a.getCompany(), a.getPurpose(), a.getHostName());
            Greeting g = exist != null ? exist : new Greeting();
            g.setAppointmentId(appointmentId);
            g.setGreetingText(aiResult.get("greeting"));
            g.setSeatSuggestion(aiResult.get("seatSuggestion"));
            g.setNotes(aiResult.get("notes"));
            g.setStatus("completed");
            greetingService.saveOrUpdate(g);
            log.info("AI话术已生成: appointmentId={}", appointmentId);
        } catch (Exception e) {
            log.error("生成AI话术失败: appointmentId={}", appointmentId, e);
        }
    }

    /**
     * AI 风险预警（异步执行，失败不影响预约提交）。
     *
     * 流程：RAG 检索相似被拒记录 → 大模型评估风险等级 → 存库 → 高风险通知管理员。
     * 已存在评估结果则跳过，避免重复评估。
     */
    @org.springframework.scheduling.annotation.Async("aiExecutor")
    public void assessRiskIfNeeded(Integer appointmentId) {
        try {
            // 已有评估结果则跳过
            RiskAssessment exist = riskAssessmentService.lambdaQuery()
                    .eq(RiskAssessment::getAppointmentId, appointmentId).one();
            if (exist != null) {
                return;
            }

            Appointment a = getById(appointmentId);
            if (a == null) return;

            // 1. RAG 检索：从向量库中检索 Top-5 相似被拒记录
            List<RiskVectorStore.SearchResult> results = riskVectorStore.search(a, 5);
            String similarCasesText = formatSimilarCases(results);
            double avgSim = results.isEmpty() ? 0 : results.stream().mapToDouble(RiskVectorStore.SearchResult::similarity).average().orElse(0);
            log.info("风险检索完成: appointmentId={}, 命中{}条相似记录, 平均相似度={}",
                    appointmentId, results.size(), String.format("%.3f", avgSim));

            // 2. 硬规则优先：在 RAG 检索结果中检查手机号/姓名/单位精确匹配
            //    仅当检索到的案例中存在强匹配才跳过 AI，避免全量索引误伤
            Map<String, Object> aiResult = checkHardRules(a, results);
            if (aiResult == null) {
                // 未命中硬规则 → 走 AI 评估
                aiResult = deepSeekClient.assessRisk(a, similarCasesText, avgSim);
            }
            String riskLevel = normalizeLevel(String.valueOf(aiResult.get("riskLevel")));
            int riskScore = parseScore(aiResult.get("riskScore"));
            String reason = String.valueOf(aiResult.getOrDefault("reason", ""));
            String source = String.valueOf(aiResult.getOrDefault("source", ""));

            // 安全锁：非硬规则源判 high 一律降为 medium。
            // 硬规则有明确证据（同记录内手机号+姓名/单位匹配），AI/缓存/兜底仅凭文本相似度不可信。
            if ("high".equals(riskLevel) && !"hard_rule".equals(source)) {
                log.warn("[风险] 非硬规则源({})判high，强制降为medium: apptId={}", source, appointmentId);
                riskLevel = "medium";
                riskScore = Math.min(riskScore, 65);
                reason = "[系统降级] " + reason;
            }

            // 3. 存库
            RiskAssessment ra = new RiskAssessment();
            ra.setAppointmentId(appointmentId);
            ra.setRiskLevel(riskLevel);
            ra.setRiskScore(riskScore);
            ra.setReason(reason);
            ra.setSimilarCases(buildSimilarCasesJson(results));
            riskAssessmentService.save(ra);
            log.info("AI风险评估已保存: appointmentId={}, level={}, score={}",
                    appointmentId, riskLevel, riskScore);

            // 4. 高风险 → 自动通知所有管理员审核
            if ("high".equals(riskLevel)) {
                notifyAdminsRiskWarning(a, riskScore, reason);
            }
        } catch (Exception e) {
            log.error("AI风险评估失败: appointmentId={}", appointmentId, e);
        }
    }

    /**
     * 硬规则检测：在 RAG 检索结果中检查手机号/姓名/单位精确匹配。
     * 必须来自同一条记录的双重匹配才算强证据，避免跨记录误判。
     */
    private Map<String, Object> checkHardRules(Appointment a, List<RiskVectorStore.SearchResult> results) {
        if (results == null || results.isEmpty()) return null;
        String phone = a.getVisitorPhone(), name = a.getVisitorName(), company = a.getCompany();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("riskLevel", "high");
        for (RiskVectorStore.SearchResult sr : results) {
            RiskVectorStore.RiskDoc doc = sr.doc();
            boolean samePhone = phone != null && !phone.isBlank() && phone.equals(doc.visitorPhone());
            boolean sameName = name != null && !name.isBlank() && name.equals(doc.visitorName());
            boolean sameCompany = company != null && !company.isBlank() && company.equals(doc.company());
            if (samePhone && sameName) {
                r.put("riskScore", 95); r.put("reason", "硬规则：同手机号+同姓名历史被拒，建议拒绝。"); r.put("source", "hard_rule"); return r;
            }
            if (samePhone && sameCompany) {
                r.put("riskScore", 85); r.put("reason", "硬规则：同手机号+同单位历史被拒，疑似重复预约。"); r.put("source", "hard_rule"); return r;
            }
        }
        return null;
    }

    /** 将检索结果格式化为大模型可读的上下文文本 */
    private String formatSimilarCases(List<RiskVectorStore.SearchResult> results) {
        if (results.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (RiskVectorStore.SearchResult r : results) {
            RiskVectorStore.RiskDoc doc = r.doc();
            sb.append(String.format(
                    "被拒预约#%d：访客[%s] 手机[%s] 单位[%s] 事由[%s] 相似度%.2f\n",
                    doc.appointmentId(), doc.visitorName(), maskPhone(doc.visitorPhone()),
                    doc.company(), doc.purpose(), r.similarity()));
        }
        return sb.toString();
    }

    /** 检索结果转 JSON 存库（脱敏手机号） */
    private String buildSimilarCasesJson(List<RiskVectorStore.SearchResult> results) {
        try {
            List<Map<String, Object>> list = new ArrayList<>();
            for (RiskVectorStore.SearchResult r : results) {
                RiskVectorStore.RiskDoc doc = r.doc();
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("appointmentId", doc.appointmentId());
                item.put("visitorName", doc.visitorName());
                item.put("company", doc.company());
                item.put("purpose", doc.purpose());
                item.put("similarity", Math.round(r.similarity() * 100) / 100.0);
                list.add(item);
            }
            return MAPPER.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    /** 手机号脱敏：138****0000 */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /** 风险等级规范化（容错） */
    private String normalizeLevel(String level) {
        if (level == null) return "low";
        String l = level.toLowerCase().trim();
        if (l.contains("high")) return "high";
        if (l.contains("medium") || l.contains("mid")) return "medium";
        return "low";
    }

    /** 风险分数解析（容错） */
    private int parseScore(Object score) {
        if (score == null) return 0;
        try {
            return Math.max(0, Math.min(100, Integer.parseInt(String.valueOf(score))));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 高风险预约通知所有管理员 */
    private void notifyAdminsRiskWarning(Appointment a, int score, String reason) {
        List<User> admins = userService.lambdaQuery()
                .eq(User::getRole, "admin")
                .eq(User::getStatus, 1)
                .list();
        String title = String.format("高风险预约预警：%s（%s）", a.getVisitorName(), a.getCompany());
        String content = String.format(
                "访客[%s]（单位：%s）的预约被AI评估为高风险（风险分：%d）。\n" +
                "预警理由：%s\n请及时审核该预约。",
                a.getVisitorName(), a.getCompany(), score, reason);
        for (User admin : admins) {
            userNotificationService.createNotification(
                    admin.getId(), "risk_warning", title, content, a.getId());
        }
        log.info("高风险预警已通知{}位管理员: appointmentId={}", admins.size(), a.getId());
    }

    public PageResult<Appointment> pageAll(int page, int size, String status) {
        LambdaQueryWrapper<Appointment> qw = new LambdaQueryWrapper<Appointment>()
                .eq(!"all".equals(status), Appointment::getStatus, status)
                .orderByDesc(Appointment::getId);
        IPage<Appointment> p = page(new Page<>(page, size), qw);
        return new PageResult<>(p.getRecords(), p.getTotal(), page, size);
    }

    public List<Appointment> allPendingList() {
        return lambdaQuery()
                .eq(Appointment::getStatus, AppointmentStatus.PENDING)
                .orderByDesc(Appointment::getId)
                .list();
    }

    /**
     * 撤销预约（校验归属 + 状态）
     */
    public void cancel(Integer id, Integer operatorId) {
        Appointment a = getById(id);
        if (a == null) throw new BusinessException("预约不存在");
        // 游客预约 visitorId 为 null，注册用户无权撤销（避免 NPE）
        if (a.getVisitorId() != null && !operatorId.equals(a.getVisitorId())) {
            throw new BusinessException("无权撤销他人预约");
        }
        if (!AppointmentStatus.PENDING.equals(a.getStatus())) {
            throw new BusinessException("只能撤销待审核预约");
        }
        a.setStatus(AppointmentStatus.CANCELLED);
        updateById(a);
    }

    // --- 统计（SQL 聚合优化）---

    public Map<String, Object> hostStats(int hostId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;

        long total = lambdaQuery()
                .eq(Appointment::getHostId, hostId)
                .ge(start != null, Appointment::getStartTime, start)
                .lt(end != null, Appointment::getStartTime, end)
                .count();

        List<Map<String, Object>> companyStats = baseMapper.companyStatsByHost(hostId, start, end);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalVisits", total);
        result.put("companyStats", companyStats);
        return result;
    }

    public Map<String, Object> overview() {
        LocalDate today = LocalDate.now();
        long todayCount = lambdaQuery()
                .ge(Appointment::getCreateTime, today.atStartOfDay())
                .lt(Appointment::getCreateTime, today.plusDays(1).atStartOfDay())
                .count();
        long pendingCount = lambdaQuery()
                .eq(Appointment::getStatus, AppointmentStatus.PENDING).count();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("todayAppointments", todayCount);
        m.put("pendingApprovals", pendingCount);
        return m;
    }

    /**
     * 预约趋势统计（一条SQL按天聚合，Java侧补零）
     */
    public List<Map<String, Object>> trend(int days) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.minusDays(days - 1L).atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        List<Map<String, Object>> dbRows = baseMapper.trendByDate(start, end);

        Map<String, Integer> dateCountMap = new HashMap<>();
        for (Map<String, Object> row : dbRows) {
            Object dateObj = row.get("date");
            String dateStr = dateObj instanceof java.sql.Date
                    ? ((java.sql.Date) dateObj).toLocalDate().toString()
                    : String.valueOf(dateObj);
            dateCountMap.put(dateStr, ((Number) row.get("count")).intValue());
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", d.toString());
            m.put("count", dateCountMap.getOrDefault(d.toString(), 0));
            list.add(m);
        }
        return list;
    }

    public Map<String, Object> visitorRecordStats(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;

        List<Map<String, Object>> companyStats = baseMapper.companyStatsAll(start, end);
        List<Map<String, Object>> deptStats = baseMapper.deptStatsAll(start, end);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("companyStats", companyStats);
        result.put("deptStats", deptStats);
        return result;
    }
}
