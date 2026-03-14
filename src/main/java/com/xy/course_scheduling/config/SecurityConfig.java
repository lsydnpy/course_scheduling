package com.xy.course_scheduling.config;

import com.xy.course_scheduling.filter.JWTAuthenticationFilter;
import com.xy.course_scheduling.security.MyAccessDeniedHandler;
import com.xy.course_scheduling.security.MyAuthenticationEntryPoint;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)//开启权限控制
public class SecurityConfig {
    @Resource
    private JWTAuthenticationFilter jwtAuthenticationFilter;//认证过滤器
    @Resource
    private MyAuthenticationEntryPoint myAuthenticationEntryPoint;//未认证处理类
    @Resource
    private MyAccessDeniedHandler myAccessDeniedHandler;//权限不足处理类


    @Bean
    // 密码编码器
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    // 认证管理器
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    @Bean
    // 配置跨域
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedOriginPattern("*");//允许所有源跨域
        corsConfiguration.addAllowedMethod("*");//允许所有请求方式跨域
        corsConfiguration.addAllowedHeader("*");//允许所有header跨域
        corsConfiguration.setAllowCredentials(true);//允许携带cookie
        UrlBasedCorsConfigurationSource corsConfig = new UrlBasedCorsConfigurationSource();
        corsConfig.registerCorsConfiguration("/**", corsConfiguration);//所有路径映射该配置
        return corsConfig;
    }

    @Bean
    // 配置过滤器链
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))//配置跨域
                .csrf(csrf -> csrf.disable()) // 禁用csrf
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))//无状态会话
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/login").permitAll()//登录接口放行
                        .requestMatchers("/admin/register").permitAll()//注册接口放行
                        .anyRequest().authenticated()//其他接口需要认证
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(myAuthenticationEntryPoint)//未认证处理类
                        .accessDeniedHandler(myAccessDeniedHandler)//权限不足处理类
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)//添加认证过滤器
                .build();
    }

}
