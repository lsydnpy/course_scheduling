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
 * 系统操作日志记录表
 * </p>
 *
 * @author 张三
 * @since 2026-01-09
 */
@Getter
@Setter
@TableName("tb_oper_log")
@Schema(description = "系统操作日志记录表")
public class OperLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日志主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "日志主键")
    private Integer id;

    /**
     * 模块标题/业务模块
     */
    @TableField("title")
    @Schema(description = "模块标题/业务模块")
    private String title;

    /**
     * 业务类型（INSERT,UPDATE,DELETE,OTHER）
     */
    @TableField("business_type")
    @Schema(description = "业务类型")
    private String businessType;

    /**
     * 请求方法名（包名。类名。方法名）
     */
    @TableField("method")
    @Schema(description = "请求方法名")
    private String method;

    /**
     * HTTP 请求方式（GET, POST 等）
     */
    @TableField("request_method")
    @Schema(description = "HTTP 请求方式")
    private String requestMethod;

    /**
     * 操作人员用户名
     */
    @TableField("operator_username")
    @Schema(description = "操作人员用户名")
    private String operatorUsername;

    /**
     * 操作人员名称
     */
    @TableField("operator_name")
    @Schema(description = "操作人员名称")
    private String operatorName;

    /**
     * 请求 URL
     */
    @TableField("oper_url")
    @Schema(description = "请求 URL")
    private String operUrl;

    /**
     * 操作主机 IP 地址
     */
    @TableField("oper_ip")
    @Schema(description = "操作主机 IP 地址")
    private String operIp;

    /**
     * 请求参数（JSON 格式，过长可截断或摘要）
     */
    @TableField("oper_param")
    @Schema(description = "请求参数")
    private String operParam;

    /**
     * 返回结果（JSON 格式，过长可截断）
     */
    @TableField("json_result")
    @Schema(description = "返回结果")
    private String jsonResult;

    /**
     * 操作状态（0 成功 1 异常）
     */
    @TableField("status")
    @Schema(description = "操作状态")
    private Integer status;

    /**
     * 错误消息（异常时记录）
     */
    @TableField("error_msg")
    @Schema(description = "错误消息")
    private String errorMsg;

    /**
     * 操作时间
     */
    @TableField("oper_time")
    @Schema(description = "操作时间")
    private LocalDateTime operTime;

    /**
     * 执行时长（毫秒）
     */
    @TableField("execute_time")
    @Schema(description = "执行时长（毫秒）")
    private Long executeTime;

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
