package com.xy.course_scheduling.controller;

import com.xy.course_scheduling.custom.annotations.OperLogAnn;
import com.xy.course_scheduling.service.CampusService;
import com.xy.course_scheduling.service.SemesterService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xy.course_scheduling.entity.Result;
import com.xy.course_scheduling.entity.Course;
import com.xy.course_scheduling.service.CourseService;

import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 课程信息表 前端控制器
 * </p>
 *
 * @author xy
 * @since 2026-01-09
 */
@RestController
@RequestMapping("/course")
@Tag(name = "课程信息表")
@Slf4j
public class CourseController {

    @Resource
    private CourseService courseService;
    @Resource
    private SemesterService semesterService;
    @Resource
    private CampusService campusService;

    @PostMapping
    @OperLogAnn(title = "添加课程信息", businessType = OperLogAnn.BusinessType.INSERT)
    @Operation(summary = "添加课程信息")
    public Result<Course> save(Course course) {
        boolean save = courseService.save(course);
        if (save) {
            return Result.ok(course);
        }
        return Result.fail("添加失败");
    }

    @DeleteMapping("/{id}")
    @OperLogAnn(title = "删除课程信息", businessType = OperLogAnn.BusinessType.DELETE)
    @Operation(summary = "删除课程信息")
    public Result<String> delete(@PathVariable Long id) {
        boolean remove = courseService.removeById(id);
        if (remove) {
            return Result.ok("删除成功");
        }
        return Result.fail("删除失败");
    }

    @PutMapping
    @OperLogAnn(title = "修改课程信息", businessType = OperLogAnn.BusinessType.UPDATE)
    @Operation(summary = "修改课程信息")
    public Result<String> update(Course course) {
        boolean update = courseService.updateById(course);
        if (update) {
            return Result.ok("修改成功");
        }
        return Result.fail("修改失败");
    }

    @GetMapping("/{id}")
    @OperLogAnn(title = "查询课程信息", businessType = OperLogAnn.BusinessType.SELECT)
    @Operation(summary = "查询课程信息")
    public Result<Course> getById(@PathVariable Long id) {
        return Result.ok(courseService.getById(id));
    }

    @GetMapping("list")
    @OperLogAnn(title = "查询课程信息列表", businessType = OperLogAnn.BusinessType.SELECT)
    @Operation(summary = "查询课程信息列表")
    public Result<List<Course>> list(Course course, Page<Course> page) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ObjectUtils.isNotEmpty(course.getCourseId()), Course::getCourseId, course.getCourseId());
        wrapper.like(ObjectUtils.isNotEmpty(course.getCourseCode()), Course::getCourseCode, course.getCourseCode());
        wrapper.like(ObjectUtils.isNotEmpty(course.getCourseName()), Course::getCourseName, course.getCourseName());
        IPage<Course> pageData = courseService.page(page, wrapper);
        pageData.getRecords().forEach(item -> {
            item.setSemester(semesterService.getById(item.getSemesterId()));
            item.setCampus(campusService.getById(item.getCampusId()));
        });
        return Result.ok(pageData);
    }
}