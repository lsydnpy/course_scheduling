package com.xy.course_scheduling.entity;

import com.baomidou.mybatisplus.core.metadata.IPage;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

@ApiModel(value = "Result", description = "统一响应结果")
public class Result<T> {
    @ApiModelProperty(value = "状态码")
    private Integer code;
    
    @ApiModelProperty(value = "响应消息")
    private String message;
    
    @ApiModelProperty(value = "响应数据")
    private T data;
    
    @ApiModelProperty(value = "总记录数")
    private Long total;
    
    @ApiModelProperty(value = "当前页码")
    private Long current;

    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }
    public static <T> Result<List<T>> ok(IPage<T> page) {
        Result<List<T>> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(page.getRecords());
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        return result;
    }
    public static <T> Result<T> fail(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }
    public static <T> Result<T> fail(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
    public static <T> Result<T> fail() {
        return fail(500, "操作失败");
    }
    public static <T> Result<T> ok(String message) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage(message);
        return result;
    }
    @Override
    public String toString() {
        return "Result{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", total=" + total +
                ", current=" + current +
                '}';
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Long getCurrent() {
        return current;
    }

    public void setCurrent(Long current) {
        this.current = current;
    }
}
