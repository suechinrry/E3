package com.visitor.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.visitor.entity.UserNotification;
import com.visitor.mapper.UserNotificationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserNotificationService extends ServiceImpl<UserNotificationMapper, UserNotification> {

    /**
     * 创建用户通知（含防重复检查：userId+appointmentId+type相同则跳过）。
     * 兜底处理：userId 为 null 时静默跳过（游客预约无关联用户，无法发通知）。
     */
    public UserNotification createNotification(Integer userId, String type, String title, String content, Integer appointmentId) {
        // 游客预约 visitorId 为 null，无法关联用户，静默跳过
        if (userId == null) {
            log.debug("userId 为 null，跳过通知创建: type={}, appointmentId={}", type, appointmentId);
            return null;
        }
        // 防重复：同一用户、同一预约、同一类型的通知已存在时跳过
        long existing = lambdaQuery()
                .eq(UserNotification::getUserId, userId)
                .eq(UserNotification::getAppointmentId, appointmentId)
                .eq(UserNotification::getType, type)
                .count();
        if (existing > 0) return null;

        UserNotification notification = new UserNotification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setAppointmentId(appointmentId);
        notification.setIsRead(0);
        save(notification);
        return notification;
    }
}
