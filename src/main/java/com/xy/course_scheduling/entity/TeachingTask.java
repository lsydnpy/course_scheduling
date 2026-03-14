package com.xy.course_scheduling.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * <p>
 * 教学任务表
 * </p>
 *
 * @author xy
 * @since 2026-01-09
 */
@Getter
@Setter
@TableName("tb_teaching_task")
public class TeachingTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "task_id", type = IdType.AUTO)
    private Integer taskId;

    @TableField("course_id")
    private Integer courseId;

    @TableField(exist = false)
    private Course course;

    @TableField("campus_id")
    private Integer campusId;
    @TableField(exist = false)
    private Campus campus;

    @TableField("part_id")
    private Integer partId;
    @TableField(exist = false)
    private CoursePart coursePart;

    @TableField("teacher_id")
    private Integer teacherId;
    @TableField(exist = false)
    private Teacher teacher;

    @TableField("class_id")
    private Integer classId;
    @TableField(exist = false)
    private StudentGroup studentGroup;

    @TableField("semester_id")
    private Integer semesterId;
    @TableField(exist = false)
    private Semester semester;

    @TableField("total_lessons")
    private Integer totalLessons;

    @TableField("lessons_per_week")
    private BigDecimal lessonsPerWeek;

    @TableField("week_schedule_pattern")
    private String weekSchedulePattern;

    @TableField("status")
    private String status;

    /**
     * 是否删除
     */
    @TableField(value = "deleted", fill = FieldFill.INSERT)
    @TableLogic(value = "0", delval = "1")
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
