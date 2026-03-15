package com.xy.course_scheduling.controller;

import com.xy.course_scheduling.custom.annotations.OperLogAnn;
import com.xy.course_scheduling.service.CampusService;
import com.xy.course_scheduling.service.RoomTypeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xy.course_scheduling.entity.Result;
import com.xy.course_scheduling.entity.Classroom;
import com.xy.course_scheduling.service.ClassroomService;

import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 教室信息表 前端控制器
 * </p>
 *
 * @author xy
 * @since 2026-01-09
 */
@RestController
@RequestMapping("/classroom")
@Tag(name = "教室信息表")
@Slf4j

public class ClassroomController {

    @Resource
    private ClassroomService classroomService;
    @Resource
    private CampusService campusService;
    @Resource
    private RoomTypeService roomTypeService;

    @PostMapping
    @OperLogAnn(title = "添加教室信息", businessType = OperLogAnn.BusinessType.INSERT)
    @Operation(summary = "添加教室信息")
    public Result<Classroom> save(Classroom classroom) {
        boolean save = classroomService.save(classroom);
        if (save) {
            return Result.ok(classroom);
        }
        return Result.fail("添加失败");
    }

    @DeleteMapping("/{id}")
    @OperLogAnn(title = "删除教室信息", businessType = OperLogAnn.BusinessType.DELETE)
    @Operation(summary = "删除教室信息")
    public Result<String> delete(@PathVariable Long id) {
        boolean remove = classroomService.removeById(id);
        if (remove) {
            return Result.ok("删除成功");
        }
        return Result.fail("删除失败");
    }

    @PutMapping
    @OperLogAnn(title = "修改教室信息", businessType = OperLogAnn.BusinessType.UPDATE)
    @Operation(summary = "修改教室信息")
    public Result<String> update(Classroom classroom) {
        boolean update = classroomService.updateById(classroom);
        if (update) {
            return Result.ok("修改成功");
        }
        return Result.fail("修改失败");
    }

    @GetMapping("/{id}")
    @OperLogAnn(title = "查询教室信息", businessType = OperLogAnn.BusinessType.SELECT)
    @Operation(summary = "查询教室信息")
    public Result<Classroom> getById(@PathVariable Long id) {
        return Result.ok(classroomService.getById(id));
    }

    @GetMapping("list")
    @OperLogAnn(title = "查询教室信息列表", businessType = OperLogAnn.BusinessType.SELECT)
    @Operation(summary = "查询教室信息列表")
    public Result<List<Classroom>> list(Classroom classroom, Page<Classroom> page) {
        LambdaQueryWrapper<Classroom> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ObjectUtils.isNotEmpty(classroom.getClassroomId()), Classroom::getClassroomId, classroom.getClassroomId());
        wrapper.like(ObjectUtils.isNotEmpty(classroom.getClassroomCode()), Classroom::getClassroomCode, classroom.getClassroomCode());
        wrapper.eq(ObjectUtils.isNotEmpty(classroom.getCampusId()), Classroom::getCampusId, classroom.getCampusId());
        wrapper.eq(ObjectUtils.isNotEmpty(classroom.getRoomTypeId()), Classroom::getRoomTypeId, classroom.getRoomTypeId());
        wrapper.eq(ObjectUtils.isNotEmpty(classroom.getCapacity()), Classroom::getCapacity, classroom.getCapacity());
        IPage<Classroom> pageData = classroomService.page(page, wrapper);
        pageData.getRecords().forEach(item -> {
            item.setCampus(campusService.getById(item.getCampusId()));
            item.setRoomType(roomTypeService.getById(item.getRoomTypeId()));
        });
        return Result.ok(pageData);
    }
}