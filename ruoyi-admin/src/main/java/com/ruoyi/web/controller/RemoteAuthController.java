package com.ruoyi.web.controller;

import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.framework.web.service.PermissionService;
import com.ruoyi.framework.web.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 远程鉴权控制器
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/remote/auth")
public class RemoteAuthController {
    
    @Autowired
    private TokenService tokenService;
    
    @Autowired
    private PermissionService permissionService;
    
    /**
     * 远程权限验证接口 - 支持与@PreAuthorize相同的表达式
     */
    @Anonymous
    @PostMapping("/validate")
    public AjaxResult validatePermission(@RequestBody AuthValidateRequest request) {
        try {
            // 1. 验证token有效性
            LoginUser loginUser = tokenService.getLoginUserByToken(request.getToken());
            if (loginUser == null) {
                return AjaxResult.error("Token无效或已过期");
            }
            
            // 2. 解析并执行权限表达式
            boolean hasPermission = evaluatePermissionExpression(loginUser, request.getExpression());
            
            if (hasPermission) {
                return AjaxResult.success("权限验证通过");
            } else {
                return AjaxResult.error("权限不足");
            }
            
        } catch (Exception e) {
            return AjaxResult.error("权限验证异常: " + e.getMessage());
        }
    }
    
    /**
     * 解析权限表达式 - 支持与@PreAuthorize相同的语法
     */
    private boolean evaluatePermissionExpression(LoginUser loginUser, String expression) {
        // 设置当前用户上下文
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities())
        );
        
        try {
            // 解析表达式，支持以下格式：
            // @ss.hasPermi('system:user:list')
            // @ss.hasRole('admin')
            // @ss.hasAnyPermi('system:user:add,system:user:edit')
            // @ss.hasAnyRoles('admin,manager')
            
            if (expression.contains("@ss.hasPermi")) {
                String permission = extractValue(expression, "hasPermi");
                return permissionService.hasPermi(permission);
            } else if (expression.contains("@ss.hasRole")) {
                String role = extractValue(expression, "hasRole");
                return permissionService.hasRole(role);
            } else if (expression.contains("@ss.hasAnyPermi")) {
                String permissions = extractValue(expression, "hasAnyPermi");
                return permissionService.hasAnyPermi(permissions);
            } else if (expression.contains("@ss.hasAnyRoles")) {
                String roles = extractValue(expression, "hasAnyRoles");
                return permissionService.hasAnyRoles(roles);
            }
            
            return false;
        } finally {
            // 清理上下文
            SecurityContextHolder.clearContext();
        }
    }
    
    /**
     * 从表达式中提取值
     */
    private String extractValue(String expression, String method) {
        String pattern = method + "\\('([^']*)'\\)";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(expression);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }
}

/**
 * 权限验证请求实体
 */
class AuthValidateRequest {
    private String token;
    private String expression;
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getExpression() {
        return expression;
    }
    
    public void setExpression(String expression) {
        this.expression = expression;
    }
}