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
 * 教室信息表
 * </p>
 *
 * @author xy
 * @since 2026-01-09
 */
@Getter
@Setter
@TableName("tb_classroom")
@ApiModel(description = "教室信息表")
public class Classroom implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "classroom_id", type = IdType.AUTO)
    @ApiModelProperty(value = "教室ID")
    private Integer classroomId;

    @TableField("classroom_code")
    @ApiModelProperty(value = "教室编号")
    private String classroomCode;

    @TableField("campus_id")
    @ApiModelProperty(value = "校区ID")
    private Integer campusId;

    @TableField(exist = false)
    @ApiModelProperty(value = "校区信息")
    private Campus campus;

    @TableField("room_type_id")
    @ApiModelProperty(value = "教室类型ID")
    private Integer roomTypeId;

    @TableField(exist = false)
    @ApiModelProperty(value = "教室类型信息")
    private RoomType roomType;

    @TableField("capacity")
    @ApiModelProperty(value = "教室容量")
    private Integer capacity;

    @TableField("is_available")
    @ApiModelProperty(value = "教室是否可用")
    private Boolean isAvailable;

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
