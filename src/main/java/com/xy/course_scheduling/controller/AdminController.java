package com.xy.course_scheduling.controller;

import com.xy.course_scheduling.custom.annotations.OperLogAnn;
import com.xy.course_scheduling.entity.CustomUserDetails;
import com.xy.course_scheduling.utils.JwtUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xy.course_scheduling.entity.Result;
import com.xy.course_scheduling.entity.Admin;
import com.xy.course_scheduling.service.AdminService;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 管理员信息表 前端控制器
 * </p>
 *
 * @author xy
 * @since 2026-01-09
 */
@RestController
@RequestMapping("/admin")
@Tag(name = "管理员信息表")
@Slf4j
public class AdminController {

    @Resource
    private AdminService adminService;
    @Resource
    private AuthenticationManager authenticationManager;

    @Resource
    private JwtUtil jwtUtil;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping
    @OperLogAnn(title = "添加管理员信息", businessType = OperLogAnn.BusinessType.INSERT)
    @Operation(summary = "添加管理员信息")
    public Result<Admin> save(Admin admin) {
        // 加密密码
        if (admin.getPassword() != null && !admin.getPassword().isEmpty()) {
            admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        }
        boolean save = adminService.save(admin);
        if (save) {
            return Result.ok(admin);
        }
        return Result.fail("添加失败");
    }

    @DeleteMapping("/{id}")
    @OperLogAnn(title = "删除管理员信息", businessType = OperLogAnn.BusinessType.DELETE)
    @Operation(summary = "删除管理员信息")
    public Result<String> delete(@PathVariable Long id) {
        boolean remove = adminService.removeById(id);
        if (remove) {
            return Result.ok("删除成功");
        }
        return Result.fail("删除失败");
    }

    @PutMapping
    @OperLogAnn(title = "修改管理员信息", businessType = OperLogAnn.BusinessType.UPDATE)
    @Operation(summary = "修改管理员信息")
    public Result<String> update(Admin admin) {
        boolean update = adminService.updateById(admin);
        if (update) {
            return Result.ok("修改成功");
        }
        return Result.fail("修改失败");
    }

    @GetMapping("/{id}")
    @OperLogAnn(title = "查询管理员信息", businessType = OperLogAnn.BusinessType.SELECT)
    @Operation(summary = "查询管理员信息")
    public Result<Admin> getById(@PathVariable Long id) {
        return Result.ok(adminService.getById(id));
    }

    @GetMapping("list")
    @OperLogAnn(title = "查询管理员信息列表", businessType = OperLogAnn.BusinessType.SELECT)
    @Operation(summary = "查询管理员信息列表")
    public Result<List<Admin>> list(Admin admin, Page<Admin> page) {
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ObjectUtils.isNotEmpty(admin.getAdminId()), Admin::getAdminId, admin.getAdminId());
        wrapper.like(ObjectUtils.isNotEmpty(admin.getUsername()), Admin::getUsername, admin.getUsername());
        wrapper.like(ObjectUtils.isNotEmpty(admin.getPassword()), Admin::getPassword, admin.getPassword());
        IPage<Admin> pageData = adminService.page(page, wrapper);
        return Result.ok(pageData);
    }

    @GetMapping("login")
    @OperLogAnn(title = "管理员登录", businessType = OperLogAnn.BusinessType.OTHER)
    @Operation(summary = "管理员登录")
    public Result<Map<String, String>> login(Admin admin) {
        try {
            // 检查用户名和密码是否为空
            if (admin.getUsername() == null || admin.getUsername().trim().isEmpty()) {
                return Result.fail("用户名不能为空");
            }
            if (admin.getPassword() == null || admin.getPassword().trim().isEmpty()) {
                return Result.fail("密码不能为空");
            }

            // 认证用户
            Authentication authenticate =
                    authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(admin.getUsername(), admin.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authenticate);

            // 生成 JWT
            CustomUserDetails userDetails = (CustomUserDetails) authenticate.getPrincipal();
            String role = "admin";
            if (userDetails.getAuthorities() != null && !userDetails.getAuthorities().isEmpty()) {
                role = userDetails.getAuthorities().iterator().next().getAuthority();
            }
            String jwt = jwtUtil.createToken(userDetails.getUsername(), role, userDetails.getName());
            Map<String, String> map = Map.of("role", role, "token", jwt,
                    "name", userDetails.getName(), "avatar", userDetails.getAvatar());
            return Result.ok(map);
        } catch (AuthenticationException e) {
            e.printStackTrace();
            return Result.fail("用户名或密码错误");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("登录失败：" + e.getMessage());
        }
    }
}
