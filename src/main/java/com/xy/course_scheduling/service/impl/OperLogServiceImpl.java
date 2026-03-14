package com.xy.course_scheduling.service.impl;

import com.xy.course_scheduling.entity.OperLog;
import com.xy.course_scheduling.mapper.OperLogMapper;
import com.xy.course_scheduling.service.OperLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 系统操作日志记录表 服务实现类
 * </p>
 *
 * @author 张三
 * @since 2026-01-09
 */
@Service
public class OperLogServiceImpl extends ServiceImpl<OperLogMapper, OperLog> implements OperLogService {

}
