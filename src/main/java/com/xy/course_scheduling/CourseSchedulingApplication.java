package com.xy.course_scheduling;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.xy.course_scheduling.mapper")
public class CourseSchedulingApplication {

    public static void main(String[] args) {
        SpringApplication.run(CourseSchedulingApplication.class, args);
    }

}
