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
 * 学院信息表
 * </p>
 *
 * @author xy
 * @since 2026-01-09
 */
@Getter
@Setter
@TableName("tb_college")
@ApiModel(value = "College", description = "学院信息表")
public class College implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "college_id", type = IdType.AUTO)
    @ApiModelProperty(value = "学院 ID")
    private Integer collegeId;

    @TableField("college_name")
    @ApiModelProperty(value = "学院名称")
    private String collegeName;

    @TableField("campus_id")
    @ApiModelProperty(value = "校区 ID")
    private Integer campusId;
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
