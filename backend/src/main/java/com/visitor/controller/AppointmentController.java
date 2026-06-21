package com.visitor.controller;

import com.visitor.ai.DeepSeekClient;
import com.visitor.common.Result;
import com.visitor.entity.Appointment;
import com.visitor.entity.Greeting;
import com.visitor.entity.User;
import com.visitor.service.AppointmentService;
import com.visitor.service.GreetingService;
import com.visitor.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@Slf4j
@Tag(name = "预约管理")
@RestController
@RequestMapping
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final UserService userService;
    private final GreetingService greetingService;
    private final DeepSeekClient deepSeekClient;

    public AppointmentController(AppointmentService appointmentService, UserService userService,
                                  GreetingService greetingService, DeepSeekClient deepSeekClient) {
        this.appointmentService = appointmentService;
        this.userService = userService;
        this.greetingService = greetingService;
        this.deepSeekClient = deepSeekClient;
    }

    // --- 访客端 ---

    @Operation(summary = "根据姓名+手机号查找被访人")
    @PostMapping("/appointment/host/lookup")
    public Result<?> lookupHost(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String phone = body.get("phone");
        if (name == null || name.isBlank() || phone == null || phone.isBlank()) {
            return Result.error("请输入被访人姓名和手机号");
        }
        User user = userService.lambdaQuery()
                .eq(User::getRole, "host")
                .eq(User::getName, name.trim())
                .eq(User::getPhone, phone.trim())
                .one();
        if (user == null) {
            return Result.error("未找到该被访人，请核实姓名和手机号");
        }
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("name", user.getName());
        result.put("departmentId", user.getDepartmentId() != null ? user.getDepartmentId() : 0);
        return Result.success(result);
    }

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

    @Operation(summary = "恢复已取消的预约")
    @PutMapping("/appointment/{id}/restore")
    public Result<Void> restore(@PathVariable Integer id) {
        Appointment a = appointmentService.getById(id);
        if (a == null) return Result.error("预约不存在");
        if (!"cancelled".equals(a.getStatus())) {
            return Result.error("只能恢复已取消的预约");
        }
        a.setStatus("pending");
        appointmentService.updateById(a);
        return Result.success("预约已恢复", null);
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
        String status = body.get("status");
        appointmentService.approve(id, status, body.get("remark"));

        // 审批通过时自动生成 AI 迎接话术
        if ("approved".equals(status)) {
            try {
                Appointment a = appointmentService.getById(id);
                if (a != null) {
                    // 检查是否已有话术
                    Greeting exist = greetingService.lambdaQuery()
                            .eq(Greeting::getAppointmentId, id).one();
                    if (exist == null) {
                        Map<String, String> aiResult = deepSeekClient.generateGreeting(
                                a.getVisitorName(), a.getCompany(), a.getPurpose(), a.getHostName());
                        Greeting g = new Greeting();
                        g.setAppointmentId(id);
                        g.setGreetingText(aiResult.get("greeting"));
                        g.setSeatSuggestion(aiResult.get("seatSuggestion"));
                        g.setNotes(aiResult.get("notes"));
                        g.setStatus("completed");
                        greetingService.save(g);
                        log.info("AI话术已生成: appointmentId={}", id);
                    }
                }
            } catch (Exception e) {
                log.error("生成AI话术失败: appointmentId={}", id, e);
            }
        }
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
        Integer hostId = (Integer) req.getAttribute("userId");
        a.setVisitorId(null);  // 访客非系统用户，可留空
        a.setHostId(hostId);
        // 自动补全被访人姓名
        if (a.getHostName() == null || a.getHostName().isBlank()) {
            User host = userService.getById(hostId);
            a.setHostName(host != null ? host.getName() : "未知");
        }
        a.setStatus("pending");
        appointmentService.save(a);
        return Result.success("预约成功", Map.of("appointmentId", a.getId()));
    }
}
