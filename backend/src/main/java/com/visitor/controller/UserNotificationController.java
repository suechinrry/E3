package com.visitor.controller;

import com.visitor.common.Result;
import com.visitor.entity.UserNotification;
import com.visitor.service.UserNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "用户通知")
@RestController
@RequestMapping("/user-notification")
public class UserNotificationController {

    private final UserNotificationService userNotificationService;

    public UserNotificationController(UserNotificationService userNotificationService) {
        this.userNotificationService = userNotificationService;
    }

    @Operation(summary = "获取当前用户未读通知（首页红点用，前端已不再调用）")
    @GetMapping("/popup")
    public Result<?> getPopupNotifications(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        log.info("getPopupNotifications userId={}", userId);

        List<UserNotification> list = userNotificationService.lambdaQuery()
                .eq(UserNotification::getUserId, userId)
                .eq(UserNotification::getIsRead, 0)
                .orderByAsc(UserNotification::getCreateTime)
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (UserNotification un : list) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", un.getId());
            item.put("type", un.getType());
            item.put("title", un.getTitle());
            item.put("content", un.getContent());
            item.put("appointmentId", un.getAppointmentId());
            item.put("isRead", un.getIsRead());
            item.put("createTime", un.getCreateTime());
            result.add(item);
        }
        return Result.success(result);
    }

    @Operation(summary = "获取当前用户全部通知（通知中心）")
    @GetMapping("/list")
    public Result<?> listAll(HttpServletRequest request,
                             @RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "20") int size) {
        Integer userId = (Integer) request.getAttribute("userId");
        log.info("listAll userId={}", userId);

        List<UserNotification> personalList = userNotificationService.lambdaQuery()
                .eq(UserNotification::getUserId, userId)
                .orderByDesc(UserNotification::getCreateTime)
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (UserNotification un : personalList) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", un.getId());
            item.put("type", un.getType());
            item.put("title", un.getTitle());
            item.put("content", un.getContent());
            item.put("appointmentId", un.getAppointmentId());
            item.put("isRead", un.getIsRead());
            item.put("createTime", un.getCreateTime());
            result.add(item);
        }

        return Result.success(result);
    }

    @Operation(summary = "标记通知已读")
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Integer id, HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        UserNotification notification = userNotificationService.getById(id);
        if (notification == null || !notification.getUserId().equals(userId)) {
            return Result.error(403, "无权操作此通知");
        }
        notification.setIsRead(1);
        userNotificationService.updateById(notification);
        return Result.success("已标记为已读", null);
    }

    @Operation(summary = "标记全部通知已读")
    @PutMapping("/read-all")
    public Result<Void> markAllRead(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        userNotificationService.lambdaUpdate()
                .eq(UserNotification::getUserId, userId)
                .eq(UserNotification::getIsRead, 0)
                .set(UserNotification::getIsRead, 1)
                .update();
        return Result.success("全部标记为已读", null);
    }
}
