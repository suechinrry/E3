package com.visitor.controller;

import com.visitor.common.Result;
import com.visitor.common.exception.BusinessException;
import com.visitor.dto.LoginReq;
import com.visitor.dto.LoginResp;
import com.visitor.entity.User;
import com.visitor.service.UserService;
import com.visitor.auth.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
}
