package com.xy.course_scheduling.controller;

import com.xy.course_scheduling.custom.annotations.OperLogAnn;
import com.xy.course_scheduling.service.CourseService;
import com.xy.course_scheduling.service.RoomTypeService;
import jakarta.annotation.Resource;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xy.course_scheduling.entity.Result;
import com.xy.course_scheduling.entity.CoursePart;
import com.xy.course_scheduling.service.CoursePartService;

import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 课程分块表（理论/实践部分） 前端控制器
 * </p>
 *
 * @author xy
 * @since 2026-01-09
 */
@RestController
@RequestMapping("/coursePart")
public class CoursePartController {

    @Resource
    private CoursePartService coursePartService;
    @Resource
    private CourseService courseService;
    @Resource
    private RoomTypeService roomTypeService;

    @PostMapping
    @OperLogAnn(title = "添加课程分块表（理论/实践部分）", businessType = OperLogAnn.BusinessType.INSERT)
    public Result<CoursePart> save(CoursePart coursePart) {
        boolean save = coursePartService.save(coursePart);
        if (save) {
            return Result.ok(coursePart);
        }
        return Result.fail("添加失败");
    }

    @DeleteMapping("/{id}")
    @OperLogAnn(title = "删除课程分块表（理论/实践部分）", businessType = OperLogAnn.BusinessType.DELETE)
    public Result<String> delete(@PathVariable Long id) {
        boolean remove = coursePartService.removeById(id);
        if (remove) {
            return Result.ok("删除成功");
        }
        return Result.fail("删除失败");
    }

    @PutMapping
    @OperLogAnn(title = "修改课程分块表（理论/实践部分）", businessType = OperLogAnn.BusinessType.UPDATE)
    public Result<String> update(CoursePart coursePart) {
        boolean update = coursePartService.updateById(coursePart);
        if (update) {
            return Result.ok("修改成功");
        }
        return Result.fail("修改失败");
    }

    @GetMapping("/{id}")
    @OperLogAnn(title = "查询课程分块表（理论/实践部分）", businessType = OperLogAnn.BusinessType.SELECT)
    public Result<CoursePart> getById(@PathVariable Long id) {
        return Result.ok(coursePartService.getById(id));
    }

    @GetMapping("list")
    @OperLogAnn(title = "查询课程分块表（理论/实践部分）列表", businessType = OperLogAnn.BusinessType.SELECT)
    public Result<List<CoursePart>> list(CoursePart coursePart, Page<CoursePart> page) {
        LambdaQueryWrapper<CoursePart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ObjectUtils.isNotEmpty(coursePart.getPartId()), CoursePart::getPartId, coursePart.getPartId());
        wrapper.eq(ObjectUtils.isNotEmpty(coursePart.getCourseId()), CoursePart::getCourseId, coursePart.getCourseId());
        wrapper.like(ObjectUtils.isNotEmpty(coursePart.getPartType()), CoursePart::getPartType, coursePart.getPartType());
        wrapper.eq(ObjectUtils.isNotEmpty(coursePart.getRequiredRoomTypeId()), CoursePart::getRequiredRoomTypeId, coursePart.getRequiredRoomTypeId());
        IPage<CoursePart> pageData = coursePartService.page(page, wrapper);
        pageData.getRecords().forEach(item -> {
            item.setCourse(courseService.getById(item.getCourseId()));
            item.setRequiredRoomType(roomTypeService.getById(item.getRequiredRoomTypeId()));
        });
        return Result.ok(pageData);
    }
}