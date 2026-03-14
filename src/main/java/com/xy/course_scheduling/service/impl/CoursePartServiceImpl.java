package com.xy.course_scheduling.service.impl;

import com.xy.course_scheduling.entity.CoursePart;
import com.xy.course_scheduling.mapper.CoursePartMapper;
import com.xy.course_scheduling.service.CoursePartService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 课程分块表（理论/实践部分） 服务实现类
 * </p>
 *
 * @author xy
 * @since 2026-01-09
 */
@Service
public class CoursePartServiceImpl extends ServiceImpl<CoursePartMapper, CoursePart> implements CoursePartService {

}
