package com.xy.course_scheduling.controller;

import com.xy.course_scheduling.custom.annotations.OperLogAnn;
import jakarta.annotation.Resource;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xy.course_scheduling.entity.Result;
import com.xy.course_scheduling.entity.OperLog;
import com.xy.course_scheduling.service.OperLogService;

import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 系统操作日志记录表 前端控制器
 * </p>
 *
 * @author 张三
 * @since 2026-01-09
 */
@RestController
@RequestMapping("/operLog")
public class OperLogController {

    @Resource
    private OperLogService operLogService;

    @PostMapping
    @OperLogAnn(title = "添加系统操作日志记录", businessType = OperLogAnn.BusinessType.INSERT)
    public Result<OperLog> save(OperLog operLog) {
        boolean save = operLogService.save(operLog);
        if (save) {
            return Result.ok(operLog);
        }
        return Result.fail("添加失败");
    }

    @DeleteMapping("/{id}")
    @OperLogAnn(title = "删除系统操作日志记录", businessType = OperLogAnn.BusinessType.DELETE)
    public Result<String> delete(@PathVariable Long id) {
        boolean remove = operLogService.removeById(id);
        if (remove) {
            return Result.ok("删除成功");
        }
        return Result.fail("删除失败");
    }

    @PutMapping
    @OperLogAnn(title = "修改系统操作日志记录", businessType = OperLogAnn.BusinessType.UPDATE)
    public Result<String> update(OperLog operLog) {
        boolean update = operLogService.updateById(operLog);
        if (update) {
            return Result.ok("修改成功");
        }
        return Result.fail("修改失败");
    }

    @GetMapping("/{id}")
    @OperLogAnn(title = "查询系统操作日志记录", businessType = OperLogAnn.BusinessType.SELECT)
    public Result<OperLog> getById(@PathVariable Long id) {
        return Result.ok(operLogService.getById(id));
    }

    @GetMapping("list")
    @OperLogAnn(title = "查询系统操作日志记录", businessType = OperLogAnn.BusinessType.SELECT)
    public Result<List<OperLog>> list(OperLog operLog, Page<OperLog> page) {
        LambdaQueryWrapper<OperLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(ObjectUtils.isNotEmpty(operLog.getTitle()), OperLog::getTitle, operLog.getTitle());
        wrapper.like(ObjectUtils.isNotEmpty(operLog.getBusinessType()), OperLog::getBusinessType, operLog.getBusinessType());
        wrapper.like(ObjectUtils.isNotEmpty(operLog.getMethod()), OperLog::getMethod, operLog.getMethod());
        wrapper.like(ObjectUtils.isNotEmpty(operLog.getRequestMethod()), OperLog::getRequestMethod, operLog.getRequestMethod());
        wrapper.like(ObjectUtils.isNotEmpty(operLog.getOperatorUsername()), OperLog::getOperatorUsername, operLog.getOperatorUsername());
        wrapper.like(ObjectUtils.isNotEmpty(operLog.getOperatorName()), OperLog::getOperatorName, operLog.getOperatorName());
        wrapper.like(ObjectUtils.isNotEmpty(operLog.getOperUrl()), OperLog::getOperUrl, operLog.getOperUrl());
        wrapper.like(ObjectUtils.isNotEmpty(operLog.getOperIp()), OperLog::getOperIp, operLog.getOperIp());
        wrapper.like(ObjectUtils.isNotEmpty(operLog.getOperParam()), OperLog::getOperParam, operLog.getOperParam());
        wrapper.like(ObjectUtils.isNotEmpty(operLog.getJsonResult()), OperLog::getJsonResult, operLog.getJsonResult());
        wrapper.eq(ObjectUtils.isNotEmpty(operLog.getStatus()), OperLog::getStatus, operLog.getStatus());
        wrapper.like(ObjectUtils.isNotEmpty(operLog.getErrorMsg()), OperLog::getErrorMsg, operLog.getErrorMsg());
        IPage<OperLog> pageData = operLogService.page(page, wrapper);
        return Result.ok(pageData);
    }
}