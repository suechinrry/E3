package com.visitor.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.visitor.entity.Department;
import com.visitor.entity.User;
import com.visitor.mapper.DepartmentMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DepartmentService extends ServiceImpl<DepartmentMapper, Department> {

    private final UserService userService;

    public DepartmentService(UserService userService) {
        this.userService = userService;
    }

    /** 返回带员工人数的全部部门（平铺列表） */
    public List<Map<String, Object>> treeList() {
        List<Department> depts = list();
        // 统计每个部门的员工人数
        Map<Integer, Long> countMap = userService.lambdaQuery()
                .eq(User::getRole, "host")
                .in(User::getDepartmentId, depts.stream().map(Department::getId).collect(Collectors.toList()))
                .list()
                .stream()
                .collect(Collectors.groupingBy(User::getDepartmentId, Collectors.counting()));
        // 解析负责人姓名
        Set<Integer> managerIds = depts.stream().map(Department::getManagerId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Integer, String> managerMap = Collections.emptyMap();
        if (!managerIds.isEmpty()) {
            managerMap = userService.listByIds(managerIds).stream()
                    .collect(Collectors.toMap(User::getId, User::getName));
        }
        Map<Integer, String> finalManagerMap = managerMap;
        return depts.stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.getId());
            m.put("name", d.getName());
            m.put("parentId", d.getParentId());
            m.put("managerId", d.getManagerId());
            m.put("managerName", finalManagerMap.getOrDefault(d.getManagerId(), ""));
            m.put("employeeCount", countMap.getOrDefault(d.getId(), 0L).intValue());
            m.put("status", d.getStatus());
            m.put("createTime", d.getCreateTime());
            return m;
        }).collect(Collectors.toList());
    }
}
