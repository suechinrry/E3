package com.visitor.controller;

import com.visitor.common.Result;
import com.visitor.entity.User;
import com.visitor.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@Tag(name = "个人资料")
@RestController
@RequestMapping("/user")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
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
        userService.updateById(user);
        return Result.success("保存成功", null);
    }
}
