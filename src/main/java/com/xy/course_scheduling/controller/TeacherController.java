package com.xy.course_scheduling.controller;

import com.xy.course_scheduling.custom.annotations.OperLogAnn;
import com.xy.course_scheduling.service.CollegeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xy.course_scheduling.entity.Result;
import com.xy.course_scheduling.entity.Teacher;
import com.xy.course_scheduling.service.TeacherService;

import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 教师信息表 前端控制器
 * </p>
 *
 * @author xy
 * @since 2026-01-09
 */
@RestController
@RequestMapping("/teacher")
@Api(tags = "教师信息表")
@Slf4j
public class TeacherController {

    @Resource
    private TeacherService teacherService;
    @Resource
    private CollegeService collegeService;

    @PostMapping
    @OperLogAnn(title = "添加教师信息", businessType = OperLogAnn.BusinessType.INSERT)
    @ApiOperation(value = "添加教师信息")
    public Result<Teacher> save(Teacher teacher) {
        boolean save = teacherService.save(teacher);
        if (save) {
            return Result.ok(teacher);
        }
        return Result.fail("添加失败");
    }

    @DeleteMapping("/{id}")
    @OperLogAnn(title = "删除教师信息", businessType = OperLogAnn.BusinessType.DELETE)
    @ApiOperation(value = "删除教师信息")
    public Result<String> delete(@PathVariable Long id) {
        boolean remove = teacherService.removeById(id);
        if (remove) {
            return Result.ok("删除成功");
        }
        return Result.fail("删除失败");
    }

    @PutMapping
    @OperLogAnn(title = "修改教师信息", businessType = OperLogAnn.BusinessType.UPDATE)
    @ApiOperation(value = "修改教师信息")
    public Result<String> update(Teacher teacher) {
        boolean update = teacherService.updateById(teacher);
        if (update) {
            return Result.ok("修改成功");
        }
        return Result.fail("修改失败");
    }

    @GetMapping("/{id}")
    @OperLogAnn(title = "查询教师信息", businessType = OperLogAnn.BusinessType.SELECT)
    @ApiOperation(value = "查询教师信息")
    public Result<Teacher> getById(@PathVariable Long id) {
        return Result.ok(teacherService.getById(id));
    }

    @GetMapping("list")
    @OperLogAnn(title = "查询教师信息列表", businessType = OperLogAnn.BusinessType.SELECT)
    @ApiOperation(value = "查询教师信息列表")
    public Result<List<Teacher>> list(Teacher teacher, Page<Teacher> page) {
        LambdaQueryWrapper<Teacher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ObjectUtils.isNotEmpty(teacher.getTeacherId()), Teacher::getTeacherId, teacher.getTeacherId());
        wrapper.like(ObjectUtils.isNotEmpty(teacher.getTeacherCode()), Teacher::getTeacherCode, teacher.getTeacherCode());
        wrapper.like(ObjectUtils.isNotEmpty(teacher.getTeacherName()), Teacher::getTeacherName, teacher.getTeacherName());
        wrapper.eq(ObjectUtils.isNotEmpty(teacher.getCollegeId()), Teacher::getCollegeId, teacher.getCollegeId());
        IPage<Teacher> pageData = teacherService.page(page, wrapper);
        pageData.getRecords().forEach(item -> {
            item.setCollege(collegeService.getById(item.getCollegeId()));
        });
        return Result.ok(pageData);
    }
}