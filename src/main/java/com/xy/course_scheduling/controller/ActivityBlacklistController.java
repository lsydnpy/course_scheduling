package com.xy.course_scheduling.controller;

import com.xy.course_scheduling.custom.annotations.OperLogAnn;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.annotation.Resource;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xy.course_scheduling.entity.Result;
import com.xy.course_scheduling.entity.ActivityBlacklist;
import com.xy.course_scheduling.service.ActivityBlacklistService;

import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 活动禁排表 前端控制器
 * </p>
 *
 * @author xy
 * @since 2026-01-09
 */
@RestController
@RequestMapping("/activityBlacklist")
@Api(tags = "活动禁排表")
@Slf4j
public class ActivityBlacklistController {

    @Resource
    private ActivityBlacklistService activityBlacklistService;

    @PostMapping
    @OperLogAnn(title = "添加活动禁排信息", businessType = OperLogAnn.BusinessType.INSERT)
    @ApiOperation(value = "添加活动禁排信息")
    public Result<ActivityBlacklist> save(ActivityBlacklist activityBlacklist) {
        boolean save = activityBlacklistService.save(activityBlacklist);
        if (save) {
            return Result.ok(activityBlacklist);
        }
        return Result.fail("添加失败");
    }

    @DeleteMapping("/{id}")
    @OperLogAnn(title = "删除活动禁排信息", businessType = OperLogAnn.BusinessType.DELETE)
    @ApiOperation(value = "删除活动禁排信息")
    public Result<String> delete(@PathVariable Long id) {
        boolean remove = activityBlacklistService.removeById(id);
        if (remove) {
            return Result.ok("删除成功");
        }
        return Result.fail("删除失败");
    }

    @PutMapping
    @OperLogAnn(title = "修改活动禁排信息", businessType = OperLogAnn.BusinessType.UPDATE)
    @ApiOperation(value = "修改活动禁排信息")
    public Result<String> update(ActivityBlacklist activityBlacklist) {
        boolean update = activityBlacklistService.updateById(activityBlacklist);
        if (update) {
            return Result.ok("修改成功");
        }
        return Result.fail("修改失败");
    }

    @GetMapping("/{id}")
    @OperLogAnn(title = "查询活动禁排信息", businessType = OperLogAnn.BusinessType.SELECT)
    @ApiOperation(value = "查询活动禁排信息")
    public Result<ActivityBlacklist> getById(@PathVariable Long id) {
        return Result.ok(activityBlacklistService.getById(id));
    }

    @GetMapping("list")
    @OperLogAnn(title = "查询活动禁排信息列表", businessType = OperLogAnn.BusinessType.SELECT)
    @ApiOperation(value = "查询活动禁排信息列表")
    public Result<List<ActivityBlacklist>> list(ActivityBlacklist activityBlacklist, Page<ActivityBlacklist> page) {
        LambdaQueryWrapper<ActivityBlacklist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ObjectUtils.isNotEmpty(activityBlacklist.getActivityId()), ActivityBlacklist::getActivityId, activityBlacklist.getActivityId());
        wrapper.like(ObjectUtils.isNotEmpty(activityBlacklist.getActivityName()), ActivityBlacklist::getActivityName, activityBlacklist.getActivityName());
        wrapper.like(ObjectUtils.isNotEmpty(activityBlacklist.getScopeType()), ActivityBlacklist::getScopeType, activityBlacklist.getScopeType());
        wrapper.eq(ObjectUtils.isNotEmpty(activityBlacklist.getCollegeId()), ActivityBlacklist::getCollegeId, activityBlacklist.getCollegeId());
        wrapper.like(ObjectUtils.isNotEmpty(activityBlacklist.getTimeSlot()), ActivityBlacklist::getTimeSlot, activityBlacklist.getTimeSlot());
        wrapper.eq(ObjectUtils.isNotEmpty(activityBlacklist.getSemesterId()), ActivityBlacklist::getSemesterId, activityBlacklist.getSemesterId());
        IPage<ActivityBlacklist> pageData = activityBlacklistService.page(page, wrapper);
        return Result.ok(pageData);
    }
}