package com.xy.course_scheduling.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "教学任务表")
public class TeachingTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "task_id", type = IdType.AUTO)
    @Schema(description = "教学任务 ID")
    private Integer taskId;

    @TableField("course_id")
    @Schema(description = "课程 ID")
    private Integer courseId;

    @TableField(exist = false)
    @Schema(description = "课程信息")
    private Course course;

    @TableField("campus_id")
    @Schema(description = "校区 ID")
    private Integer campusId;
    
    @TableField(exist = false)
    @Schema(description = "校区信息")
    private Campus campus;

    @TableField("part_id")
    @Schema(description = "课程分块 ID")
    private Integer partId;
    
    @TableField(exist = false)
    @Schema(description = "课程分块信息")
    private CoursePart coursePart;

    @TableField("teacher_id")
    @Schema(description = "教师 ID")
    private Integer teacherId;
    
    @TableField(exist = false)
    @Schema(description = "教师信息")
    private Teacher teacher;

    @TableField("class_id")
    @Schema(description = "班级 ID")
    private Integer classId;
    
    @TableField(exist = false)
    @Schema(description = "班级信息")
    private StudentGroup studentGroup;

    @TableField("semester_id")
    @Schema(description = "学期 ID")
    private Integer semesterId;
    
    @TableField(exist = false)
    @Schema(description = "学期信息")
    private Semester semester;

    @TableField("total_lessons")
    @Schema(description = "总课时数")
    private Integer totalLessons;

    @TableField("lessons_per_week")
    @Schema(description = "每周课时数")
    private BigDecimal lessonsPerWeek;

    @TableField("week_schedule_pattern")
    @Schema(description = "周次安排模式")
    private String weekSchedulePattern;

    @TableField("status")
    @Schema(description = "任务状态")
    private String status;

    /**
     * 是否删除
     */
    @TableField(value = "deleted", fill = FieldFill.INSERT)
    @TableLogic(value = "0", delval = "1")
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
