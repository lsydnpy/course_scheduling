package com.xy.course_scheduling.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 排课结果状态枚举
 */
@ApiModel(value = "SchedulingStatus", description = "排课结果状态")
public enum SchedulingStatus {
    @ApiModelProperty(value = "成功")
    SUCCESS("成功"),
    
    @ApiModelProperty(value = "部分成功")
    PARTIAL_SUCCESS("部分成功"),
    
    @ApiModelProperty(value = "失败")
    FAILED("失败");

    private final String description;

    SchedulingStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
