package com.xy.course_scheduling.controller;

import com.xy.course_scheduling.entity.*;
import com.xy.course_scheduling.service.*;
import com.xy.course_scheduling.service.impl.TeacherScheduleService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/classTable")
public class ClassTableController {
    @Resource
    private TeacherScheduleService teacherScheduleService;


    @GetMapping
    public Result<List<ClassTable>> getTeacherClassTable(Integer teacherId) {
        List<ClassTable> classTableList = teacherScheduleService.getTeacherSchedule(teacherId);
        return Result.ok(classTableList);
    }


}
