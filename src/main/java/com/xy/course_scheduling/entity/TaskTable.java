package com.xy.course_scheduling.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "TaskTable", description = "课表任务项")
public class TaskTable {
    @ApiModelProperty(value = "任务 ID")
    private Integer taskId;
    
    @ApiModelProperty(value = "课程名称")
    private String courseName;
    
    @ApiModelProperty(value = "教室代码")
    private String classRoomCode;
    
    @ApiModelProperty(value = "学生群体/班级")
    private String studentGroup;
    
    @ApiModelProperty(value = "周次列表")
    private List<Integer> weeks;
}
