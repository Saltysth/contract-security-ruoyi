package com.ruoyi.feign.dto;

/**
 * 权限验证请求DTO
 * 
 * @author Saltyfish
 */
public class AuthValidateRequest {
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