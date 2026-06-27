package com.visitor.controller;

import com.visitor.common.Result;
import com.visitor.common.exception.BusinessException;
import com.visitor.dto.LoginReq;
import com.visitor.dto.LoginResp;
import com.visitor.dto.RegisterReq;
import com.visitor.entity.User;
import com.visitor.service.UserService;
import com.visitor.auth.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@Tag(name = "认证管理")
@RestController
@RequestMapping("/auth")
public class LoginController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public LoginController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @Operation(summary = "账号密码登录（开发用）")
    @PostMapping("/login/bypass")
    public Result<LoginResp> loginByPass(@Valid @RequestBody LoginReq req) {
        log.info(">>> 收到登录请求: username={}", req.getUsername());
        User user = userService.lambdaQuery()
                .eq(User::getUsername, req.getUsername())
                .eq(User::getPassword, req.getPassword())
                .one();
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }
        String token = jwtUtil.generate(user.getId(), user.getRole());
        LoginResp resp = new LoginResp(token, new LoginResp.UserInfo(
                user.getId(), user.getUsername(), user.getName(),
                user.getRole(), user.getPhone(),
                user.getDepartmentId() != null ? null : null,
                user.getCompany()));
        log.info("<<< 登录成功: userId={}, role={}, name={}", user.getId(), user.getRole(), user.getName());
        return Result.success(resp);
    }

    @Operation(summary = "微信登录（预留）")
    @PostMapping("/login")
    public Result<LoginResp> login(@RequestBody LoginReq req) {
        return loginByPass(req);
    }

    @Operation(summary = "访客注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterReq req) {
        log.info(">>> 收到注册请求: username={}, name={}", req.getUsername(), req.getName());

        // 检查用户名是否已存在
        Long count = userService.lambdaQuery()
                .eq(User::getUsername, req.getUsername())
                .count();
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 检查手机号是否已存在
        count = userService.lambdaQuery()
                .eq(User::getPhone, req.getPhone())
                .count();
        if (count > 0) {
            throw new BusinessException("手机号已被注册");
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(req.getPassword());
        user.setName(req.getName());
        user.setPhone(req.getPhone());
        user.setCompany(req.getCompany());
        user.setRole("visitor");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());

        userService.save(user);
        log.info("<<< 注册成功: userId={}, username={}", user.getId(), user.getUsername());
        return Result.success("注册成功", null);
    }
}
