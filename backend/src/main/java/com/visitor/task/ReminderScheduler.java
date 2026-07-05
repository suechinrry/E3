package com.visitor.task;

import com.visitor.ai.DeepSeekClient;
import com.visitor.ai.RiskVectorStore;
import com.visitor.entity.Appointment;
import com.visitor.service.AppointmentService;
import com.visitor.service.RiskAssessmentService;
import com.visitor.service.UserNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@EnableScheduling
public class ReminderScheduler {

    private final AppointmentService appointmentService;
    private final UserNotificationService userNotificationService;
    private final RiskAssessmentService riskAssessmentService;
    private final RiskVectorStore riskVectorStore;
    private final DeepSeekClient deepSeekClient;
    private static final int RISK_RETENTION_DAYS = 90;

    public ReminderScheduler(AppointmentService appointmentService, UserNotificationService userNotificationService,
                             RiskAssessmentService riskAssessmentService, RiskVectorStore riskVectorStore,
                             DeepSeekClient deepSeekClient) {
        this.appointmentService = appointmentService; this.userNotificationService = userNotificationService;
        this.riskAssessmentService = riskAssessmentService; this.riskVectorStore = riskVectorStore;
        this.deepSeekClient = deepSeekClient;
    }

    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void sendReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<Appointment> upcoming = appointmentService.findUpcoming(now, now.plusMinutes(30));
        if (upcoming.isEmpty()) return;
        log.info("发现{}个即将开始的预约", upcoming.size());
        for (Appointment a : upcoming) {
            if (a.getVisitorId() == null) continue;
            userNotificationService.createNotification(a.getVisitorId(), "announcement", "预约即将开始",
                    String.format("您预约的【%s】将于%s开始。被访人：%s。", a.getPurpose(), a.getStartTime().toLocalTime(), a.getHostName()), a.getId());
        }
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanRiskData() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RISK_RETENTION_DAYS);
        long before = riskAssessmentService.count();
        riskAssessmentService.lambdaUpdate().lt(com.visitor.entity.RiskAssessment::getCreateTime, cutoff).remove();
        long after = riskAssessmentService.count();
        log.info("风险数据清理：删除{}条过期记录，剩余{}条", before - after, after);
        riskVectorStore.reload();
        deepSeekClient.clearAllRiskCache();
        log.info("风险向量库定时重建：{}条，已清除风险缓存", riskVectorStore.size());
    }
}
