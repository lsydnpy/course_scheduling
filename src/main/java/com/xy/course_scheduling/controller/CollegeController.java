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
import com.xy.course_scheduling.entity.College;
import com.xy.course_scheduling.service.CollegeService;

import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 学院信息表 前端控制器
 * </p>
 *
 * @author xy
 * @since 2026-01-09
 */
@RestController
@RequestMapping("/college")
@Tag(name = "学院信息表")
@Slf4j
public class CollegeController {

    @Resource
    private CollegeService collegeService;

    @PostMapping
    @OperLogAnn(title = "添加学院信息", businessType = OperLogAnn.BusinessType.INSERT)
    @Operation(summary = "添加学院信息")
    public Result<College> save(College college) {
        boolean save = collegeService.save(college);
        if (save) {
            return Result.ok(college);
        }
        return Result.fail("添加失败");
    }

    @DeleteMapping("/{id}")
    @OperLogAnn(title = "删除学院信息", businessType = OperLogAnn.BusinessType.DELETE)
    @Operation(summary = "删除学院信息")
    public Result<String> delete(@PathVariable Long id) {
        boolean remove = collegeService.removeById(id);
        if (remove) {
            return Result.ok("删除成功");
        }
        return Result.fail("删除失败");
    }

    @PutMapping
    @OperLogAnn(title = "修改学院信息", businessType = OperLogAnn.BusinessType.UPDATE)
    @Operation(summary = "修改学院信息")
    public Result<String> update(College college) {
        boolean update = collegeService.updateById(college);
        if (update) {
            return Result.ok("修改成功");
        }
        return Result.fail("修改失败");
    }

    @GetMapping("/{id}")
    @OperLogAnn(title = "查询学院信息", businessType = OperLogAnn.BusinessType.SELECT)
    @Operation(summary = "查询学院信息")
    public Result<College> getById(@PathVariable Long id) {
        return Result.ok(collegeService.getById(id));
    }

    @GetMapping("list")
    @OperLogAnn(title = "查询学院信息列表", businessType = OperLogAnn.BusinessType.SELECT)
    @Operation(summary = "查询学院信息列表")
    public Result<List<College>> list(College college, Page<College> page) {
        LambdaQueryWrapper<College> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ObjectUtils.isNotEmpty(college.getCollegeId()), College::getCollegeId, college.getCollegeId());
        wrapper.like(ObjectUtils.isNotEmpty(college.getCollegeName()), College::getCollegeName, college.getCollegeName());
        wrapper.eq(ObjectUtils.isNotEmpty(college.getCampusId()), College::getCampusId, college.getCampusId());
        IPage<College> pageData = collegeService.page(page, wrapper);
        return Result.ok(pageData);
    }
}