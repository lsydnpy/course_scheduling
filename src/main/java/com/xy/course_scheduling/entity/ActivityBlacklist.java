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
 * 活动禁排表
 * </p>
 *
 * @author xy
 * @since 2026-01-09
 */
@Getter
@Setter
@TableName("tb_activity_blacklist")
@Schema(description = "活动禁排表")
public class ActivityBlacklist implements Serializable {

    // 序列化版本号
    private static final long serialVersionUID = 1L;

    // 活动ID，主键，自动增长
    @TableId(value = "activity_id", type = IdType.AUTO)
    @Schema(description = "活动 ID")
    private Integer activityId;

    // 活动名称
    @TableField("activity_name")
    @Schema(description = "活动名称")
    private String activityName;

    // 范围类型
    @TableField("scope_type")
    @Schema(description = "范围类型")
    private String scopeType;

    // 学院ID
    @TableField("college_id")
    @Schema(description = "学院 ID")
    private Integer collegeId;

    // 开始日期
    @TableField("day_of_week")
    @Schema(description = "开始日期")
    private Integer dayOfWeek;

    // 结束日期
    @TableField("campus_id")
    @Schema(description = "结束日期")
    private String campusId;

    // 时间段
    @TableField("time_slot")
    @Schema(description = "时间段")
    private String timeSlot;

    // 学期ID
    @TableField("semester_id")
    @Schema(description = "学期 ID")
    private Integer semesterId;

    /**
     * 是否删除
     */
    // 逻辑删除字段，插入时自动填充，默认值为0，删除时更新为1
    @TableField(value = "deleted", fill = FieldFill.INSERT)
    @TableLogic(value = "0", delval = "1")
    @Schema(description = "是否删除")
    private Integer deleted;

    /**
     * 更新时间
     */
    // 更新时间字段，插入和更新时自动填充
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;

    /**
     * 创建时间
     */
    // 创建时间字段，插入时自动填充
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;
}
