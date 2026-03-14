package com.xy.course_scheduling.controller;

import com.xy.course_scheduling.custom.annotations.OperLogAnn;
import com.xy.course_scheduling.service.*;
import jakarta.annotation.Resource;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xy.course_scheduling.entity.Result;
import com.xy.course_scheduling.entity.TeachingTask;

import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 教学任务表 前端控制器
 * </p>
 *
 * @author xy
 * @since 2026-01-09
 */
@RestController
@RequestMapping("/teachingTask")
public class TeachingTaskController {

    @Resource
    private TeachingTaskService teachingTaskService;
    @Resource
    private CourseService courseService;
    @Resource
    private CampusService campusService;
    @Resource
    private CoursePartService coursePartService;
    @Resource
    private TeacherService teacherService;
    @Resource
    private StudentGroupService studentGroupService;
    @Resource
    private SemesterService semesterService;

    @PostMapping
    @OperLogAnn(title = "添加教学任务", businessType = OperLogAnn.BusinessType.INSERT)
    public Result<TeachingTask> save(TeachingTask teachingTask) {
        boolean save = teachingTaskService.save(teachingTask);
        if (save) {
            return Result.ok(teachingTask);
        }
        return Result.fail("添加失败");
    }

    @DeleteMapping("/{id}")
    @OperLogAnn(title = "删除教学任务", businessType = OperLogAnn.BusinessType.DELETE)
    public Result<String> delete(@PathVariable Long id) {
        boolean remove = teachingTaskService.removeById(id);
        if (remove) {
            return Result.ok("删除成功");
        }
        return Result.fail("删除失败");
    }

    @PutMapping
    @OperLogAnn(title = "修改教学任务", businessType = OperLogAnn.BusinessType.UPDATE)
    public Result<String> update(TeachingTask teachingTask) {
        boolean update = teachingTaskService.updateById(teachingTask);
        if (update) {
            return Result.ok("修改成功");
        }
        return Result.fail("修改失败");
    }

    @GetMapping("/{id}")
    @OperLogAnn(title = "查询教学任务", businessType = OperLogAnn.BusinessType.SELECT)
    public Result<TeachingTask> getById(@PathVariable Long id) {
        return Result.ok(teachingTaskService.getById(id));
    }

    @GetMapping("list")
    @OperLogAnn(title = "查询教学任务列表", businessType = OperLogAnn.BusinessType.SELECT)
    public Result<List<TeachingTask>> list(TeachingTask teachingTask, Page<TeachingTask> page) {
        LambdaQueryWrapper<TeachingTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ObjectUtils.isNotEmpty(teachingTask.getTaskId()), TeachingTask::getTaskId, teachingTask.getTaskId());
        wrapper.eq(ObjectUtils.isNotEmpty(teachingTask.getCourseId()), TeachingTask::getCourseId, teachingTask.getCourseId());
        wrapper.eq(ObjectUtils.isNotEmpty(teachingTask.getPartId()), TeachingTask::getPartId, teachingTask.getPartId());
        wrapper.eq(ObjectUtils.isNotEmpty(teachingTask.getTeacherId()), TeachingTask::getTeacherId, teachingTask.getTeacherId());
        wrapper.eq(ObjectUtils.isNotEmpty(teachingTask.getClassId()), TeachingTask::getClassId, teachingTask.getClassId());
        wrapper.eq(ObjectUtils.isNotEmpty(teachingTask.getSemesterId()), TeachingTask::getSemesterId, teachingTask.getSemesterId());
        wrapper.eq(ObjectUtils.isNotEmpty(teachingTask.getTotalLessons()), TeachingTask::getTotalLessons, teachingTask.getTotalLessons());
        wrapper.like(ObjectUtils.isNotEmpty(teachingTask.getWeekSchedulePattern()), TeachingTask::getWeekSchedulePattern, teachingTask.getWeekSchedulePattern());
        wrapper.like(ObjectUtils.isNotEmpty(teachingTask.getStatus()), TeachingTask::getStatus, teachingTask.getStatus());
        IPage<TeachingTask> pageData = teachingTaskService.page(page, wrapper);
        pageData.getRecords().forEach(item -> {
            item.setCourse(courseService.getById(item.getCourseId()));
            item.setCampus(campusService.getById(item.getCampusId()));
            item.setCoursePart(coursePartService.getById(item.getPartId()));
            item.setTeacher(teacherService.getById(item.getTeacherId()));
            item.setStudentGroup(studentGroupService.getById(item.getClassId()));
            item.setSemester(semesterService.getById(item.getSemesterId()));
        });
        return Result.ok(pageData);
    }
}