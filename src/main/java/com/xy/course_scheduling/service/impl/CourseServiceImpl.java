package com.xy.course_scheduling.service.impl;

import com.xy.course_scheduling.entity.Course;
import com.xy.course_scheduling.mapper.CourseMapper;
import com.xy.course_scheduling.service.CourseService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 课程信息表 服务实现类
 * </p>
 *
 * @author xy
 * @since 2026-01-09
 */
@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

}
