package com.visitor.controller;

import com.visitor.common.Result;
import com.visitor.entity.Holiday;
import com.visitor.service.HolidayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "节假日设置")
@RestController
@RequestMapping("/admin/holiday")
public class HolidayController {

    private final HolidayService holidayService;

    public HolidayController(HolidayService holidayService) {
        this.holidayService = holidayService;
    }

    @Operation(summary = "节假日列表")
    @GetMapping
    public Result<?> list(@RequestParam(required = false) Integer year) {
        if (year != null) {
            return Result.success(holidayService.lambdaQuery()
                    .apply("YEAR(date) = {0}", year).list());
        }
        return Result.success(holidayService.list());
    }

    @Operation(summary = "新增节假日")
    @PostMapping
    public Result<Void> add(@RequestBody Holiday h) {
        holidayService.save(h);
        return Result.success("新增成功", null);
    }

    @Operation(summary = "删除节假日")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        holidayService.removeById(id);
        return Result.success("删除成功", null);
    }
}
