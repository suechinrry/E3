package com.visitor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.Result;
import com.visitor.entity.Notification;
import com.visitor.entity.UserNotification;
import com.visitor.service.NotificationService;
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
@Tag(name = "用户通知（弹窗提醒）")
@RestController
@RequestMapping("/user-notification")
public class UserNotificationController {

    private final UserNotificationService userNotificationService;
    private final NotificationService notificationService;

    public UserNotificationController(UserNotificationService userNotificationService,
                                      NotificationService notificationService) {
        this.userNotificationService = userNotificationService;
        this.notificationService = notificationService;
    }

    @Operation(summary = "获取当前用户未读弹窗通知")
    @GetMapping("/popup")
    public Result<?> getPopupNotifications(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        log.info("getPopupNotifications userId={}, role={}", userId, role);

        List<UserNotification> personalList = userNotificationService.lambdaQuery()
                .eq(UserNotification::getUserId, userId)
                .eq(UserNotification::getIsRead, 0)
                .orderByAsc(UserNotification::getCreateTime)
                .list();

        // 同时查出与当前用户角色匹配的未读公告（not_notification 中 status=1 且 target_role 匹配）
        List<Notification> publicAnnouncements = getMatchingPublicAnnouncements(role, userId);

        // 合并：将公告转换为 UserNotification 格式（用于弹窗显示）
        List<Map<String, Object>> mergedList = new ArrayList<>();
        for (UserNotification un : personalList) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", un.getId());
            item.put("userId", un.getUserId());
            item.put("type", un.getType());
            item.put("title", un.getTitle());
            item.put("content", un.getContent());
            item.put("appointmentId", un.getAppointmentId());
            item.put("isRead", un.getIsRead());
            item.put("createTime", un.getCreateTime());
            item.put("_source", "personal");
            mergedList.add(item);
        }
        for (Notification n : publicAnnouncements) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", 10000 + n.getId()); // 偏移避免ID冲突
            item.put("userId", null);
            item.put("type", "announcement");
            item.put("title", n.getTitle());
            item.put("content", n.getContent());
            item.put("appointmentId", null);
            item.put("isRead", 0); // 公告默认未读
            item.put("createTime", n.getCreateTime());
            item.put("_source", "public");
            mergedList.add(item);
        }

        log.info("popup 返回：个人={}, 公告={}, 合计={}", personalList.size(), publicAnnouncements.size(), mergedList.size());
        return Result.success(mergedList);
    }

    @Operation(summary = "获取当前用户全部通知（通知中心）")
    @GetMapping("/list")
    public Result<?> listAll(HttpServletRequest request,
                             @RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "20") int size) {
        Integer userId = (Integer) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        log.info("listAll userId={}, role={}, page={}, size={}", userId, role, page, size);

        // 1. 查个人通知
        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotification::getUserId, userId)
               .orderByDesc(UserNotification::getCreateTime);
        List<UserNotification> personalList = userNotificationService.list(wrapper);

        // 2. 查与当前用户角色匹配的公告
        List<Notification> publicAnnouncements = getMatchingPublicAnnouncements(role, userId);

        // 3. 合并并去重（按唯一key：type+title）
        Map<String, Map<String, Object>> mergedMap = new HashMap<>();
        for (UserNotification un : personalList) {
            String key = "personal_" + un.getId();
            Map<String, Object> item = new HashMap<>();
            item.put("id", un.getId());
            item.put("userId", un.getUserId());
            item.put("type", un.getType());
            item.put("title", un.getTitle());
            item.put("content", un.getContent());
            item.put("appointmentId", un.getAppointmentId());
            item.put("isRead", un.getIsRead());
            item.put("createTime", un.getCreateTime());
            item.put("_source", "personal");
            mergedMap.put(key, item);
        }
        for (Notification n : publicAnnouncements) {
            String key = "public_" + n.getId();
            // 只在 sys_user_notification 中没有同标题同类型的记录时才加入
            boolean alreadyInPersonal = personalList.stream()
                    .anyMatch(un -> "announcement".equals(un.getType()) && un.getTitle().equals(n.getTitle()));
            if (!alreadyInPersonal) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", 10000 + n.getId());
                item.put("userId", null);
                item.put("type", "announcement");
                item.put("title", n.getTitle());
                item.put("content", n.getContent());
                item.put("appointmentId", null);
                item.put("isRead", 1); // 公告在列表中显示为已读
                item.put("createTime", n.getCreateTime());
                item.put("_source", "public");
                mergedMap.put(key, item);
            }
        }

        List<Map<String, Object>> allList = new ArrayList<>(mergedMap.values());
        // 按id倒序排列
        allList.sort((a, b) -> {
            Object idA = a.get("id");
            Object idB = b.get("id");
            return ((Number) idB).intValue() - ((Number) idA).intValue();
        });

        log.info("listAll 返回：个人={}, 公告={}, 合计={}", personalList.size(), publicAnnouncements.size(), allList.size());
        return Result.success(allList);
    }

    /**
     * 获取与当前用户角色匹配的公告列表
     */
    private List<Notification> getMatchingPublicAnnouncements(String role, Integer userId) {
        List<Notification> allPublic = notificationService.lambdaQuery()
                .eq(Notification::getStatus, 1)
                .orderByDesc(Notification::getId)
                .list();

        List<Notification> matched = new ArrayList<>();
        for (Notification n : allPublic) {
            String targetRole = n.getTargetRole();
            if ("all".equals(targetRole) || targetRole.equals(role)) {
                matched.add(n);
            }
        }
        return matched;
    }

    @Operation(summary = "标记通知已读")
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Integer id, HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        // 公共公告的id以10000+偏移，在sys_user_notification中不存在，直接返回成功即可
        if (id >= 10000) {
            log.info("公共公告id={}，无需标记已读", id);
            return Result.success("已标记为已读", null);
        }
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
