package com.visitor.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.visitor.common.Result;
import com.visitor.entity.Notification;
import com.visitor.entity.User;
import com.visitor.entity.UserNotification;
import com.visitor.service.NotificationService;
import com.visitor.service.UserNotificationService;
import com.visitor.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "通知公告")
@RestController
@RequestMapping
public class NotificationController {

    private final NotificationService notificationService;
    private final UserNotificationService userNotificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService,
                                  UserNotificationService userNotificationService,
                                  UserService userService) {
        this.notificationService = notificationService;
        this.userNotificationService = userNotificationService;
        this.userService = userService;
    }

    // --- 访客查看 ---
    @Operation(summary = "访客查看通知列表")
    @GetMapping("/notification")
    public Result<?> publicList(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "10") int size) {
        log.info("publicList 被调用: page={}, size={}", page, size);
        IPage<Notification> p = notificationService.lambdaQuery()
                .eq(Notification::getStatus, 1)
                .orderByDesc(Notification::getId)
                .page(new Page<>(page, size));
        log.info("publicList 返回记录数={}", p.getRecords().size());
        return Result.success(p);
    }

    // --- 管理员管理 ---
    @Operation(summary = "通知列表（管理端）")
    @GetMapping("/admin/notification")
    public Result<?> adminList(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int size) {
        IPage<Notification> p = notificationService.page(new Page<>(page, size));
        return Result.success(p);
    }

    @Operation(summary = "发布通知")
    @PostMapping("/admin/notification")
    public Result<Void> add(@RequestBody Notification n, HttpServletRequest request) {
        // 前端未传 status 时默认发布状态
        if (n.getStatus() == null) {
            n.setStatus(1);
        }
        notificationService.save(n);

        // 发布公告时，同步为目标角色用户创建通知记录
        if (n.getStatus() == 1) {
            Integer currentUserId = (Integer) request.getAttribute("userId");
            createUserNotificationsForAnnouncement(n, currentUserId);
        }

        return Result.success("发布成功", null);
    }

    /**
     * 根据公告 target_role 为目标用户创建弹窗通知
     * @param currentUserId 当前发布通知的管理员ID（用于给管理员自己也发一份）
     */
    private void createUserNotificationsForAnnouncement(Notification n, Integer currentUserId) {
        List<User> targetUsers;
        String role = n.getTargetRole();

        if ("all".equals(role)) {
            targetUsers = userService.lambdaQuery()
                    .eq(User::getStatus, 1)
                    .list();
        } else {
            targetUsers = userService.lambdaQuery()
                    .eq(User::getStatus, 1)
                    .eq(User::getRole, role)
                    .list();
        }

        log.info("发布公告 [{}] targetRole={}, 目标用户数={}", n.getTitle(), role, targetUsers.size());

        int successCount = 0;
        for (User user : targetUsers) {
            // 跳过当前管理员（后面统一处理，避免重复）
            if (user.getId().equals(currentUserId) && "admin".equals(user.getRole())) {
                continue;
            }
            UserNotification un = new UserNotification();
            un.setUserId(user.getId());
            un.setType("announcement");
            un.setTitle(n.getTitle());
            un.setContent(n.getContent());
            un.setAppointmentId(null);
            un.setIsRead(0);
            boolean saved = userNotificationService.save(un);
            if (saved) successCount++;
            else log.warn("为用户[{}]创建通知失败", user.getId());
        }

        // 给当前管理员也创建一份通知，方便在通知中心查看
        if (currentUserId != null) {
            UserNotification adminUn = new UserNotification();
            adminUn.setUserId(currentUserId);
            adminUn.setType("announcement");
            adminUn.setTitle(n.getTitle());
            adminUn.setContent(n.getContent());
            adminUn.setAppointmentId(null);
            adminUn.setIsRead(0);
            boolean saved = userNotificationService.save(adminUn);
            if (saved) successCount++;
            else log.warn("为管理员[{}]创建通知失败", currentUserId);
        }

        log.info("公告通知分发完成：成功={}, 总目标={}", successCount, targetUsers.size());
    }

    /**
     * 补分发：将已有的 not_notification 公告数据补写入 sys_user_notification
     * 用于修复历史数据（ALTER 执行后调用一次即可）
     */
    @Operation(summary = "补分发历史公告到用户通知表")
    @PostMapping("/admin/notification/redistribute")
    public Result<?> redistributeAll() {
        List<Notification> announcements = notificationService.lambdaQuery()
                .eq(Notification::getStatus, 1)
                .list();

        int totalCreated = 0;
        for (Notification n : announcements) {
            // 检查该公告是否已有分发记录
            long existing = userNotificationService.lambdaQuery()
                    .eq(UserNotification::getType, "announcement")
                    .eq(UserNotification::getTitle, n.getTitle())
                    .count();
            if (existing > 0) {
                log.info("公告[{}]已有{}条分发记录，跳过", n.getTitle(), existing);
                continue;
            }

            List<User> targetUsers;
            String role = n.getTargetRole();
            if ("all".equals(role)) {
                targetUsers = userService.lambdaQuery().eq(User::getStatus, 1).list();
            } else {
                targetUsers = userService.lambdaQuery()
                        .eq(User::getStatus, 1)
                        .eq(User::getRole, role).list();
            }

            for (User user : targetUsers) {
                UserNotification un = new UserNotification();
                un.setUserId(user.getId());
                un.setType("announcement");
                un.setTitle(n.getTitle());
                un.setContent(n.getContent());
                un.setAppointmentId(null);
                un.setIsRead(0);
                userNotificationService.save(un);
                totalCreated++;
            }
        }

        log.info("补分发完成：共创建 {} 条用户通知", totalCreated);
        return Result.success("补分发完成，共创建" + totalCreated + "条通知记录");
    }

    @Operation(summary = "修改通知")
    @PutMapping("/admin/notification/{id}")
    public Result<Void> update(@PathVariable Integer id, @RequestBody Notification n) {
        n.setId(id);
        notificationService.updateById(n);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "删除通知")
    @DeleteMapping("/admin/notification/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        notificationService.removeById(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "查看通知分发详情（谁收到了该通知）")
    @GetMapping("/admin/notification/{id}/recipients")
    public Result<?> getRecipients(@PathVariable Integer id) {
        Notification notification = notificationService.getById(id);
        if (notification == null) {
            return Result.error(404, "通知不存在");
        }

        List<UserNotification> userNotifications = userNotificationService.lambdaQuery()
                .eq(UserNotification::getAppointmentId, null)
                .eq(UserNotification::getTitle, notification.getTitle())
                .orderByDesc(UserNotification::getCreateTime)
                .list();

        return Result.success(userNotifications);
    }
}
