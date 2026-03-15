package com.xy.course_scheduling.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * <p>
 * 调课申请表
 * </p>
 *
 * @author xy
 * @since 2026-03-13
 */
@Getter
@Setter
@TableName("tb_course_adjust")
@Schema(description = "调课申请表")
public class CourseAdjust implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "adjust_id", type = IdType.AUTO)
    @Schema(description = "调课申请 ID")
    private Integer adjustId;

    /**
     * 原课表 ID
     */
    @TableField("schedule_id")
    @Schema(description = "原课程安排 ID")
    private Integer scheduleId;

    /**
     * 学期 ID
     */
    @TableField("semester_id")
    @Schema(description = "学期 ID")
    private Integer semesterId;

    /**
     * 课程 ID
     */
    @TableField("course_id")
    @Schema(description = "课程 ID")
    private Integer courseId;

    /**
     * 课程名称
     */
    @TableField("course_name")
    @Schema(description = "课程名称")
    private String courseName;

    /**
     * 教师 ID
     */
    @TableField("teacher_id")
    @Schema(description = "教师 ID")
    private Integer teacherId;

    /**
     * 教师名称
     */
    @TableField("teacher_name")
    @Schema(description = "教师姓名")
    private String teacherName;

    /**
     * 班级 ID
     */
    @TableField("class_id")
    @Schema(description = "班级 ID")
    private Integer classId;

    /**
     * 班级名称
     */
    @TableField("class_name")
    @Schema(description = "班级名称")
    private String className;

    /**
     * 原星期
     */
    @TableField("original_day_of_week")
    private Integer originalDayOfWeek;

    /**
     * 原开始节次
     */
    @TableField("original_lesson_start")
    @Schema(description = "原开始节次")
    private Integer originalLessonStart;

    /**
     * 原结束节次
     */
    @TableField("original_lesson_end")
    @Schema(description = "原结束节次")
    private Integer originalLessonEnd;

    /**
     * 原教室 ID
     */
    @TableField("original_classroom_id")
    @Schema(description = "原教室 ID")
    private Integer originalClassroomId;

    /**
     * 原教室代码
     */
    @TableField("original_classroom_code")
    @Schema(description = "原教室代码")
    private String originalClassroomCode;

    /**
     * 调整后星期
     */
    @TableField("adjusted_day_of_week")
    @Schema(description = "调整后星期")
    private Integer adjustedDayOfWeek;

    /**
     * 调整后开始节次
     */
    @TableField("adjusted_lesson_start")
    @Schema(description = "调整后开始节次")
    private Integer adjustedLessonStart;

    /**
     * 调整后结束节次
     */
    @TableField("adjusted_lesson_end")
    @Schema(description = "调整后结束节次")
    private Integer adjustedLessonEnd;

    /**
     * 调整后教室 ID
     */
    @TableField("adjusted_classroom_id")
    @Schema(description = "调整后教室 ID")
    private Integer adjustedClassroomId;

    /**
     * 调整后教室代码
     */
    @TableField("adjusted_classroom_code")
    @Schema(description = "调整后教室代码")
    private String adjustedClassroomCode;

    /**
     * 调课原因
     */
    @TableField("reason")
    @Schema(description = "调课原因")
    private String reason;

    /**
     * 备注
     */
    @TableField("remark")
    @Schema(description = "备注")
    private String remark;

    /**
     * 状态：PENDING-待审批，APPROVED-已通过，REJECTED-已拒绝，CANCELLED-已取消
     */
    @TableField("status")
    @Schema(description = "申请状态")
    private String status;

    /**
     * 申请人用户名
     */
    @TableField("applicant_username")
    @Schema(description = "申请人用户名")
    private String applicantUsername;

    /**
     * 申请人姓名
     */
    @TableField("applicant_name")
    @Schema(description = "申请人姓名")
    private String applicantName;

    /**
     * 审批人用户名
     */
    @TableField("approver_username")
    @Schema(description = "审批人用户名")
    private String approverUsername;

    /**
     * 审批人姓名
     */
    @TableField("approver_name")
    @Schema(description = "审批人姓名")
    private String approverName;

    /**
     * 审批意见
     */
    @TableField("approve_remark")
    @Schema(description = "审批意见")
    private String approveRemark;

    /**
     * 申请时间
     */
    @TableField(value = "apply_time", fill = FieldFill.INSERT)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "申请时间")
    private LocalDateTime applyTime;

    /**
     * 审批时间
     */
    @TableField("approve_time")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "审批时间")
    private LocalDateTime approveTime;

    /**
     * 是否删除
     */
    @TableField(value = "deleted", fill = FieldFill.INSERT)
    @TableLogic(value = "0", delval = "1")
    @Schema(description = "是否删除")
    private Integer deleted;

    /**
     * 创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;
}
