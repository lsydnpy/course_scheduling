package com.xy.course_scheduling.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * <p>
 * 课程安排表
 * </p>
 *
 * @author xy
 * @since 2026-01-09
 */
@Getter
@Setter
@TableName("tb_schedule")
@ApiModel(value = "Schedule", description = "课程安排表")
public class Schedule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "schedule_id", type = IdType.AUTO)
    @ApiModelProperty(value = "课程安排 ID")
    private Integer scheduleId;

    @TableField("task_id")
    @ApiModelProperty(value = "教学任务 ID")
    private Integer taskId;
    
    @TableField(exist = false)
    @ApiModelProperty(value = "教学任务信息")
    private TeachingTask teachingTask;

    @TableField("classroom_id")
    @ApiModelProperty(value = "教室 ID")
    private Integer classroomId;
    
    @TableField(exist = false)
    @ApiModelProperty(value = "教室信息")
    private Classroom classroom;

    @TableField("semester_id")
    @ApiModelProperty(value = "学期 ID")
    private Integer semesterId;
    
    @TableField(exist = false)
    @ApiModelProperty(value = "学期信息")
    private Semester semester;

    @TableField("day_of_week")
    @ApiModelProperty(value = "星期几")
    private Integer dayOfWeek;

    @TableField("lesson_start")
    @ApiModelProperty(value = "开始节次")
    private Integer lessonStart;

    @TableField("lesson_end")
    @ApiModelProperty(value = "结束节次")
    private Integer lessonEnd;

    @TableField("week_pattern")
    @ApiModelProperty(value = "周次模式")
    private Long weekPattern;

    /**
     * 是否删除
     */
    @TableField(value = "deleted", fill = FieldFill.INSERT)
    // @TableLogic(value = "0", delval = "1")  // 注释掉这个注解，避免覆盖自动填充的值
    @ApiModelProperty(value = "是否删除")
    private Integer deleted;

    /**
     * 更新时间
     */
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedTime;

    /**
     * 创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdTime;
}
