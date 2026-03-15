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
 * 课程信息表
 * </p>
 *
 * @author xy
 * @since 2026-01-09
 */
@Getter
@Setter
@TableName("tb_course")
@Schema(description = "课程信息表")
public class Course implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "course_id", type = IdType.AUTO)
    @Schema(description = "课程 ID")
    private Integer courseId;
    
    @TableField(value = "campus_id")
    @Schema(description = "校区 ID")
    private Integer campusId;
    
    @TableField(exist = false)
    @Schema(description = "校区信息")
    private Campus campus;

    @TableField("semester_id")
    @Schema(description = "学期 ID")
    private Integer semesterId;
    
    @TableField(exist = false)
    @Schema(description = "学期信息")
    private Semester semester;

    @TableField("course_code")
    @Schema(description = "课程编号")
    private String courseCode;

    @TableField("course_name")
    @Schema(description = "课程名称")
    private String courseName;

    @TableField("credit")
    @Schema(description = "学分")
    private BigDecimal credit;

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
