package com.xy.course_scheduling.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xy.course_scheduling.algorithm.HybridCourseSchedulingAlgorithm;
import com.xy.course_scheduling.entity.*;
import com.xy.course_scheduling.mapper.ScheduleMapper;
import com.xy.course_scheduling.mapper.SchedulePlanMapper;
import com.xy.course_scheduling.mapper.TeachingTaskMapper;
import com.xy.course_scheduling.service.SchedulePlanService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 张三
 * @since 2026-02-02
 */
@Service
public class SchedulePlanServiceImpl extends ServiceImpl<SchedulePlanMapper, SchedulePlan> implements SchedulePlanService {

    @Resource
    private HybridCourseSchedulingAlgorithm hybridCourseSchedulingAlgorithm;

    @Resource
    private ScheduleMapper scheduleMapper;

    @Resource
    private TeachingTaskMapper teachingTaskMapper;

    @Resource
    private SchedulePlanMapper schedulePlanMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SchedulingResult executeSchedule(Integer semesterId, Integer schedulePlanId) {
        try {
            // 1. 检查是否已有排课记录
            List<Schedule> existingSchedules = scheduleMapper.selectList(
                    new LambdaQueryWrapper<Schedule>()
                            .eq(Schedule::getSemesterId, semesterId)
                            .eq(Schedule::getDeleted, 0)
            );

            if (!existingSchedules.isEmpty()) {
                // 已有排课记录，返回失败
                return new SchedulingResult(
                        SchedulingStatus.FAILED,
                        null,
                        null,
                        "该学期已有排课记录，请勿重复排课。如需重新排课，请先清空现有课表。"
                );
            }

            // 2. 检查是否有待排课的教学任务
            List<TeachingTask> teachingTasks = teachingTaskMapper.selectList(
                    new LambdaQueryWrapper<TeachingTask>()
                            .eq(TeachingTask::getSemesterId, semesterId)
                            .eq(TeachingTask::getStatus, "CONFIRMED")
                            .eq(TeachingTask::getDeleted, 0)
            );

            if (teachingTasks.isEmpty()) {
                // 更新排课计划状态
                SchedulePlan plan = getById(schedulePlanId);
                if (plan != null) {
                    plan.setStatus("无任务");
                    plan.setRemark("没有需要排课的教学任务");
                    plan.setUpdatedTime(LocalDateTime.now());
                    updateById(plan);
                }

                return new SchedulingResult(
                        SchedulingStatus.SUCCESS,
                        null,
                        null,
                        "没有需要排课的教学任务"
                );
            }

            // 3. 更新排课计划状态为"正在排课"
            SchedulePlan plan = getById(schedulePlanId);
            if (plan != null) {
                plan.setStatus("正在排课");
                plan.setUpdatedTime(LocalDateTime.now());
                updateById(plan);
            }

            // 3.1 清空该学期的现有课表记录（避免重复排课导致唯一约束冲突）
            System.out.println("=== 清空该学期的现有课表记录 ===");
            LambdaQueryWrapper<Schedule> clearWrapper = new LambdaQueryWrapper<Schedule>()
                    .eq(Schedule::getSemesterId, semesterId);
            int clearedCount = scheduleMapper.delete(clearWrapper);
            System.out.println("已删除 " + clearedCount + " 条旧课表记录");

            // 4. 执行混合排课算法（启发式 + 遗传算法 + 拆分课时策略）
            System.out.println("=== 开始执行排课算法 ===");
            System.out.println("学期 ID: " + semesterId);
            System.out.println("排课计划 ID: " + schedulePlanId);
            
            HybridCourseSchedulingAlgorithm.SchedulingResult algoResult =
                hybridCourseSchedulingAlgorithm.generateScheduleHybrid(semesterId);

            System.out.println("=== 排课算法执行完成 ===");
            System.out.println("排课状态：" + algoResult.getStatus());
            System.out.println("排课消息：" + algoResult.getMessage());
            System.out.println("成功数量：" + (algoResult.getScheduledClasses() != null ? algoResult.getScheduledClasses().size() : 0));
            System.out.println("失败数量：" + (algoResult.getFailedTasks() != null ? algoResult.getFailedTasks().size() : 0));

            // 5. 保存排课结果到数据库
            List<Schedule> scheduledClasses = algoResult.getScheduledClasses();
            List<TeachingTask> failedTasks = algoResult.getFailedTasks();

            System.out.println("=== 开始保存排课结果 ===");
            System.out.println("成功排课数量：" + (scheduledClasses != null ? scheduledClasses.size() : 0));
            System.out.println("失败排课数量：" + (failedTasks != null ? failedTasks.size() : 0));

            // 保存成功的排课记录到 tb_schedule 表
            if (scheduledClasses != null && !scheduledClasses.isEmpty()) {
                int savedCount = 0;
                int duplicateCount = 0;
                int foreignKeyErrorCount = 0;
                int nullFieldCount = 0;
                int otherErrorCount = 0;
                
                System.out.println("=== 开始保存 " + scheduledClasses.size() + " 条排课记录 ===");
                
                for (Schedule schedule : scheduledClasses) {
                    try {
                        // 设置课表 ID 为 null，让数据库自动生成
                        schedule.setScheduleId(null);
                        
                        // 验证必要字段
                        if (schedule.getTaskId() == null) {
                            nullFieldCount++;
                            continue;
                        }
                        if (schedule.getClassroomId() == null) {
                            nullFieldCount++;
                            continue;
                        }
                        if (schedule.getSemesterId() == null) {
                            nullFieldCount++;
                            continue;
                        }
                        
                        scheduleMapper.insert(schedule);
                        savedCount++;
                    } catch (org.springframework.dao.DuplicateKeyException e) {
                        // 唯一约束冲突（重复排课）
                        duplicateCount++;
                    } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        // 外键约束冲突
                        foreignKeyErrorCount++;
                        System.err.println("外键约束失败：" + e.getMessage());
                        System.err.println("  task_id=" + schedule.getTaskId() + 
                            ", classroom_id=" + schedule.getClassroomId() + 
                            ", semester_id=" + schedule.getSemesterId());
                    } catch (Exception e) {
                        otherErrorCount++;
                        System.err.println("保存课表记录失败：" + e.getMessage());
                        System.err.println("  数据：" + schedule);
                        e.printStackTrace();
                    }
                }
                
                System.out.println("=== 保存结果统计 ===");
                System.out.println("成功保存：" + savedCount + " 条");
                System.out.println("重复记录：" + duplicateCount + " 条");
                System.out.println("外键失败：" + foreignKeyErrorCount + " 条");
                System.out.println("空字段：" + nullFieldCount + " 条");
                System.out.println("其他错误：" + otherErrorCount + " 条");
                System.out.println("总计：" + (savedCount + duplicateCount + foreignKeyErrorCount + nullFieldCount + otherErrorCount) + " 条");

                // 将排课成功的任务状态更新为"SCHEDULED"（已排课）
                for (Schedule schedule : scheduledClasses) {
                    TeachingTask task = new TeachingTask();
                    task.setTaskId(schedule.getTaskId());
                    task.setStatus("SCHEDULED");
                    teachingTaskMapper.updateById(task);
                }
            } else {
                System.out.println("=== 没有成功排课的记录 ===");
            }
            
            // 将排课失败的任务状态更新为"FAILED"（排课失败）
            if (failedTasks != null && !failedTasks.isEmpty()) {
                for (TeachingTask task : failedTasks) {
                    task.setStatus("FAILED");
                    teachingTaskMapper.updateById(task);
                }
                System.out.println("=== 失败任务已标记为 FAILED ===");
            } else {
                System.out.println("=== 没有失败的任务 ===");
            }

            // 6. 转换结果类型并更新排课计划状态和结果
            if (plan != null) {
                com.xy.course_scheduling.entity.SchedulingStatus entityStatus;
                if (algoResult.getStatus() == HybridCourseSchedulingAlgorithm.SchedulingStatus.SUCCESS) {
                    entityStatus = com.xy.course_scheduling.entity.SchedulingStatus.SUCCESS;
                    plan.setStatus("已完成");
                } else if (algoResult.getStatus() == HybridCourseSchedulingAlgorithm.SchedulingStatus.PARTIAL_SUCCESS) {
                    entityStatus = com.xy.course_scheduling.entity.SchedulingStatus.PARTIAL_SUCCESS;
                    plan.setStatus("部分完成");
                } else {
                    entityStatus = com.xy.course_scheduling.entity.SchedulingStatus.FAILED;
                    plan.setStatus("排课失败");
                }

                Integer scheduledCount = scheduledClasses != null ? scheduledClasses.size() : 0;
                Integer failedCount = failedTasks != null ? failedTasks.size() : 0;

                plan.setScheduledCount(scheduledCount);
                plan.setFailedCount(failedCount);
                plan.setConflictCount(0); // 冲突数量暂时设为 0
                plan.setRemark(algoResult.getMessage());
                plan.setUpdatedTime(LocalDateTime.now());
                updateById(plan);
            }

            // 转换为实体类的 SchedulingResult
            return new SchedulingResult(
                    algoResult.getStatus() == HybridCourseSchedulingAlgorithm.SchedulingStatus.SUCCESS ?
                        SchedulingStatus.SUCCESS :
                        (algoResult.getStatus() == HybridCourseSchedulingAlgorithm.SchedulingStatus.PARTIAL_SUCCESS ?
                            SchedulingStatus.PARTIAL_SUCCESS : SchedulingStatus.FAILED),
                    scheduledClasses,
                    failedTasks,
                    algoResult.getMessage()
            );

        } catch (Exception e) {
            e.printStackTrace();

            // 更新排课计划状态为失败
            SchedulePlan plan = getById(schedulePlanId);
            if (plan != null) {
                plan.setStatus("排课失败");
                plan.setRemark("排课过程中发生错误：" + e.getMessage());
                plan.setUpdatedTime(LocalDateTime.now());
                updateById(plan);
            }

            return new SchedulingResult(
                    SchedulingStatus.FAILED,
                    null,
                    null,
                    "排课失败：" + e.getMessage()
            );
        }
    }
}
