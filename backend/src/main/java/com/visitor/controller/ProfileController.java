package com.visitor.controller;

import com.visitor.common.Result;
import com.visitor.entity.User;
import com.visitor.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "个人资料")
@RestController
@RequestMapping("/user")
public class ProfileController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public ProfileController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Operation(summary = "获取个人资料")
    @GetMapping("/profile")
    public Result<User> getProfile(HttpServletRequest req) {
        Integer userId = (Integer) req.getAttribute("userId");
        return Result.success(userService.getById(userId));
    }

    @Operation(summary = "修改个人资料")
    @PutMapping("/profile")
    public Result<Void> updateProfile(HttpServletRequest req, @RequestBody User user) {
        Integer userId = (Integer) req.getAttribute("userId");
        user.setId(userId);
        // 修改密码时才加密，null 表示不修改密码
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null);
        }
        userService.updateById(user);
        return Result.success("保存成功", null);
    }
}
