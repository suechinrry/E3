package com.visitor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.Result;
import com.visitor.entity.RolePermission;
import com.visitor.service.RolePermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "角色权限")
@RestController
@RequestMapping("/admin/role")
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;

    public RolePermissionController(RolePermissionService rolePermissionService) {
        this.rolePermissionService = rolePermissionService;
    }

    @Operation(summary = "获取所有角色权限")
    @GetMapping
    public Result<?> allRoles() {
        return Result.success(rolePermissionService.list().stream()
                .collect(java.util.stream.Collectors.groupingBy(RolePermission::getRoleKey)));
    }

    @Operation(summary = "更新角色权限")
    @PutMapping("/{roleKey}")
    public Result<Void> update(@PathVariable String roleKey, @RequestBody Map<String, List<String>> body) {
        rolePermissionService.remove(
                new LambdaQueryWrapper<RolePermission>()
                        .eq(RolePermission::getRoleKey, roleKey));
        List<String> perms = body.get("permissions");
        if (perms != null) {
            for (String p : perms) {
                RolePermission rp = new RolePermission();
                rp.setRoleKey(roleKey);
                rp.setPermissionKey(p);
                rp.setPermissionName(p);
                rolePermissionService.save(rp);
            }
        }
        return Result.success("更新成功", null);
    }
}
