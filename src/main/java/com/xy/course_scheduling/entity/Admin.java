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
 * 管理员信息表
 * </p>
 *
 * @author xy
 * @since 2026-01-09
 */
@Getter
@Setter
@TableName("tb_admin")
@ApiModel(description = "管理员信息表")
public class Admin implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "admin_id", type = IdType.AUTO)
    @ApiModelProperty(value = "管理员ID")
    private Integer adminId;

    @TableField("name")
    @ApiModelProperty(value = "姓名")
    private String name;


    @TableField("username")
    @ApiModelProperty(value = "用户名")
    private String username;

    @TableField("password")
    @ApiModelProperty(value = "密码")
    private String password;

    @TableField("avatar")
    @ApiModelProperty(value = "头像")
    private String avatar;

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
