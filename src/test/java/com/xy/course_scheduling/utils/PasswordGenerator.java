package com.xy.course_scheduling.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt 密码生成工具
 */
public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // 生成 123456 的 BCrypt 密码
        String rawPassword = "123456";
        String encodedPassword = encoder.encode(rawPassword);
        
        System.out.println("原始密码：" + rawPassword);
        System.out.println("BCrypt 加密后：" + encodedPassword);
        
        // 生成 SQL 插入语句
        System.out.println("\nSQL 插入语句:");
        System.out.println("INSERT INTO tb_admin (admin_id, username, password, name, avatar, deleted, created_time, updated_time)");
        System.out.println("VALUES (1, 'admin', '" + encodedPassword + "', '超级管理员', NULL, 0, NOW(), NOW());");
    }
}
