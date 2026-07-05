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

import java.util.LinkedHashMap;
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
        Greeting exist = greetingService.lambdaQuery()
                .eq(Greeting::getAppointmentId, appointmentId)
                .one();
        if (exist != null && "completed".equals(exist.getStatus())) {
            return Result.success(buildResponse(exist, "cached"));
        }
        return doGenerate(appointmentId);
    }

    @Operation(summary = "重新生成话术（清除缓存后强制调用AI）")
    @PostMapping("/greeting/{appointmentId}/regenerate")
    public Result<Map<String, String>> regenerate(@PathVariable Integer appointmentId) {
        Appointment a = appointmentService.getById(appointmentId);
        if (a == null) return Result.error("预约不存在");
        // 清除 Redis/内存缓存 → 直调 AI（不走 generateGreeting 的缓存路径）
        Map<String, String> aiResult = deepSeekClient.regenerateGreeting(
                a.getVisitorName(), a.getCompany(), a.getPurpose(), a.getHostName());
        return saveAndRespond(appointmentId, aiResult);
    }

    private Result<Map<String, String>> doGenerate(Integer appointmentId) {
        Appointment a = appointmentService.getById(appointmentId);
        if (a == null) return Result.error("预约不存在");

        Map<String, String> aiResult = deepSeekClient.generateGreeting(
                a.getVisitorName(), a.getCompany(), a.getPurpose(), a.getHostName());
        return saveAndRespond(appointmentId, aiResult);
    }

    private Result<Map<String, String>> saveAndRespond(Integer appointmentId, Map<String, String> aiResult) {
        Greeting g = greetingService.lambdaQuery()
                .eq(Greeting::getAppointmentId, appointmentId).one();
        if (g == null) g = new Greeting();
        g.setAppointmentId(appointmentId);
        g.setGreetingText(aiResult.get("greeting"));
        g.setSeatSuggestion(aiResult.get("seatSuggestion"));
        g.setNotes(aiResult.get("notes"));
        g.setStatus("completed");
        greetingService.saveOrUpdate(g);

        return Result.success(buildResponse(g, aiResult.getOrDefault("source", "unknown")));
    }

    @Operation(summary = "查看话术")
    @GetMapping("/appointment/{id}/greeting")
    public Result<Map<String, String>> view(@PathVariable Integer id) {
        Greeting g = greetingService.lambdaQuery()
                .eq(Greeting::getAppointmentId, id)
                .one();
        if (g == null) return Result.error("话术尚未生成");
        return Result.success(buildResponse(g, "cached"));
    }

    private Map<String, String> buildResponse(Greeting g, String source) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("greeting", g.getGreetingText() != null ? g.getGreetingText() : "");
        m.put("greetingText", g.getGreetingText() != null ? g.getGreetingText() : "");
        m.put("seatSuggestion", g.getSeatSuggestion() != null ? g.getSeatSuggestion() : "");
        m.put("notes", g.getNotes() != null ? g.getNotes() : "");
        m.put("source", source);
        return m;
    }
}
