package com.xy.course_scheduling.controller;

import com.xy.course_scheduling.custom.annotations.OperLogAnn;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xy.course_scheduling.entity.Result;
import com.xy.course_scheduling.entity.ScheduleWeekDetail;
import com.xy.course_scheduling.service.ScheduleWeekDetailService;

import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 排课周次详情表 前端控制器
 * </p>
 *
 * @author xy
 * @since 2026-01-09
 */
@RestController
@RequestMapping("/scheduleWeekDetail")
@Slf4j
@Tag(name = "排课周次详情表")
public class ScheduleWeekDetailController {

    @Resource
    private ScheduleWeekDetailService scheduleWeekDetailService;

    @PostMapping
    @OperLogAnn(title = "添加排课周次详情表记录", businessType = OperLogAnn.BusinessType.INSERT)
    @Operation(summary = "添加排课周次详情表记录")
    public Result<ScheduleWeekDetail> save(ScheduleWeekDetail scheduleWeekDetail) {
        boolean save = scheduleWeekDetailService.save(scheduleWeekDetail);
        if (save) {
            return Result.ok(scheduleWeekDetail);
        }
        return Result.fail("添加失败");
    }

    @DeleteMapping("/{id}")
    @OperLogAnn(title = "删除排课周次详情表记录", businessType = OperLogAnn.BusinessType.DELETE)
    @Operation(summary = "删除排课周次详情表记录")
    public Result<String> delete(@PathVariable Long id) {
        boolean remove = scheduleWeekDetailService.removeById(id);
        if (remove) {
            return Result.ok("删除成功");
        }
        return Result.fail("删除失败");
    }

    @PutMapping
    @OperLogAnn(title = "修改排课周次详情表记录", businessType = OperLogAnn.BusinessType.UPDATE)
    @Operation(summary = "修改排课周次详情表记录")
    public Result<String> update(ScheduleWeekDetail scheduleWeekDetail) {
        boolean update = scheduleWeekDetailService.updateById(scheduleWeekDetail);
        if (update) {
            return Result.ok("修改成功");
        }
        return Result.fail("修改失败");
    }

    @GetMapping("/{id}")
    @OperLogAnn(title = "查询排课周次详情表记录", businessType = OperLogAnn.BusinessType.SELECT)
    @Operation(summary = "查询排课周次详情表记录")
    public Result<ScheduleWeekDetail> getById(@PathVariable Long id) {
        return Result.ok(scheduleWeekDetailService.getById(id));
    }

    @GetMapping("list")
    @OperLogAnn(title = "查询排课周次详情表记录", businessType = OperLogAnn.BusinessType.SELECT)
    @Operation(summary = "查询排课周次详情表记录")
    public Result<List<ScheduleWeekDetail>> list(ScheduleWeekDetail scheduleWeekDetail, Page<ScheduleWeekDetail> page) {
        LambdaQueryWrapper<ScheduleWeekDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ObjectUtils.isNotEmpty(scheduleWeekDetail.getWeekNumber()), ScheduleWeekDetail::getWeekNumber, scheduleWeekDetail.getWeekNumber());
        wrapper.eq(ObjectUtils.isNotEmpty(scheduleWeekDetail.getLessonCount()), ScheduleWeekDetail::getLessonCount, scheduleWeekDetail.getLessonCount());
        IPage<ScheduleWeekDetail> pageData = scheduleWeekDetailService.page(page, wrapper);
        return Result.ok(pageData);
    }
}