package com.xy.course_scheduling.service;

import com.xy.course_scheduling.entity.SchedulePlan;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.course_scheduling.entity.SchedulingResult;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 张三
 * @since 2026-02-02
 */
public interface SchedulePlanService extends IService<SchedulePlan> {

    /**
     * 执行排课（防止重复排课）
     */
    SchedulingResult executeSchedule(Integer semesterId, Integer schedulePlanId);
}
