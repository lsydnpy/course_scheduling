package com.xy.course_scheduling.entity;

import com.xy.course_scheduling.algorithm.CourseSchedulingAlgorithm;
import lombok.Data;
import java.util.List;

/**
 * 排课结果
 */
@Data
public class SchedulingResult {
    private SchedulingStatus status;
    private List<Schedule> scheduledClasses;
    private List<TeachingTask> failedTasks;
    private String message;
    private Integer scheduledCount;
    private Integer failedCount;

    public SchedulingResult(SchedulingStatus status, List<Schedule> scheduledClasses,
                           List<TeachingTask> failedTasks, String message) {
        this.status = status;
        this.scheduledClasses = scheduledClasses;
        this.failedTasks = failedTasks;
        this.message = message;
        this.scheduledCount = scheduledClasses != null ? scheduledClasses.size() : 0;
        this.failedCount = failedTasks != null ? failedTasks.size() : 0;
    }

    /**
     * 从算法结果转换
     */
    public static SchedulingResult fromAlgorithmResult(CourseSchedulingAlgorithm.SchedulingResult algoResult) {
        SchedulingStatus status;
        if (algoResult.getStatus() == CourseSchedulingAlgorithm.SchedulingStatus.SUCCESS) {
            status = SchedulingStatus.SUCCESS;
        } else if (algoResult.getStatus() == CourseSchedulingAlgorithm.SchedulingStatus.PARTIAL_SUCCESS) {
            status = SchedulingStatus.PARTIAL_SUCCESS;
        } else {
            status = SchedulingStatus.FAILED;
        }

        return new SchedulingResult(
                status,
                algoResult.getScheduledClasses(),
                algoResult.getFailedTasks(),
                algoResult.getMessage()
        );
    }
}
