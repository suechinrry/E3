package com.visitor.controller;

import com.visitor.common.Result;
import com.visitor.entity.Greeting;
import com.visitor.entity.VisitRecord;
import com.visitor.entity.Appointment;
import com.visitor.service.AppointmentService;
import com.visitor.service.GreetingService;
import com.visitor.service.UserNotificationService;
import com.visitor.service.VisitRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "门岗核验")
@RestController
@RequestMapping("/guard")
public class GuardController {

    private final AppointmentService appointmentService;
    private final VisitRecordService visitRecordService;
    private final GreetingService greetingService;
    private final UserNotificationService userNotificationService;

    public GuardController(AppointmentService appointmentService,
                           VisitRecordService visitRecordService,
                           GreetingService greetingService,
                           UserNotificationService userNotificationService) {
        this.appointmentService = appointmentService;
        this.visitRecordService = visitRecordService;
        this.greetingService = greetingService;
        this.userNotificationService = userNotificationService;
    }

    @Operation(summary = "扫码核验")
    @PostMapping("/verify")
    public Result<Map<String, Object>> verify(@RequestBody Map<String, String> body) {
        String qrCode = body.get("qrCode");
        Integer id;
        try {
            id = Integer.parseInt(qrCode.replace("appointment_", ""));
        } catch (Exception e) {
            return Result.error("二维码格式无效");
        }
        Appointment a = appointmentService.getById(id);
        if (a == null) return Result.error("预约不存在");
        boolean valid = "approved".equals(a.getStatus()) || "confirmed".equals(a.getStatus());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("valid", valid);
        m.put("message", valid ? "核验通过，欢迎来访" : "预约未通过审核");
        m.put("appointmentId", a.getId());
        m.put("visitorName", a.getVisitorName());
        m.put("company", a.getCompany());
        m.put("hostName", a.getHostName());
        m.put("time", a.getStartTime() + " - " + (a.getEndTime() != null ? a.getEndTime() : ""));
        m.put("status", a.getStatus());

        // 核验成功时，给访客发送接待建议通知
        if (valid && a.getVisitorId() != null) {
            sendGreetingNotification(a);
        }
        return Result.success(m);
    }

    @Operation(summary = "手动核验（按姓名+手机号查找预约）")
    @PostMapping("/verify-by-name")
    public Result<Map<String, Object>> verifyByName(@RequestBody Map<String, String> body) {
        String name = body.get("visitorName");
        String phone = body.get("visitorPhone");
        if (name == null || name.isBlank() || phone == null || phone.isBlank()) {
            return Result.error("请输入访客姓名和手机号");
        }
        List<Appointment> list = appointmentService.lambdaQuery()
                .eq(Appointment::getVisitorName, name.trim())
                .eq(Appointment::getVisitorPhone, phone.trim())
                .in(Appointment::getStatus, List.of("approved", "confirmed"))
                .orderByDesc(Appointment::getStartTime)
                .list();
        if (list.isEmpty()) {
            return Result.error("未找到该访客的有效预约，请核实姓名和手机号");
        }
        Appointment a = list.get(0);
        boolean valid = true;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("valid", valid);
        m.put("message", "核验通过，欢迎来访");
        m.put("appointmentId", a.getId());
        m.put("visitorName", a.getVisitorName());
        m.put("company", a.getCompany());
        m.put("hostName", a.getHostName());
        m.put("time", a.getStartTime() + " - " + (a.getEndTime() != null ? a.getEndTime() : ""));
        m.put("status", a.getStatus());

        // 核验成功时，给访客发送接待建议通知
        if (a.getVisitorId() != null) {
            sendGreetingNotification(a);
        }
        return Result.success(m);
    }

    /**
     * 核验成功时发送接待建议通知给访客（防重复由service层统一处理）
     */
    private void sendGreetingNotification(Appointment a) {
        // 查询该预约的接待建议
        Greeting greeting = greetingService.lambdaQuery()
                .eq(Greeting::getAppointmentId, a.getId())
                .one();

        String content;
        if (greeting != null && "completed".equals(greeting.getStatus())) {
            content = String.format(
                "{\"greetingText\":\"%s\",\"notes\":\"%s\"}",
                greeting.getGreetingText() != null ? greeting.getGreetingText() : "",
                greeting.getNotes() != null ? greeting.getNotes() : ""
            );
        } else {
            content = String.format("{\"greetingText\":\"欢迎%s莅临我司，%s将接待您。\"}",
                a.getVisitorName(), a.getHostName());
        }

        userNotificationService.createNotification(
            a.getVisitorId(), "greeting", "欢迎莅临", content, a.getId());
    }

    @Operation(summary = "确认放行")
    @PutMapping("/confirm/{appointmentId}")
    public Result<Void> confirm(@PathVariable Integer appointmentId, HttpServletRequest req) {
        Appointment a = appointmentService.getById(appointmentId);
        if (a == null) return Result.error("预约不存在");
        a.setStatus("confirmed");
        appointmentService.updateById(a);

        VisitRecord r = new VisitRecord();
        r.setAppointmentId(appointmentId);
        r.setGuardId((Integer) req.getAttribute("userId"));
        r.setConfirmTime(LocalDateTime.now());
        visitRecordService.save(r);
        return Result.success("已确认放行", null);
    }
}
