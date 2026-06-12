package com.visitor.controller;

import com.visitor.common.Result;
import com.visitor.entity.Appointment;
import com.visitor.entity.Greeting;
import com.visitor.service.AppointmentService;
import com.visitor.service.GreetingService;
import com.visitor.ai.DeepSeekClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "AI话术")
@RestController
@RequestMapping("/ai")
public class GreetingController {

    private final AppointmentService appointmentService;
    private final GreetingService greetingService;
    private final DeepSeekClient deepSeekClient;

    public GreetingController(AppointmentService appointmentService,
                              GreetingService greetingService,
                              DeepSeekClient deepSeekClient) {
        this.appointmentService = appointmentService;
        this.greetingService = greetingService;
        this.deepSeekClient = deepSeekClient;
    }

    @Operation(summary = "生成/获取迎接话术")
    @PostMapping("/greeting/{appointmentId}")
    public Result<Map<String, String>> generate(@PathVariable Integer appointmentId) {
        // 查已有话术，有就直接返回（除非强制重新生成）
        Greeting exist = greetingService.lambdaQuery()
                .eq(Greeting::getAppointmentId, appointmentId)
                .one();
        if (exist != null && "completed".equals(exist.getStatus())) {
            return Result.success(Map.of(
                    "greeting", exist.getGreetingText(),
                    "seatSuggestion", exist.getSeatSuggestion(),
                    "notes", exist.getNotes(),
                    "source", "cached"
            ));
        }
        return doGenerate(appointmentId);
    }

    @Operation(summary = "重新生成话术（强制调用AI）")
    @PostMapping("/greeting/{appointmentId}/regenerate")
    public Result<Map<String, String>> regenerate(@PathVariable Integer appointmentId) {
        return doGenerate(appointmentId);
    }

    private Result<Map<String, String>> doGenerate(Integer appointmentId) {
        Appointment a = appointmentService.getById(appointmentId);
        if (a == null) return Result.error("预约不存在");

        Map<String, String> result = deepSeekClient.generateGreeting(
                a.getVisitorName(), a.getCompany(), a.getPurpose(), a.getHostName());

        // 更新或新增话术记录
        Greeting g = greetingService.lambdaQuery()
                .eq(Greeting::getAppointmentId, appointmentId).one();
        if (g == null) g = new Greeting();
        g.setAppointmentId(appointmentId);
        g.setGreetingText(result.get("greeting"));
        g.setSeatSuggestion(result.get("seatSuggestion"));
        g.setNotes(result.get("notes"));
        g.setStatus("completed");
        greetingService.saveOrUpdate(g);

        return Result.success(result);
    }

    @Operation(summary = "查看话术")
    @GetMapping("/appointment/{id}/greeting")
    public Result<Map<String, String>> view(@PathVariable Integer id) {
        Greeting g = greetingService.lambdaQuery()
                .eq(Greeting::getAppointmentId, id)
                .one();
        if (g == null) return Result.error("话术尚未生成");
        return Result.success(Map.of(
                "greeting", g.getGreetingText(),
                "seatSuggestion", g.getSeatSuggestion(),
                "notes", g.getNotes(),
                "status", g.getStatus()
        ));
    }
}
