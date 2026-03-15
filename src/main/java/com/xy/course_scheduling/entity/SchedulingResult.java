package com.xy.course_scheduling.entity;

import com.xy.course_scheduling.algorithm.CourseSchedulingAlgorithm;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.List;

/**
 * 排课结果
 */
@Data
@ApiModel(value = "SchedulingResult", description = "排课结果")
public class SchedulingResult {
    @ApiModelProperty(value = "排课状态")
    private SchedulingStatus status;
    
    @ApiModelProperty(value = "成功排课的课程列表")
    private List<Schedule> scheduledClasses;
    
    @ApiModelProperty(value = "排课失败的任务列表")
    private List<TeachingTask> failedTasks;
    
    @ApiModelProperty(value = "结果消息")
    private String message;
    
    @ApiModelProperty(value = "成功排课数量")
    private Integer scheduledCount;
    
    @ApiModelProperty(value = "失败排课数量")
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
