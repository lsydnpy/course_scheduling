package com.xy.course_scheduling.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@ApiModel(value = "CustomUserDetails", description = "用户详细信息")
public class CustomUserDetails implements UserDetails {
    @ApiModelProperty(value = "用户名")
    private String username;
    
    @ApiModelProperty(value = "密码")
    private String password;
    
    @ApiModelProperty(value = "姓名")
    private String name;
    
    @ApiModelProperty(value = "头像 URL")
    private String avatar;
    
    @ApiModelProperty(value = "权限列表")
    private Collection<? extends GrantedAuthority> authorities;
    
    @ApiModelProperty(value = "是否启用")
    private boolean enabled;

    public CustomUserDetails(Admin admin) {
        this.username = admin.getUsername() != null ? admin.getUsername() : "";
        this.password = admin.getPassword() != null ? admin.getPassword() : "";
        this.name = admin.getName() != null ? admin.getName() : "";
        this.authorities = List.of(new SimpleGrantedAuthority("ADMIN"));
        this.enabled = admin.getDeleted() == null || admin.getDeleted() == 0;
        this.avatar = admin.getAvatar() != null ? admin.getAvatar() : "";
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(Collection<? extends GrantedAuthority> authorities) {
        this.authorities = authorities;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
