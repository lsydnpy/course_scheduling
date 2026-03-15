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
 * 课程分块表（理论/实践部分）
 * </p>
 *
 * @author xy
 * @since 2026-01-09
 */
@Getter
@Setter
@TableName("tb_course_part")
@Schema(description = "课程分块表（理论/实践部分）")
public class CoursePart implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "part_id", type = IdType.AUTO)
    @Schema(description = "课程分块 ID")
    private Integer partId;

    @TableField("course_id")
    @Schema(description = "课程 ID")
    private Integer courseId;
    
    @TableField(exist = false)
    @Schema(description = "课程信息")
    private Course course;

    @TableField("part_type")
    @Schema(description = "分块类型")
    private String partType;

    @TableField("required_room_type_id")
    @Schema(description = "所需教室类型 ID")
    private Integer requiredRoomTypeId;
    
    @TableField(exist = false)
    @Schema(description = "所需教室类型信息")
    private RoomType requiredRoomType;

    @TableField("lesson_ratio")
    @Schema(description = "课时比例")
    private BigDecimal lessonRatio;
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
