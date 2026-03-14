package com.xy.course_scheduling.controller;

import com.xy.course_scheduling.algorithm.HybridCourseSchedulingAlgorithm;
import com.xy.course_scheduling.entity.Result;
import com.xy.course_scheduling.entity.SchedulePlan;
import com.xy.course_scheduling.entity.SchedulingResult;
import com.xy.course_scheduling.service.SemesterService;
import com.xy.course_scheduling.service.SchedulePlanService;
import jakarta.annotation.Resource;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 排课计划 前端控制器
 * </p>
 *
 * @author 张三
 * @since 2026-02-02
 */
@RestController
@RequestMapping("/schedulePlan")
public class SchedulePlanController {

    @Resource
    private SchedulePlanService schedulePlanService;
    @Resource
    private SemesterService semesterService;
    @Resource
    private HybridCourseSchedulingAlgorithm hybridCourseSchedulingAlgorithm;

    @PostMapping
    public Result<SchedulePlan> save(SchedulePlan schedulePlan) {
        // 检查该学期是否已有排课计划
        List<SchedulePlan> existingPlans = schedulePlanService.list(new LambdaQueryWrapper<SchedulePlan>()
                .eq(SchedulePlan::getSemesterId, schedulePlan.getSemesterId())
                .ne(SchedulePlan::getStatus, "排课失败")
                .ne(SchedulePlan::getStatus, "无任务")
                .eq(SchedulePlan::getDeleted, 0));

        if (!existingPlans.isEmpty()) {
            return Result.fail("该学期已有排课计划，请勿重复创建");
        }

        schedulePlan.setStatus("待排课");
        boolean save = schedulePlanService.save(schedulePlan);

        if (save) {
            // 不再自动执行排课，由前端主动调用 /schedulePlan/execute 接口
            System.out.println("排课计划已保存，请前端调用 /schedulePlan/execute 接口开始排课");
            return Result.ok(schedulePlan);
        }
        return Result.fail("添加失败");
    }

    /**
     * 执行排课（前端主动调用）
     */
    @PostMapping("/execute")
    public Result<String> executeSchedule(@RequestParam("semesterId") Integer semesterId, 
                                          @RequestParam("schedulePlanId") Integer schedulePlanId) {
        System.out.println("=== 排课请求参数 ===");
        System.out.println("semesterId: " + semesterId);
        System.out.println("schedulePlanId: " + schedulePlanId);
        
        if (semesterId == null || semesterId <= 0) {
            return Result.fail("参数错误：semesterId 无效");
        }
        if (schedulePlanId == null || schedulePlanId <= 0) {
            return Result.fail("参数错误：schedulePlanId 无效");
        }
        
        // 异步执行排课
        new Thread(() -> {
            try {
                System.out.println("=== 开始排课 ===");
                System.out.println("学期 ID: " + semesterId);
                System.out.println("排课计划 ID: " + schedulePlanId);
                schedulePlanService.executeSchedule(semesterId, schedulePlanId);
                System.out.println("=== 排课完成 ===");
            } catch (Exception e) {
                System.out.println("=== 排课失败 ===");
                e.printStackTrace();
            }
        }).start();
        
        return Result.ok("排课已启动，请稍后查看结果");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        boolean remove = schedulePlanService.removeById(id);
        if (remove) {
            return Result.ok("删除成功");
        }
        return Result.fail("删除失败");
    }

    @PutMapping
    public Result<String> update(SchedulePlan schedulePlan) {
        boolean update = schedulePlanService.updateById(schedulePlan);
        if (update) {
            return Result.ok("修改成功");
        }
        return Result.fail("修改失败");
    }

    @GetMapping("/{id}")
    public Result<SchedulePlan> getById(@PathVariable Long id) {
        return Result.ok(schedulePlanService.getById(id));
    }

    @GetMapping("list")
    public Result<List<SchedulePlan>> list(SchedulePlan schedulePlan, Page<SchedulePlan> page) {
        LambdaQueryWrapper<SchedulePlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ObjectUtils.isNotEmpty(schedulePlan.getSchedulePlanId()), SchedulePlan::getSchedulePlanId, schedulePlan.getSchedulePlanId());
        wrapper.eq(ObjectUtils.isNotEmpty(schedulePlan.getSemesterId()), SchedulePlan::getSemesterId, schedulePlan.getSemesterId());
        wrapper.like(ObjectUtils.isNotEmpty(schedulePlan.getStatus()), SchedulePlan::getStatus, schedulePlan.getStatus());
        wrapper.eq(SchedulePlan::getDeleted, 0);
        wrapper.orderByDesc(SchedulePlan::getCreatedTime);
        
        com.baomidou.mybatisplus.core.metadata.IPage<SchedulePlan> pageData = schedulePlanService.page(page, wrapper);
        pageData.getRecords().forEach(item -> item.setSemester(semesterService.getById(item.getSemesterId())));
        return Result.ok(pageData);
    }

    /**
     * 重新排课（清空现有课表后重新排课）
     */
    @PostMapping("/retry/{id}")
    public Result<String> retrySchedule(@PathVariable Integer id) {
        SchedulePlan plan = schedulePlanService.getById(id);
        if (plan == null) {
            return Result.fail("排课计划不存在");
        }

        // 异步执行重新排课
        new Thread(() -> {
            try {
                schedulePlanService.executeSchedule(plan.getSemesterId(), id);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        return Result.ok("重新排课已启动");
    }
}
