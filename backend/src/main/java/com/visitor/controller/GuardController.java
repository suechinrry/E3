package com.visitor.controller;

import com.visitor.common.Result;
import com.visitor.entity.VisitRecord;
import com.visitor.entity.Appointment;
import com.visitor.service.AppointmentService;
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

    public GuardController(AppointmentService appointmentService,
                           VisitRecordService visitRecordService) {
        this.appointmentService = appointmentService;
        this.visitRecordService = visitRecordService;
    }

    @Operation(summary = "扫码核验")
    @PostMapping("/verify")
    public Result<Map<String, Object>> verify(@RequestBody Map<String, String> body) {
        String qrCode = body.get("qrCode");
        // qrCode 格式: "appointment_1001"
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
        // 查找该访客所有已通过的预约（按时间倒序取最新）
        List<Appointment> list = appointmentService.lambdaQuery()
                .eq(Appointment::getVisitorName, name.trim())
                .eq(Appointment::getVisitorPhone, phone.trim())
                .in(Appointment::getStatus, List.of("approved", "confirmed"))
                .orderByDesc(Appointment::getStartTime)
                .list();
        if (list.isEmpty()) {
            return Result.error("未找到该访客的有效预约，请核实姓名和手机号");
        }
        Appointment a = list.get(0);  // 最新一条
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
        return Result.success(m);
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
