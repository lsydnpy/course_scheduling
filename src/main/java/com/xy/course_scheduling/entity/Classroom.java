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
 * 教室信息表
 * </p>
 *
 * @author xy
 * @since 2026-01-09
 */
@Getter
@Setter
@TableName("tb_classroom")
@Schema(description = "教室信息表")
public class Classroom implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "classroom_id", type = IdType.AUTO)
    @Schema(description = "教室 ID")
    private Integer classroomId;

    @TableField("classroom_code")
    @Schema(description = "教室编号")
    private String classroomCode;

    @TableField("campus_id")
    @Schema(description = "校区 ID")
    private Integer campusId;

    @TableField(exist = false)
    @Schema(description = "校区信息")
    private Campus campus;

    @TableField("room_type_id")
    @Schema(description = "教室类型 ID")
    private Integer roomTypeId;

    @TableField(exist = false)
    @Schema(description = "教室类型信息")
    private RoomType roomType;

    @TableField("capacity")
    @Schema(description = "教室容量")
    private Integer capacity;

    @TableField("is_available")
    @Schema(description = "教室是否可用")
    private Boolean isAvailable;

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
