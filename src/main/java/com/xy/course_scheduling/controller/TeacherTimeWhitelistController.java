package com.xy.course_scheduling.controller;

import com.xy.course_scheduling.custom.annotations.OperLogAnn;
import com.xy.course_scheduling.service.TeacherService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xy.course_scheduling.entity.Result;
import com.xy.course_scheduling.entity.TeacherTimeWhitelist;
import com.xy.course_scheduling.service.TeacherTimeWhitelistService;

import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 教师时间白名单表 前端控制器
 * </p>
 *
 * @author xy
 * @since 2026-01-09
 */
@RestController
@RequestMapping("/teacherTimeWhitelist")
@Slf4j
@Api(tags = "教师时间白名单管理")
public class TeacherTimeWhitelistController {

    @Resource
    private TeacherTimeWhitelistService teacherTimeWhitelistService;
    @Resource
    private TeacherService teacherService;

    @PostMapping
    @OperLogAnn(title = "添加教师时间白名单", businessType = OperLogAnn.BusinessType.INSERT)
    @ApiOperation(value = "添加教师时间白名单")
    public Result<TeacherTimeWhitelist> save(TeacherTimeWhitelist teacherTimeWhitelist) {
        boolean save = teacherTimeWhitelistService.save(teacherTimeWhitelist);
        if (save) {
            return Result.ok(teacherTimeWhitelist);
        }
        return Result.fail("添加失败");
    }

    @DeleteMapping("/{id}")
    @OperLogAnn(title = "删除教师时间白名单", businessType = OperLogAnn.BusinessType.DELETE)
    @ApiOperation(value = "删除教师时间白名单")
    public Result<String> delete(@PathVariable Long id) {
        boolean remove = teacherTimeWhitelistService.removeById(id);
        if (remove) {
            return Result.ok("删除成功");
        }
        return Result.fail("删除失败");
    }

    @PutMapping
    @OperLogAnn(title = "修改教师时间白名单", businessType = OperLogAnn.BusinessType.UPDATE)
    @ApiOperation(value = "修改教师时间白名单")
    public Result<String> update(TeacherTimeWhitelist teacherTimeWhitelist) {
        boolean update = teacherTimeWhitelistService.updateById(teacherTimeWhitelist);
        if (update) {
            return Result.ok("修改成功");
        }
        return Result.fail("修改失败");
    }

    @GetMapping("/{id}")
    @OperLogAnn(title = "查询教师时间白名单", businessType = OperLogAnn.BusinessType.SELECT)
    @ApiOperation(value = "查询教师时间白名单")
    public Result<TeacherTimeWhitelist> getById(@PathVariable Long id) {
        return Result.ok(teacherTimeWhitelistService.getById(id));
    }

    @GetMapping("list")
    @OperLogAnn(title = "查询教师时间白名单列表", businessType = OperLogAnn.BusinessType.SELECT)
    @ApiOperation(value = "查询教师时间白名单列表")
    public Result<List<TeacherTimeWhitelist>> list(TeacherTimeWhitelist teacherTimeWhitelist, Page<TeacherTimeWhitelist> page) {
        LambdaQueryWrapper<TeacherTimeWhitelist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ObjectUtils.isNotEmpty(teacherTimeWhitelist.getId()), TeacherTimeWhitelist::getId, teacherTimeWhitelist.getId());
        wrapper.eq(ObjectUtils.isNotEmpty(teacherTimeWhitelist.getTeacherId()), TeacherTimeWhitelist::getTeacherId, teacherTimeWhitelist.getTeacherId());
        wrapper.eq(ObjectUtils.isNotEmpty(teacherTimeWhitelist.getSemesterId()), TeacherTimeWhitelist::getSemesterId, teacherTimeWhitelist.getSemesterId());
        wrapper.eq(ObjectUtils.isNotEmpty(teacherTimeWhitelist.getDayOfWeek()), TeacherTimeWhitelist::getDayOfWeek, teacherTimeWhitelist.getDayOfWeek());
        wrapper.eq(ObjectUtils.isNotEmpty(teacherTimeWhitelist.getLessonStart()), TeacherTimeWhitelist::getLessonStart, teacherTimeWhitelist.getLessonStart());
        wrapper.eq(ObjectUtils.isNotEmpty(teacherTimeWhitelist.getLessonEnd()), TeacherTimeWhitelist::getLessonEnd, teacherTimeWhitelist.getLessonEnd());
        IPage<TeacherTimeWhitelist> pageData = teacherTimeWhitelistService.page(page, wrapper);
        pageData.getRecords().forEach(item -> {
            item.setTeacher(teacherService.getById(item.getTeacherId()));
        });
        return Result.ok(pageData);
    }
}