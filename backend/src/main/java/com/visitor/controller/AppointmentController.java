package com.visitor.controller;

import com.visitor.common.Result;
import com.visitor.entity.Appointment;
import com.visitor.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@Tag(name = "预约管理")
@RestController
@RequestMapping
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    // --- 访客端 ---

    @Operation(summary = "提交预约申请")
    @PostMapping("/appointment")
    public Result<Map<String, Integer>> create(@RequestBody Appointment a, HttpServletRequest req) {
        Integer userId = (Integer) req.getAttribute("userId");
        a.setVisitorId(userId);
        a.setStatus("pending");
        appointmentService.save(a);
        return Result.success("预约提交成功", Map.of("appointmentId", a.getId()));
    }

    @Operation(summary = "我的预约记录")
    @GetMapping("/appointment/my")
    public Result<?> myList(HttpServletRequest req,
                            @RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "10") int size,
                            @RequestParam(defaultValue = "all") String status) {
        Integer userId = (Integer) req.getAttribute("userId");
        return Result.success(appointmentService.pageMy(userId, page, size, status));
    }

    @Operation(summary = "撤销预约")
    @PutMapping("/appointment/{id}/cancel")
    public Result<Void> cancel(@PathVariable Integer id) {
        appointmentService.cancel(id);
        return Result.success("预约已撤销", null);
    }

    @Operation(summary = "再次预约")
    @PostMapping("/appointment/{id}/rebook")
    public Result<Map<String, Integer>> rebook(@PathVariable Integer id, HttpServletRequest req) {
        Appointment old = appointmentService.getById(id);
        if (old == null) return Result.error("预约不存在");
        Appointment a = new Appointment();
        a.setVisitorName(old.getVisitorName());
        a.setVisitorPhone(old.getVisitorPhone());
        a.setCompany(old.getCompany());
        a.setPurpose(old.getPurpose());
        a.setHostId(old.getHostId());
        a.setHostName(old.getHostName());
        a.setVisitorId((Integer) req.getAttribute("userId"));
        a.setStatus("pending");
        appointmentService.save(a);
        return Result.success("预约提交成功", Map.of("appointmentId", a.getId()));
    }

    // --- 被访人端 ---

    @Operation(summary = "被访记录")
    @GetMapping("/appointment/host")
    public Result<?> hostList(HttpServletRequest req,
                              @RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "10") int size,
                              @RequestParam(defaultValue = "all") String status,
                              @RequestParam(required = false) LocalDate startDate,
                              @RequestParam(required = false) LocalDate endDate) {
        Integer hostId = (Integer) req.getAttribute("userId");
        return Result.success(appointmentService.pageHost(hostId, page, size, status, startDate, endDate));
    }

    @Operation(summary = "被访统计")
    @GetMapping("/appointment/host/stats")
    public Result<?> hostStats(HttpServletRequest req,
                               @RequestParam(required = false) LocalDate startDate,
                               @RequestParam(required = false) LocalDate endDate) {
        Integer hostId = (Integer) req.getAttribute("userId");
        return Result.success(appointmentService.hostStats(hostId, startDate, endDate));
    }

    @Operation(summary = "待审批列表")
    @GetMapping("/appointment/host/pending")
    public Result<?> pending(HttpServletRequest req) {
        Integer hostId = (Integer) req.getAttribute("userId");
        return Result.success(appointmentService.pendingList(hostId));
    }

    @Operation(summary = "审批预约")
    @PutMapping("/appointment/{id}/approve")
    public Result<Void> approve(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        appointmentService.approve(id, body.get("status"), body.get("remark"));
        return Result.success("审批完成", null);
    }

    // --- 管理员端 ---

    @Operation(summary = "全部预约（管理员）")
    @GetMapping("/admin/appointment")
    public Result<?> allList(@RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "10") int size,
                             @RequestParam(defaultValue = "all") String status) {
        return Result.success(appointmentService.pageAll(page, size, status));
    }

    @Operation(summary = "全部待审批（管理员）")
    @GetMapping("/admin/appointment/pending")
    public Result<?> allPending() {
        return Result.success(appointmentService.allPendingList());
    }

    @Operation(summary = "辅助预约（代填）")
    @PostMapping("/appointment/helper")
    public Result<Map<String, Integer>> helper(@RequestBody Appointment a, HttpServletRequest req) {
        a.setVisitorId(null);
        a.setHostId((Integer) req.getAttribute("userId"));
        a.setStatus("pending");
        appointmentService.save(a);
        return Result.success("预约成功", Map.of("appointmentId", a.getId()));
    }
}
