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
 * 教师时间黑名单表
 * </p>
 *
 * @author xy
 * @since 2026-01-09
 */
@Getter
@Setter
@TableName("tb_teacher_time_whitelist")
@Schema(description = "教师时间白名单表")
public class TeacherTimeWhitelist implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键 ID")
    private Integer id;

    @TableField(exist = false)
    @Schema(description = "教师信息")
    private Teacher teacher;

    @TableField("teacher_id")
    @Schema(description = "教师 ID")
    private Integer teacherId;

    @TableField("semester_id")
    @Schema(description = "学期 ID")
    private Integer semesterId;

    @TableField("day_of_week")
    @Schema(description = "星期几")
    private Integer dayOfWeek;

    @TableField("lesson_start")
    @Schema(description = "开始节次")
    private Integer lessonStart;

    @TableField("lesson_end")
    @Schema(description = "结束节次")
    private Integer lessonEnd;

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
