package com.xy.course_scheduling.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xy.course_scheduling.entity.Result;
import com.xy.course_scheduling.entity.TeachingTask;
import com.xy.course_scheduling.service.*;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 仪表盘统计接口
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Resource
    private CampusService campusService;
    
    @Resource
    private ClassroomService classroomService;
    
    @Resource
    private TeacherService teacherService;
    
    @Resource
    private CourseService courseService;
    
    @Resource
    private TeachingTaskService teachingTaskService;
    
    @Resource
    private StudentGroupService studentGroupService;
    
    @Resource
    private CollegeService collegeService;
    
    @Resource
    private SemesterService semesterService;

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 校区数量
        stats.put("campusCount", campusService.count());
        
        // 教室数量
        stats.put("classroomCount", classroomService.count());
        
        // 教师数量
        stats.put("teacherCount", teacherService.count());
        
        // 课程数量
        stats.put("courseCount", courseService.count());
        
        // 班级数量
        stats.put("classCount", studentGroupService.count());
        
        // 学院数量
        stats.put("collegeCount", collegeService.count());
        
        // 学期数量
        stats.put("semesterCount", semesterService.count());
        
        // 教学任务统计
        long taskCount = teachingTaskService.count();
        
        LambdaQueryWrapper<TeachingTask> scheduledWrapper = new LambdaQueryWrapper<>();
        scheduledWrapper.eq(TeachingTask::getStatus, "SCHEDULED");
        long scheduledCount = teachingTaskService.count(scheduledWrapper);
        
        long pendingCount = taskCount - scheduledCount;
        
        stats.put("taskCount", taskCount);
        stats.put("scheduledCount", scheduledCount);
        stats.put("pendingCount", pendingCount);
        stats.put("scheduledPercent", taskCount > 0 ? Math.round((float) scheduledCount / taskCount * 100) : 0);
        stats.put("pendingPercent", taskCount > 0 ? Math.round((float) pendingCount / taskCount * 100) : 0);
        
        return Result.ok(stats);
    }

    @GetMapping("/task-stats")
    public Result<Map<String, Object>> getTaskStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long taskCount = teachingTaskService.count();
        
        LambdaQueryWrapper<TeachingTask> scheduledWrapper = new LambdaQueryWrapper<>();
        scheduledWrapper.eq(TeachingTask::getStatus, "SCHEDULED");
        long scheduledCount = teachingTaskService.count(scheduledWrapper);
        
        LambdaQueryWrapper<TeachingTask> draftWrapper = new LambdaQueryWrapper<>();
        draftWrapper.eq(TeachingTask::getStatus, "DRAFT");
        long draftCount = teachingTaskService.count(draftWrapper);
        
        LambdaQueryWrapper<TeachingTask> confirmedWrapper = new LambdaQueryWrapper<>();
        confirmedWrapper.eq(TeachingTask::getStatus, "CONFIRMED");
        long confirmedCount = teachingTaskService.count(confirmedWrapper);
        
        stats.put("total", taskCount);
        stats.put("scheduled", scheduledCount);
        stats.put("draft", draftCount);
        stats.put("confirmed", confirmedCount);
        
        return Result.ok(stats);
    }
}
