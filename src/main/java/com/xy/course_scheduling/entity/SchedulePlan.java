package com.xy.course_scheduling.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

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
public class SchedulePlan implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 排课 ID
     */
    @TableId(value = "schedule_plan_id", type = IdType.AUTO)
    private Integer schedulePlanId;

    /**
     * 排课学期
     */
    @TableField("semester_id")
    private Integer semesterId;
    @TableField(exist = false)
    private Semester semester;

    /**
     * 排课状态
     */
    @TableField("status")
    private String status;

    /**
     * 排课成功数量
     */
    @TableField("scheduled_count")
    private Integer scheduledCount;

    /**
     * 排课失败数量
     */
    @TableField("failed_count")
    private Integer failedCount;

    /**
     * 冲突数量
     */
    @TableField("conflict_count")
    private Integer conflictCount;

    /**
     * 排课详情/备注
     */
    @TableField("remark")
    private String remark;

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
