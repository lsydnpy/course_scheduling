package com.xy.course_scheduling.controller;

import com.xy.course_scheduling.custom.annotations.OperLogAnn;
import com.xy.course_scheduling.service.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xy.course_scheduling.entity.Result;
import com.xy.course_scheduling.entity.Schedule;

import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 课程安排表 前端控制器
 * </p>
 *
 * @author xy
 * @since 2026-01-09
 */
@RestController
@RequestMapping("/schedule")
@Api(tags = "课程安排表")
@Slf4j
public class ScheduleController {

    @Resource
    private ScheduleService scheduleService;
    @Resource
    private TeachingTaskService teachingTaskService;
    @Resource
    private ClassroomService classroomService;
    @Resource
    private SemesterService semesterService;
    @Resource
    private CourseService courseService;
    @Resource
    private CoursePartService coursePartService;
    @Resource
    private TeacherService teacherService;

    @PostMapping
    @OperLogAnn(title = "添加课程安排表", businessType = OperLogAnn.BusinessType.INSERT)
    @ApiOperation(value = "添加课程安排表")
    public Result<Schedule> save(Schedule schedule) {
        boolean save = scheduleService.save(schedule);
        if (save) {
            return Result.ok(schedule);
        }
        return Result.fail("添加失败");
    }

    @DeleteMapping("/{id}")
    @OperLogAnn(title = "删除课程安排表", businessType = OperLogAnn.BusinessType.DELETE)
    @ApiOperation(value = "删除课程安排表")
    public Result<String> delete(@PathVariable Long id) {
        boolean remove = scheduleService.removeById(id);
        if (remove) {
            return Result.ok("删除成功");
        }
        return Result.fail("删除失败");
    }

    @PutMapping
    @OperLogAnn(title = "修改课程安排表", businessType = OperLogAnn.BusinessType.UPDATE)
    @ApiOperation(value = "修改课程安排表")
    public Result<String> update(Schedule schedule) {
        boolean update = scheduleService.updateById(schedule);
        if (update) {
            return Result.ok("修改成功");
        }
        return Result.fail("修改失败");
    }

    @GetMapping("/{id}")
    @OperLogAnn(title = "查询课程安排表", businessType = OperLogAnn.BusinessType.SELECT)
    @ApiOperation(value = "查询课程安排表")
    public Result<Schedule> getById(@PathVariable Long id) {
        return Result.ok(scheduleService.getById(id));
    }

    @GetMapping("list")
    @OperLogAnn(title = "查询课程安排表", businessType = OperLogAnn.BusinessType.SELECT)
    @ApiOperation(value = "查询课程安排表")
    public Result<List<Schedule>> list(Schedule schedule, Page<Schedule> page) {
        if (ObjectUtils.isEmpty(schedule.getWeekPattern()))
            schedule.setWeekPattern(1L);
        else
            schedule.setWeekPattern(1L << schedule.getWeekPattern().longValue() - 1);
        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ObjectUtils.isNotEmpty(schedule.getTaskId()), Schedule::getTaskId, schedule.getTaskId());
        wrapper.eq(ObjectUtils.isNotEmpty(schedule.getClassroomId()), Schedule::getClassroomId, schedule.getClassroomId());
        wrapper.eq(ObjectUtils.isNotEmpty(schedule.getSemesterId()), Schedule::getSemesterId, schedule.getSemesterId());
        wrapper.eq(ObjectUtils.isNotEmpty(schedule.getDayOfWeek()), Schedule::getDayOfWeek, schedule.getDayOfWeek());
        wrapper.eq(ObjectUtils.isNotEmpty(schedule.getLessonStart()), Schedule::getLessonStart, schedule.getLessonStart());
        wrapper.eq(ObjectUtils.isNotEmpty(schedule.getLessonEnd()), Schedule::getLessonEnd, schedule.getLessonEnd());
        wrapper.eq(ObjectUtils.isNotEmpty(schedule.getWeekPattern()), Schedule::getWeekPattern, schedule.getWeekPattern());
        IPage<Schedule> pageData = scheduleService.page(page, wrapper);
        pageData.getRecords().forEach(item -> {
            item.setTeachingTask(teachingTaskService.getById(item.getTaskId()));
            item.setClassroom(classroomService.getById(item.getClassroomId()));
            item.setSemester(semesterService.getById(item.getSemesterId()));
            item.getTeachingTask().setCourse(courseService.getById(item.getTeachingTask().getCourseId()));
            item.getTeachingTask().setCoursePart(coursePartService.getById(item.getTeachingTask().getPartId()));
            item.getTeachingTask().setTeacher(teacherService.getById(item.getTeachingTask().getTeacherId()));
        });
        return Result.ok(pageData);
    }
}