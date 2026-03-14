package com.xy.course_scheduling.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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
public class CoursePart implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "part_id", type = IdType.AUTO)
    private Integer partId;

    @TableField("course_id")
    private Integer courseId;
    @TableField(exist = false)
    private Course course;

    @TableField("part_type")
    private String partType;

    @TableField("required_room_type_id")
    private Integer requiredRoomTypeId;
    @TableField(exist = false)
    private RoomType requiredRoomType;

    @TableField("lesson_ratio")
    private BigDecimal lessonRatio;
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
