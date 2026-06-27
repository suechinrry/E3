package com.visitor.controller;

import com.visitor.common.Result;
import com.visitor.service.SystemSettingService;
import com.visitor.service.UserService;
import com.visitor.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "系统设置 & 数据统计")
@RestController
@RequestMapping("/admin")
public class SystemController {

    private final SystemSettingService settingService;
    private final UserService userService;
    private final AppointmentService appointmentService;

    public SystemController(SystemSettingService settingService,
                            UserService userService,
                            AppointmentService appointmentService) {
        this.settingService = settingService;
        this.userService = userService;
        this.appointmentService = appointmentService;
    }

    // --- 系统设置 ---
    @Operation(summary = "获取系统配置")
    @GetMapping("/settings")
    public Result<Map<String, String>> getSettings() {
        return Result.success(settingService.getMap());
    }

    @Operation(summary = "更新系统配置")
    @PutMapping("/settings")
    public Result<Void> updateSettings(@RequestBody Map<String, String> body) {
        body.forEach((key, value) -> {
            var s = settingService.lambdaQuery()
                    .eq(com.visitor.entity.SystemSetting::getConfigKey, key).one();
            if (s != null) {
                s.setConfigValue(value);
                settingService.updateById(s);
            }
        });
        return Result.success("保存成功", null);
    }

    // --- 管理员管理 ---
    @Operation(summary = "管理员列表")
    @GetMapping("/admin")
    public Result<?> adminList(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int size) {
        return Result.success(userService.pageAdmins(page, size));
    }

    @Operation(summary = "新增管理员")
    @PostMapping("/admin")
    public Result<Void> addAdmin(@RequestBody com.visitor.entity.User user) {
        user.setRole("admin");
        userService.save(user);
        return Result.success("新增成功", null);
    }

    @Operation(summary = "修改管理员")
    @PutMapping("/admin/{id}")
    public Result<Void> updateAdmin(@PathVariable Integer id, @RequestBody com.visitor.entity.User user) {
        user.setId(id);
        // 角色单一限制：管理员必须是admin，不允许改角色
        user.setRole(null);
        userService.updateById(user);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "删除管理员")
    @DeleteMapping("/admin/{id}")
    public Result<Void> deleteAdmin(@PathVariable Integer id) {
        userService.removeById(id);
        return Result.success("删除成功", null);
    }

    // --- 数据统计 ---
    @Operation(summary = "总览统计")
    @GetMapping("/stats/overview")
    public Result<Map<String, Object>> overview() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.putAll(appointmentService.overview());
        m.put("totalEmployees", userService.lambdaQuery()
                .eq(com.visitor.entity.User::getRole, "host").count());
        m.put("totalDepartments", 5L);
        return Result.success(m);
    }

    @Operation(summary = "访客记录统计")
    @GetMapping("/stats/visitor-record")
    public Result<?> visitorRecord(@RequestParam(required = false) LocalDate startDate,
                                   @RequestParam(required = false) LocalDate endDate) {
        return Result.success(appointmentService.visitorRecordStats(startDate, endDate));
    }

    @Operation(summary = "预约趋势")
    @GetMapping("/stats/trend")
    public Result<?> trend(@RequestParam(defaultValue = "7") int days) {
        return Result.success(appointmentService.trend(days));
    }
}
