package com.visitor.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.visitor.entity.RolePermission;
import com.visitor.mapper.RolePermissionMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RolePermissionService extends ServiceImpl<RolePermissionMapper, RolePermission> {

    public List<String> getPermissionsByRole(String roleKey) {
        return lambdaQuery()
                .eq(RolePermission::getRoleKey, roleKey)
                .list()
                .stream()
                .map(RolePermission::getPermissionKey)
                .collect(Collectors.toList());
    }
}
