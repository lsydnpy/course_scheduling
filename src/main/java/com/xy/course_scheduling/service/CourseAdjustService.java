package com.xy.course_scheduling.service;

import com.xy.course_scheduling.entity.CourseAdjust;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 调课申请表 服务类
 * </p>
 *
 * @author xy
 * @since 2026-03-13
 */
public interface CourseAdjustService extends IService<CourseAdjust> {

    /**
     * 提交调课申请
     */
    boolean submitAdjust(CourseAdjust adjust);

    /**
     * 审批调课申请
     */
    boolean approveAdjust(Integer adjustId, boolean approved, String remark, String approverUsername, String approverName);

    /**
     * 撤销调课申请
     */
    boolean cancelAdjust(Integer adjustId);
}
