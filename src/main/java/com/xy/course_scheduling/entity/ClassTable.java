package com.xy.course_scheduling.entity;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ClassTable {
    //节次
    private String section;
    //星期一
    private List<TaskTable> mon;
    //星期二
    private List<TaskTable> tue;
    //星期三
    private List<TaskTable> wed;
    //星期四
    private List<TaskTable> thu;
    //星期五
    private List<TaskTable> fri;
    //星期六
    private List<TaskTable> sat;
    //星期日
    private List<TaskTable> sun;
}
