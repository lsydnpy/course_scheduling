package com.xy.course_scheduling.controller;

import com.xy.course_scheduling.custom.annotations.OperLogAnn;
import jakarta.annotation.Resource;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xy.course_scheduling.entity.Result;
import com.xy.course_scheduling.entity.RoomType;
import com.xy.course_scheduling.service.RoomTypeService;

import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 教室类型表 前端控制器
 * </p>
 *
 * @author xy
 * @since 2026-01-09
 */
@RestController
@RequestMapping("/roomType")
public class RoomTypeController {

    @Resource
    private RoomTypeService roomTypeService;

    @PostMapping
    @OperLogAnn(title = "添加教室类型表", businessType = OperLogAnn.BusinessType.INSERT)
    public Result<RoomType> save(RoomType roomType) {
        boolean save = roomTypeService.save(roomType);
        if (save) {
            return Result.ok(roomType);
        }
        return Result.fail("添加失败");
    }

    @DeleteMapping("/{id}")
    @OperLogAnn(title = "删除教室类型表", businessType = OperLogAnn.BusinessType.DELETE)
    public Result<String> delete(@PathVariable Long id) {
        boolean remove = roomTypeService.removeById(id);
        if (remove) {
            return Result.ok("删除成功");
        }
        return Result.fail("删除失败");
    }

    @PutMapping
    @OperLogAnn(title = "修改教室类型表", businessType = OperLogAnn.BusinessType.UPDATE)
    public Result<String> update(RoomType roomType) {
        boolean update = roomTypeService.updateById(roomType);
        if (update) {
            return Result.ok("修改成功");
        }
        return Result.fail("修改失败");
    }

    @GetMapping("/{id}")
    @OperLogAnn(title = "查询教室类型表", businessType = OperLogAnn.BusinessType.SELECT)
    public Result<RoomType> getById(@PathVariable Long id) {
        return Result.ok(roomTypeService.getById(id));
    }

    @GetMapping("list")
    @OperLogAnn(title = "查询教室类型表", businessType = OperLogAnn.BusinessType.SELECT)
    public Result<List<RoomType>> list(RoomType roomType, Page<RoomType> page) {
        LambdaQueryWrapper<RoomType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ObjectUtils.isNotEmpty(roomType.getTypeId()), RoomType::getTypeId, roomType.getTypeId());
        wrapper.like(ObjectUtils.isNotEmpty(roomType.getTypeName()), RoomType::getTypeName, roomType.getTypeName());
        wrapper.like(ObjectUtils.isNotEmpty(roomType.getDescription()), RoomType::getDescription, roomType.getDescription());
        IPage<RoomType> pageData = roomTypeService.page(page, wrapper);
        return Result.ok(pageData);
    }
}