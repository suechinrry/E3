package com.visitor.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.visitor.common.Result;
import com.visitor.entity.Notification;
import com.visitor.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "通知公告")
@RestController
@RequestMapping
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // --- 访客查看 ---
    @Operation(summary = "访客查看通知列表")
    @GetMapping("/notification")
    public Result<?> publicList(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "10") int size) {
        IPage<Notification> p = notificationService.lambdaQuery()
                .eq(Notification::getStatus, 1)
                .orderByDesc(Notification::getId)
                .page(new Page<>(page, size));
        return Result.success(p.getRecords());
    }

    // --- 管理员管理 ---
    @Operation(summary = "通知列表（管理端）")
    @GetMapping("/admin/notification")
    public Result<?> adminList(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int size) {
        IPage<Notification> p = notificationService.page(new Page<>(page, size));
        return Result.success(p.getRecords());
    }

    @Operation(summary = "发布通知")
    @PostMapping("/admin/notification")
    public Result<Void> add(@RequestBody Notification n) {
        notificationService.save(n);
        return Result.success("发布成功", null);
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
}
