package com.xy.course_scheduling.controller;

import com.xy.course_scheduling.custom.annotations.OperLogAnn;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xy.course_scheduling.entity.Result;
import com.xy.course_scheduling.entity.Semester;
import com.xy.course_scheduling.service.SemesterService;

import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 学期信息表 前端控制器
 * </p>
 *
 * @author xy
 * @since 2026-01-09
 */
@RestController
@RequestMapping("/semester")
@Slf4j
@Tag(name = "学期信息表")
public class SemesterController {

    @Resource
    private SemesterService semesterService;

    @PostMapping
    @OperLogAnn(title = "添加学期信息", businessType = OperLogAnn.BusinessType.INSERT)
    @Operation(summary = "添加学期信息")
    public Result<Semester> save(Semester semester) {
        boolean save = semesterService.save(semester);
        if (save) {
            return Result.ok(semester);
        }
        return Result.fail("添加失败");
    }

    @DeleteMapping("/{id}")
    @OperLogAnn(title = "删除学期信息", businessType = OperLogAnn.BusinessType.DELETE)
    @Operation(summary = "删除学期信息")
    public Result<String> delete(@PathVariable Long id) {
        boolean remove = semesterService.removeById(id);
        if (remove) {
            return Result.ok("删除成功");
        }
        return Result.fail("删除失败");
    }

    @PutMapping
    @OperLogAnn(title = "修改学期信息", businessType = OperLogAnn.BusinessType.UPDATE)
    @Operation(summary = "修改学期信息")
    public Result<String> update(Semester semester) {
        boolean update = semesterService.updateById(semester);
        if (update) {
            return Result.ok("修改成功");
        }
        return Result.fail("修改失败");
    }

    @GetMapping("/{id}")
    @OperLogAnn(title = "查询学期信息", businessType = OperLogAnn.BusinessType.SELECT)
    @Operation(summary = "查询学期信息")
    public Result<Semester> getById(@PathVariable Long id) {
        return Result.ok(semesterService.getById(id));
    }

    @GetMapping("list")
    @OperLogAnn(title = "查询学期信息", businessType = OperLogAnn.BusinessType.SELECT)
    @Operation(summary = "查询学期信息")
    public Result<List<Semester>> list(Semester semester, Page<Semester> page) {
        LambdaQueryWrapper<Semester> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ObjectUtils.isNotEmpty(semester.getSemesterId()), Semester::getSemesterId, semester.getSemesterId());
        wrapper.like(ObjectUtils.isNotEmpty(semester.getSemesterName()), Semester::getSemesterName, semester.getSemesterName());
        wrapper.eq(ObjectUtils.isNotEmpty(semester.getTotalWeeks()), Semester::getTotalWeeks, semester.getTotalWeeks());
        IPage<Semester> pageData = semesterService.page(page, wrapper);
        return Result.ok(pageData);
    }
}