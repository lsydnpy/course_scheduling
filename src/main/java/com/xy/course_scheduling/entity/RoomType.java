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
 * 教室类型表
 * </p>
 *
 * @author xy
 * @since 2026-01-09
 */
@Getter
@Setter
@TableName("tb_room_type")
@ApiModel(value = "RoomType", description = "教室类型表")
public class RoomType implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "type_id", type = IdType.AUTO)
    @ApiModelProperty(value = "教室类型 ID")
    private Integer typeId;

    @TableField("type_name")
    @ApiModelProperty(value = "教室类型名称")
    private String typeName;

    @TableField("description")
    @ApiModelProperty(value = "教室类型描述")
    private String description;
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
