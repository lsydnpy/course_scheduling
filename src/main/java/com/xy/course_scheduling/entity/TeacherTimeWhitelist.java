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
 * 教师时间黑名单表
 * </p>
 *
 * @author xy
 * @since 2026-01-09
 */
@Getter
@Setter
@TableName("tb_teacher_time_whitelist")
@ApiModel(value = "TeacherTimeWhitelist", description = "教师时间白名单表")
public class TeacherTimeWhitelist implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "主键 ID")
    private Integer id;

    @TableField(exist = false)
    @ApiModelProperty(value = "教师信息")
    private Teacher teacher;

    @TableField("teacher_id")
    @ApiModelProperty(value = "教师 ID")
    private Integer teacherId;

    @TableField("semester_id")
    @ApiModelProperty(value = "学期 ID")
    private Integer semesterId;

    @TableField("day_of_week")
    @ApiModelProperty(value = "星期几")
    private Integer dayOfWeek;

    @TableField("lesson_start")
    @ApiModelProperty(value = "开始节次")
    private Integer lessonStart;

    @TableField("lesson_end")
    @ApiModelProperty(value = "结束节次")
    private Integer lessonEnd;

    /**
     * 是否删除
     */
    @TableField(value = "deleted", fill = FieldFill.INSERT)
    @TableLogic(value = "0", delval = "1")
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
