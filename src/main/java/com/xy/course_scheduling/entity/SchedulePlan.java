package com.xy.course_scheduling.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * <p>
 * 排课计划表
 * </p>
 *
 * @author 张三
 * @since 2026-02-02
 */
@Getter
@Setter
@TableName("tb_schedule_plan")
@ApiModel(value = "SchedulePlan", description = "排课计划表")
public class SchedulePlan implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 排课 ID
     */
    @TableId(value = "schedule_plan_id", type = IdType.AUTO)
    @ApiModelProperty(value = "排课 ID")
    private Integer schedulePlanId;

    /**
     * 排课学期
     */
    @TableField("semester_id")
    @ApiModelProperty(value = "排课学期 ID")
    private Integer semesterId;
    
    @TableField(exist = false)
    @ApiModelProperty(value = "学期信息")
    private Semester semester;

    /**
     * 排课状态
     */
    @TableField("status")
    @ApiModelProperty(value = "排课状态")
    private String status;

    /**
     * 排课成功数量
     */
    @TableField("scheduled_count")
    @ApiModelProperty(value = "排课成功数量")
    private Integer scheduledCount;

    /**
     * 排课失败数量
     */
    @TableField("failed_count")
    @ApiModelProperty(value = "排课失败数量")
    private Integer failedCount;

    /**
     * 冲突数量
     */
    @TableField("conflict_count")
    @ApiModelProperty(value = "冲突数量")
    private Integer conflictCount;

    /**
     * 排课详情/备注
     */
    @TableField("remark")
    @ApiModelProperty(value = "排课详情/备注")
    private String remark;

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
