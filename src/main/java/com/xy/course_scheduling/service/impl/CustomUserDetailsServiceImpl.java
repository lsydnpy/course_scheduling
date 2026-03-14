package com.xy.course_scheduling.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xy.course_scheduling.entity.Admin;
import com.xy.course_scheduling.entity.CustomUserDetails;
import com.xy.course_scheduling.exception.MyException;
import com.xy.course_scheduling.mapper.AdminMapper;
import jakarta.annotation.Resource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsServiceImpl implements UserDetailsService {
    @Resource
    private AdminMapper adminMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, username));
        if (admin == null) {
            throw new UsernameNotFoundException("用户不存在：" + username);
        }
        CustomUserDetails details = new CustomUserDetails(admin);
        return details;
    }
}
