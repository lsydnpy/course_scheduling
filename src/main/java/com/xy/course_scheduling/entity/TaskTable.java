package com.xy.course_scheduling.entity;

import lombok.Data;

import java.util.List;

@Data
public class TaskTable {
    private Integer taskId;
    private String courseName;
    private String classRoomCode;
    private String studentGroup;
    private List<Integer> weeks;
}
