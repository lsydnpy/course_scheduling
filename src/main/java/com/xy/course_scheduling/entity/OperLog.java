package com.xy.course_scheduling.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;

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
public class OperLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日志主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 模块标题/业务模块
     */
    @TableField("title")
    private String title;

    /**
     * 业务类型（INSERT,UPDATE,DELETE,OTHER）
     */
    @TableField("business_type")
    private String businessType;

    /**
     * 请求方法名（包名.类名.方法名）
     */
    @TableField("method")
    private String method;

    /**
     * HTTP请求方式（GET, POST等）
     */
    @TableField("request_method")
    private String requestMethod;

    /**
     * 操作人员用户名
     */
    @TableField("operator_username")
    private String operatorUsername;

    /**
     * 操作人员名称
     */
    @TableField("operator_name")
    private String operatorName;

    /**
     * 请求URL
     */
    @TableField("oper_url")
    private String operUrl;

    /**
     * 操作主机IP地址
     */
    @TableField("oper_ip")
    private String operIp;

    /**
     * 请求参数（JSON格式，过长可截断或摘要）
     */
    @TableField("oper_param")
    private String operParam;

    /**
     * 返回结果（JSON格式，过长可截断）
     */
    @TableField("json_result")
    private String jsonResult;

    /**
     * 操作状态（0成功 1异常）
     */
    @TableField("status")
    private Integer status;

    /**
     * 错误消息（异常时记录）
     */
    @TableField("error_msg")
    private String errorMsg;

    /**
     * 操作时间
     */
    @TableField("oper_time")
    private LocalDateTime operTime;

    /**
     * 执行时长（毫秒）
     */
    @TableField("execute_time")
    private Long executeTime;

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
