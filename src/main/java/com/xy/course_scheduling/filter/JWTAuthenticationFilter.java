package com.xy.course_scheduling.filter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xy.course_scheduling.entity.Admin;
import com.xy.course_scheduling.entity.CustomUserDetails;
import com.xy.course_scheduling.utils.JwtUtil;
import com.xy.course_scheduling.service.AdminService;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

//JWT认证过滤器
@Component
public class JWTAuthenticationFilter extends OncePerRequestFilter {
    @Resource
    private AdminService adminService;
    @Resource
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        //从request中获取token
        String token = request.getHeader("Authorization");
        if (token != null){
            //从token中获取用户名
            String username = jwtUtil.getUsername(token);
            //从数据库中查找用户信息
            Admin admin = adminService.getOne(new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, username));
            //将用户信息user保存到MyUserDetails中
            CustomUserDetails customUserDetails = new CustomUserDetails(admin);
            //判断token是否有效
            if (jwtUtil.validateToken(token, customUserDetails)){
                //将用户信息保存到SecurityContextHolder中
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                        new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
                usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                if (jwtUtil.checkRenewal(token)){
                    String newToken = jwtUtil.createToken(username, "ADMIN", admin.getName());
                    response.setHeader("X-Refresh-Token", newToken);
                    //暴露自定义响应头给前端
                    response.setHeader("Access-Control-Expose-Headers", "X-Refresh-Token");
                }
            }
        }
        //放行
        filterChain.doFilter(request, response);
    }
}
