package com.visitor.controller;

import com.visitor.common.Result;
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

    @Operation(summary = "部门列表")
    @GetMapping
    public Result<?> list(@RequestParam(required = false) String keyword) {
        if (keyword != null) {
            return Result.success(departmentService.lambdaQuery()
                    .like(Department::getName, keyword).list());
        }
        return Result.success(departmentService.treeList());
    }

    @Operation(summary = "新增部门")
    @PostMapping
    public Result<Void> add(@RequestBody Department dept) {
        departmentService.save(dept);
        return Result.success("新增成功", null);
    }

    @Operation(summary = "修改部门")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Integer id, @RequestBody Department dept) {
        dept.setId(id);
        departmentService.updateById(dept);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "删除部门")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        departmentService.removeById(id);
        return Result.success("删除成功", null);
    }
}
