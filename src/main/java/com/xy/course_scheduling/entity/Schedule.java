package com.xy.course_scheduling.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;

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
public class Schedule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "schedule_id", type = IdType.AUTO)
    private Integer scheduleId;

    @TableField("task_id")
    private Integer taskId;
    @TableField(exist = false)
    private TeachingTask teachingTask;

    @TableField("classroom_id")
    private Integer classroomId;
    @TableField(exist = false)
    private Classroom classroom;

    @TableField("semester_id")
    private Integer semesterId;
    @TableField(exist = false)
    private Semester semester;

    @TableField("day_of_week")
    private Integer dayOfWeek;

    @TableField("lesson_start")
    private Integer lessonStart;

    @TableField("lesson_end")
    private Integer lessonEnd;

    @TableField("week_pattern")
    private Long weekPattern;

    /**
     * 是否删除
     */
    @TableField(value = "deleted", fill = FieldFill.INSERT)
    // @TableLogic(value = "0", delval = "1")  // 注释掉这个注解，避免覆盖自动填充的值
    private Integer deleted;

    /**
     * 更新时间
     */
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;

    /**
     * 创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
}
