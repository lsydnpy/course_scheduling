package com.xy.course_scheduling.entity;

import lombok.Data;

/**
 * 排课结果状态枚举
 */
public enum SchedulingStatus {
    SUCCESS("成功"),
    PARTIAL_SUCCESS("部分成功"),
    FAILED("失败");

    private final String description;

    SchedulingStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
