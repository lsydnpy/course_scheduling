package com.xy.course_scheduling.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xy.course_scheduling.entity.CourseAdjust;
import com.xy.course_scheduling.entity.Schedule;
import com.xy.course_scheduling.mapper.CourseAdjustMapper;
import com.xy.course_scheduling.service.CourseAdjustService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.course_scheduling.service.ScheduleService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * <p>
 * 调课申请表 服务实现类
 * </p>
 *
 * @author xy
 * @since 2026-03-13
 */
@Service
public class CourseAdjustServiceImpl extends ServiceImpl<CourseAdjustMapper, CourseAdjust> implements CourseAdjustService {

    @Resource
    private ScheduleService scheduleService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitAdjust(CourseAdjust adjust) {
        adjust.setStatus("PENDING");
        adjust.setApplyTime(LocalDateTime.now());
        return save(adjust);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approveAdjust(Integer adjustId, boolean approved, String remark, String approverUsername, String approverName) {
        CourseAdjust adjust = getById(adjustId);
        if (adjust == null || !"PENDING".equals(adjust.getStatus())) {
            return false;
        }

        if (approved) {
            // 创建新的课表记录
            Schedule newSchedule = new Schedule();
            newSchedule.setTaskId(adjust.getScheduleId());
            newSchedule.setClassroomId(adjust.getAdjustedClassroomId());
            newSchedule.setSemesterId(adjust.getSemesterId());
            newSchedule.setDayOfWeek(adjust.getAdjustedDayOfWeek());
            newSchedule.setLessonStart(adjust.getAdjustedLessonStart());
            newSchedule.setLessonEnd(adjust.getAdjustedLessonEnd());
            newSchedule.setWeekPattern(1L);
            scheduleService.save(newSchedule);

            // 删除原课表记录（可选）
            // scheduleService.removeById(adjust.getScheduleId());

            adjust.setStatus("APPROVED");
        } else {
            adjust.setStatus("REJECTED");
        }

        adjust.setApproverUsername(approverUsername);
        adjust.setApproverName(approverName);
        adjust.setApproveRemark(remark);
        adjust.setApproveTime(LocalDateTime.now());

        return updateById(adjust);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelAdjust(Integer adjustId) {
        CourseAdjust adjust = getById(adjustId);
        if (adjust == null || !"PENDING".equals(adjust.getStatus())) {
            return false;
        }
        adjust.setStatus("CANCELLED");
        return updateById(adjust);
    }
}
