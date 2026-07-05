package com.visitor.controller;

import com.visitor.common.Result;
import com.visitor.common.exception.BusinessException;
import com.visitor.entity.Department;
import com.visitor.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "部门管理")
@RestController
@RequestMapping("/admin/department")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @Operation(summary = "部门列表（带员工数、负责人名）")
    @GetMapping
    public Result<?> list(@RequestParam(required = false) String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return Result.success(departmentService.treeListFiltered(keyword));
        }
        return Result.success(departmentService.treeList());
    }

    @Operation(summary = "新增部门")
    @PostMapping
    public Result<Void> add(@RequestBody Department dept) {
        if (dept.getName() == null || dept.getName().isBlank()) {
            throw new BusinessException("部门名称不能为空");
        }
        if (dept.getParentId() == null) {
            dept.setParentId(0);
        }
        if (dept.getStatus() == null) {
            dept.setStatus(1);
        }
        departmentService.save(dept);
        return Result.success("新增成功", null);
    }

    @Operation(summary = "修改部门")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Integer id, @RequestBody Department dept) {
        dept.setId(id);
        if (dept.getParentId() != null && dept.getParentId().equals(id)) {
            throw new BusinessException("不能将部门的上级设为自己");
        }
        departmentService.updateById(dept);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "删除部门（校验子部门与员工）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        departmentService.deleteWithCheck(id);
        return Result.success("删除成功", null);
    }
}
