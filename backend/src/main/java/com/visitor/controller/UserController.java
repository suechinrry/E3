package com.visitor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.PageResult;
import com.visitor.common.Result;
import com.visitor.common.exception.BusinessException;
import com.visitor.entity.User;
import com.visitor.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "员工管理")
@RestController
@RequestMapping("/admin/employee")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
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
        user.setRole("host");
        userService.save(user);
        return Result.success("新增成功", null);
    }

    @Operation(summary = "修改员工")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Integer id, @RequestBody User user) {
        user.setId(id);
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
