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
 * 活动禁排表
 * </p>
 *
 * @author xy
 * @since 2026-01-09
 */
@Getter
@Setter
@TableName("tb_activity_blacklist")
@ApiModel(description = "活动禁排表")
public class ActivityBlacklist implements Serializable {

    // 序列化版本号
    private static final long serialVersionUID = 1L;

    // 活动ID，主键，自动增长
    @TableId(value = "activity_id", type = IdType.AUTO)
    @ApiModelProperty(value = "活动ID")
    private Integer activityId;

    // 活动名称
    @TableField("activity_name")
    @ApiModelProperty(value = "活动名称")
    private String activityName;

    // 范围类型
    @TableField("scope_type")
    @ApiModelProperty(value = "范围类型")
    private String scopeType;

    // 学院ID
    @TableField("college_id")
    @ApiModelProperty(value = "学院ID")
    private Integer collegeId;

    // 开始日期
    @TableField("day_of_week")
    @ApiModelProperty(value = "开始日期")
    private Integer dayOfWeek;

    // 结束日期
    @TableField("campus_id")
    @ApiModelProperty(value = "结束日期")
    private String campusId;

    // 时间段
    @TableField("time_slot")
    @ApiModelProperty(value = "时间段")
    private String timeSlot;

    // 学期ID
    @TableField("semester_id")
    @ApiModelProperty(value = "学期ID")
    private Integer semesterId;

    /**
     * 是否删除
     */
    // 逻辑删除字段，插入时自动填充，默认值为0，删除时更新为1
    @TableField(value = "deleted", fill = FieldFill.INSERT)
    @TableLogic(value = "0", delval = "1")
    @ApiModelProperty(value = "是否删除")
    private Integer deleted;

    /**
     * 更新时间
     */
    // 更新时间字段，插入和更新时自动填充
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedTime;

    /**
     * 创建时间
     */
    // 创建时间字段，插入时自动填充
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdTime;
}
