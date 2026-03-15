package com.xy.course_scheduling.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * <p>
 * 学期信息表
 * </p>
 *
 * @author xy
 * @since 2026-01-09
 */
@Getter
@Setter
@TableName("tb_semester")
@ApiModel(value = "Semester", description = "学期信息表")
public class Semester implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "semester_id", type = IdType.AUTO)
    @ApiModelProperty(value = "学期 ID")
    private Integer semesterId;

    @TableField("semester_name")
    @ApiModelProperty(value = "学期名称")
    private String semesterName;

    @TableField("start_date")
    @ApiModelProperty(value = "开始日期")
    private LocalDate startDate;

    @TableField("end_date")
    @ApiModelProperty(value = "结束日期")
    private LocalDate endDate;

    @TableField("total_weeks")
    @ApiModelProperty(value = "总周数")
    private Integer totalWeeks;

    @TableField("is_current")
    @ApiModelProperty(value = "是否当前学期")
    private Boolean isCurrent;

    /**
     * 是否删除
     */
    @TableField(value = "deleted",fill = FieldFill.INSERT)
    @TableLogic(value = "0", delval = "1")
    @ApiModelProperty(value = "是否删除")
    private Integer deleted;

    /**
     * 更新时间
     */
    @TableField(value = "updated_time",fill = FieldFill.INSERT_UPDATE)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedTime;

    /**
     * 创建时间
     */
    @TableField(value = "created_time",fill = FieldFill.INSERT)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdTime;
}
