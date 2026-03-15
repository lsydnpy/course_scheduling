package com.xy.course_scheduling.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "课表任务项")
public class TaskTable {
    @Schema(description = "任务 ID")
    private Integer taskId;
    
    @Schema(description = "课程名称")
    private String courseName;
    
    @Schema(description = "教室代码")
    private String classRoomCode;
    
    @Schema(description = "学生群体/班级")
    private String studentGroup;
    
    @Schema(description = "周次列表")
    private List<Integer> weeks;
}
