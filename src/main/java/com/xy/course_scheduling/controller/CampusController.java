package com.xy.course_scheduling.controller;

import com.xy.course_scheduling.custom.annotations.OperLogAnn;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xy.course_scheduling.entity.Result;
import com.xy.course_scheduling.entity.Campus;
import com.xy.course_scheduling.service.CampusService;

import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 校区信息表 前端控制器
 * </p>
 *
 * @author xy
 * @since 2026-01-09
 */
@RestController
@RequestMapping("/campus")
@Api(tags = "校区信息表")
@Slf4j
public class CampusController {

    @Resource

    private CampusService campusService;

    @PostMapping
    @OperLogAnn(title = "添加校区信息", businessType = OperLogAnn.BusinessType.INSERT)
    public Result<Campus> save(Campus campus) {
        boolean save = campusService.save(campus);
        if (save) {
            return Result.ok(campus);
        }
        return Result.fail("添加失败");
    }

    @DeleteMapping("/{id}")
    @OperLogAnn(title = "删除校区信息", businessType = OperLogAnn.BusinessType.DELETE)
    @ApiOperation(value = "删除校区信息")
    public Result<String> delete(@PathVariable Long id) {
        boolean remove = campusService.removeById(id);
        if (remove) {
            return Result.ok("删除成功");
        }
        return Result.fail("删除失败");
    }

    @PutMapping
    @OperLogAnn(title = "修改校区信息", businessType = OperLogAnn.BusinessType.UPDATE)
    @ApiOperation(value = "修改校区信息")
    public Result<String> update(Campus campus) {
        boolean update = campusService.updateById(campus);
        if (update) {
            return Result.ok("修改成功");
        }
        return Result.fail("修改失败");
    }

    @GetMapping("/{id}")
    @OperLogAnn(title = "查询校区信息", businessType = OperLogAnn.BusinessType.SELECT)
    @ApiOperation(value = "查询校区信息")
    public Result<Campus> getById(@PathVariable Long id) {
        return Result.ok(campusService.getById(id));
    }

    @GetMapping("list")
    @OperLogAnn(title = "查询校区信息列表", businessType = OperLogAnn.BusinessType.SELECT)
    @ApiOperation(value = "查询校区信息列表")
    public Result<List<Campus>> list(Campus campus, Page<Campus> page) {
        LambdaQueryWrapper<Campus> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ObjectUtils.isNotEmpty(campus.getCampusId()), Campus::getCampusId, campus.getCampusId());
        wrapper.like(ObjectUtils.isNotEmpty(campus.getCampusName()), Campus::getCampusName, campus.getCampusName());
        wrapper.eq(ObjectUtils.isNotEmpty(campus.getDailyLessonLimit()), Campus::getDailyLessonLimit, campus.getDailyLessonLimit());
        IPage<Campus> pageData = campusService.page(page, wrapper);
        return Result.ok(pageData);
    }
}