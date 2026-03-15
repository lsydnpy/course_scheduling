package com.xy.course_scheduling.entity;

import com.xy.course_scheduling.algorithm.CourseSchedulingAlgorithm;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

/**
 * 排课结果
 */
@Data
@Schema(description = "排课结果")
public class SchedulingResult {
    @Schema(description = "排课状态")
    private SchedulingStatus status;
    
    @Schema(description = "成功排课的课程列表")
    private List<Schedule> scheduledClasses;
    
    @Schema(description = "排课失败的任务列表")
    private List<TeachingTask> failedTasks;
    
    @Schema(description = "结果消息")
    private String message;
    
    @Schema(description = "成功排课数量")
    private Integer scheduledCount;
    
    @Schema(description = "失败排课数量")
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
