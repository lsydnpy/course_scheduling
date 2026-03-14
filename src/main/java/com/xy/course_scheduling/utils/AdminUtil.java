package com.xy.course_scheduling.utils;

import com.xy.course_scheduling.entity.CustomUserDetails;
import org.springframework.security.core.context.SecurityContextHolder;

public class AdminUtil {
    public static CustomUserDetails getCurrentAdmin() {
        CustomUserDetails details = null;
        try {
            details = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        } catch (Exception e) {
            System.out.println("获取当前登录用户失败");
        }
        return details;
    }
}
