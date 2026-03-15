package com.xy.course_scheduling.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "课程安排表")
public class Schedule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "schedule_id", type = IdType.AUTO)
    @Schema(description = "课程安排 ID")
    private Integer scheduleId;

    @TableField("task_id")
    @Schema(description = "教学任务 ID")
    private Integer taskId;
    
    @TableField(exist = false)
    @Schema(description = "教学任务信息")
    private TeachingTask teachingTask;

    @TableField("classroom_id")
    @Schema(description = "教室 ID")
    private Integer classroomId;
    
    @TableField(exist = false)
    @Schema(description = "教室信息")
    private Classroom classroom;

    @TableField("semester_id")
    @Schema(description = "学期 ID")
    private Integer semesterId;
    
    @TableField(exist = false)
    @Schema(description = "学期信息")
    private Semester semester;

    @TableField("day_of_week")
    @Schema(description = "星期几")
    private Integer dayOfWeek;

    @TableField("lesson_start")
    @Schema(description = "开始节次")
    private Integer lessonStart;

    @TableField("lesson_end")
    @Schema(description = "结束节次")
    private Integer lessonEnd;

    @TableField("week_pattern")
    @Schema(description = "周次模式")
    private Long weekPattern;

    /**
     * 是否删除
     */
    @TableField(value = "deleted", fill = FieldFill.INSERT)
    // @TableLogic(value = "0", delval = "1")  // 注释掉这个注解，避免覆盖自动填充的值
    @Schema(description = "是否删除")
    private Integer deleted;

    /**
     * 更新时间
     */
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;

    /**
     * 创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;
}
