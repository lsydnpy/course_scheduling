package com.xy.course_scheduling.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 排课结果状态枚举
 */
@Schema(description = "排课结果状态")
public enum SchedulingStatus {
    @Schema(description = "成功")
    SUCCESS("成功"),
    
    @Schema(description = "部分成功")
    PARTIAL_SUCCESS("部分成功"),
    
    @Schema(description = "失败")
    FAILED("失败");

    private final String description;

    SchedulingStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
