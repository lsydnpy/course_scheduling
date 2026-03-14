package com.xy.course_scheduling.service.impl;

import com.xy.course_scheduling.entity.Admin;
import com.xy.course_scheduling.mapper.AdminMapper;
import com.xy.course_scheduling.service.AdminService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 管理员信息表 服务实现类
 * </p>
 *
 * @author xy
 * @since 2026-01-09
 */
@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

}
