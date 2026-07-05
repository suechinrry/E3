package com.visitor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.PageResult;
import com.visitor.common.Result;
import com.visitor.common.exception.BusinessException;
import com.visitor.entity.User;
import com.visitor.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "员工管理")
@RestController
@RequestMapping("/admin/employee")
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Operation(summary = "分页查询员工（被访人）")
    @GetMapping
    public Result<PageResult<User>> page(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Integer departmentId) {
        return Result.success(userService.pageEmployees(page, size, keyword, departmentId));
    }

    @Operation(summary = "新增员工")
    @PostMapping
    public Result<Void> add(@RequestBody User user) {
        // 前端可能不传 username/password，自动生成默认值
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            user.setUsername(user.getPhone() != null ? user.getPhone() : "user_" + System.currentTimeMillis());
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            user.setPassword("123456");
        }
        // 密码 BCrypt 加密存储
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("host");
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        userService.save(user);
        return Result.success("新增成功", null);
    }

    @Operation(summary = "修改员工")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Integer id, @RequestBody User user) {
        user.setId(id);
        // 角色单一限制：员工必须是host，不允许改角色
        user.setRole(null);
        // 修改密码时才加密，null 表示不修改密码
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null);
        }
        userService.updateById(user);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "删除员工")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        userService.removeById(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "批量操作")
    @PostMapping("/batch")
    public Result<Void> batch(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> ids = (List<Integer>) body.get("ids");
        String action = (String) body.get("action");
        if ("delete".equals(action)) {
            userService.removeByIds(ids);
        }
        return Result.success("操作成功", null);
    }
}
