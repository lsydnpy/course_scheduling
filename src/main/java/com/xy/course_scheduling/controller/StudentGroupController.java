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
import com.xy.course_scheduling.entity.StudentGroup;
import com.xy.course_scheduling.service.StudentGroupService;

import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 班级信息表 前端控制器
 * </p>
 *
 * @author xy
 * @since 2026-01-09
 */
@RestController
@RequestMapping("/studentGroup")
@Slf4j
@Api(tags = "班级信息表")
public class StudentGroupController {

    @Resource
    private StudentGroupService studentGroupService;
    @Resource
    private CollegeService collegeService;

    @PostMapping
    @OperLogAnn(title = "添加班级信息", businessType = OperLogAnn.BusinessType.INSERT)
    @ApiOperation(value = "添加班级信息")
    public Result<StudentGroup> save(StudentGroup studentGroup) {
        boolean save = studentGroupService.save(studentGroup);
        if (save) {
            return Result.ok(studentGroup);
        }
        return Result.fail("添加失败");
    }

    @DeleteMapping("/{id}")
    @OperLogAnn(title = "删除班级信息", businessType = OperLogAnn.BusinessType.DELETE)
    @ApiOperation(value = "删除班级信息")
    public Result<String> delete(@PathVariable Long id) {
        boolean remove = studentGroupService.removeById(id);
        if (remove) {
            return Result.ok("删除成功");
        }
        return Result.fail("删除失败");
    }

    @PutMapping
    @OperLogAnn(title = "修改班级信息", businessType = OperLogAnn.BusinessType.UPDATE)
    @ApiOperation(value = "修改班级信息")
    public Result<String> update(StudentGroup studentGroup) {
        boolean update = studentGroupService.updateById(studentGroup);
        if (update) {
            return Result.ok("修改成功");
        }
        return Result.fail("修改失败");
    }

    @GetMapping("/{id}")
    @OperLogAnn(title = "查询班级信息", businessType = OperLogAnn.BusinessType.SELECT)
    @ApiOperation(value = "查询班级信息")
    public Result<StudentGroup> getById(@PathVariable Long id) {
        return Result.ok(studentGroupService.getById(id));
    }

    @GetMapping("list")
    @OperLogAnn(title = "查询班级信息", businessType = OperLogAnn.BusinessType.SELECT)
    @ApiOperation(value = "查询班级信息")
    public Result<List<StudentGroup>> list(StudentGroup studentGroup, Page<StudentGroup> page) {
        LambdaQueryWrapper<StudentGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ObjectUtils.isNotEmpty(studentGroup.getClassId()), StudentGroup::getClassId, studentGroup.getClassId());
        wrapper.like(ObjectUtils.isNotEmpty(studentGroup.getClassName()), StudentGroup::getClassName, studentGroup.getClassName());
        wrapper.eq(ObjectUtils.isNotEmpty(studentGroup.getStudentCount()), StudentGroup::getStudentCount, studentGroup.getStudentCount());
        wrapper.eq(ObjectUtils.isNotEmpty(studentGroup.getCollegeId()), StudentGroup::getCollegeId, studentGroup.getCollegeId());
        IPage<StudentGroup> pageData = studentGroupService.page(page, wrapper);
        pageData.getRecords().forEach(item -> {
            item.setCollege(collegeService.getById(item.getCollegeId()));
        });
        return Result.ok(pageData);
    }
}