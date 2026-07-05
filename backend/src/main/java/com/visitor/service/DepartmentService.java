package com.visitor.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.visitor.common.exception.BusinessException;
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

    /** 返回带员工人数、负责人名的全部部门（平铺列表） */
    public List<Map<String, Object>> treeList() {
        return buildDeptList(list());
    }

    /** 按关键字过滤的部门列表（同样带员工数、负责人名） */
    public List<Map<String, Object>> treeListFiltered(String keyword) {
        return buildDeptList(lambdaQuery().like(Department::getName, keyword).list());
    }

    private List<Map<String, Object>> buildDeptList(List<Department> depts) {
        if (depts.isEmpty()) return Collections.emptyList();

        List<Integer> deptIds = depts.stream().map(Department::getId).collect(Collectors.toList());

        Map<Integer, Long> countMap = userService.lambdaQuery()
                .eq(User::getRole, "host")
                .in(User::getDepartmentId, deptIds)
                .list()
                .stream()
                .collect(Collectors.groupingBy(User::getDepartmentId, Collectors.counting()));

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

    /**
     * 删除部门前校验：有子部门或员工则拒绝
     */
    public void deleteWithCheck(Integer id) {
        long childCount = lambdaQuery().eq(Department::getParentId, id).count();
        if (childCount > 0) {
            throw new BusinessException("该部门下存在 " + childCount + " 个子部门，请先删除子部门");
        }
        long empCount = userService.lambdaQuery()
                .eq(User::getDepartmentId, id)
                .count();
        if (empCount > 0) {
            throw new BusinessException("该部门下存在 " + empCount + " 名员工，请先转移员工");
        }
        removeById(id);
    }
}
