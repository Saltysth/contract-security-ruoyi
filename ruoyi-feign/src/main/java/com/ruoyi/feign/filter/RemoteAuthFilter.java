package com.ruoyi.feign.filter;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.feign.annotation.RemotePreAuthorize;
import com.ruoyi.feign.config.RemoteAuthProperties;
import com.ruoyi.feign.dto.AuthValidateRequest;
import com.ruoyi.feign.service.RemoteAuthFeignService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 远程鉴权过滤器，在Filter层执行权限验证
 *
 * @author Saltyfish
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RemoteAuthFilter extends OncePerRequestFilter {

    private static final String INTERNAL_AUTH_HEADER = "X-Internal-Auth-Secret";

    private final RemoteAuthFeignService remoteAuthFeignService;
    private final RequestMappingHandlerMapping handlerMapping;
    private final RemoteAuthProperties remoteAuthProperties;

    public RemoteAuthFilter(RemoteAuthFeignService remoteAuthFeignService,
        RequestMappingHandlerMapping handlerMapping,
        RemoteAuthProperties remoteAuthProperties) {
        this.remoteAuthFeignService = remoteAuthFeignService;
        this.handlerMapping = handlerMapping;
        this.remoteAuthProperties = remoteAuthProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {

        // 1. 获取请求映射的方法
        HandlerMethod handlerMethod = getHandlerMethod(request);
        if (handlerMethod == null) {
            chain.doFilter(request, response);
            return;
        }

        // 2. 检查方法或类上是否有@RemotePreAuthorize注解
        RemotePreAuthorize annotation = getRemotePreAuthorizeAnnotation(handlerMethod);
        if (annotation == null) {
            // 没有@RemotePreAuthorize，检查是否有@Anonymous
            if (hasAnonymousAnnotation(handlerMethod)) {
                chain.doFilter(request, response);
                return;
            }
            // 既没有@RemotePreAuthorize也没有@Anonymous，拒绝访问
            handleAuthFailure(response, "接口未配置鉴权注解");
            return;
        }

        // 2.5. 内部密钥校验（如果开启）
        if (remoteAuthProperties.isEnabled() && StringUtils.isNotBlank(request.getHeader(INTERNAL_AUTH_HEADER))) {
            if (validateInternalSecret(request)) {
                // 内部密钥校验通过，直接放行
                chain.doFilter(request, response);
                return;
            } else {
                // 内部密钥校验失败，拒绝访问
                handleAuthFailure(response, "内部鉴权失败");
                return;
            }
        }

        // 3. 提取token
        String token = extractToken(request);
        if (StringUtils.isEmpty(token)) {
            handleAuthFailure(response, "未提供认证token");
            return;
        }

        // 4. 执行远程权限验证
        boolean hasPermission = validateRemotePermission(token, annotation.value());
        if (!hasPermission) {
            handleAuthFailure(response, "权限不足");
            return;
        }

        // 5. 权限验证通过，继续执行
        chain.doFilter(request, response);
    }

    /**
     * 获取HandlerMethod
     */
    private HandlerMethod getHandlerMethod(HttpServletRequest request) {
        try {
            HandlerExecutionChain handlerExecutionChain = handlerMapping.getHandler(request);
            if (handlerExecutionChain == null) {
                return null;
            }
            Object handler = handlerExecutionChain.getHandler();
            if (handler instanceof HandlerMethod) {
                return (HandlerMethod) handler;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取@RemotePreAuthorize注解
     */
    private RemotePreAuthorize getRemotePreAuthorizeAnnotation(HandlerMethod handlerMethod) {
        // 先检查方法上的注解
        RemotePreAuthorize methodAnnotation = handlerMethod.getMethodAnnotation(RemotePreAuthorize.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }

        // 再检查类上的注解
        return handlerMethod.getBeanType().getAnnotation(RemotePreAuthorize.class);
    }

    /**
     * 检查是否有@Anonymous注解
     */
    private boolean hasAnonymousAnnotation(HandlerMethod handlerMethod) {
        // 先检查方法上的注解
        Anonymous methodAnnotation = handlerMethod.getMethodAnnotation(Anonymous.class);
        if (methodAnnotation != null) {
            return true;
        }

        // 再检查类上的注解
        return handlerMethod.getBeanType().getAnnotation(Anonymous.class) != null;
    }

    /**
     * 从请求中提取token
     */
    private String extractToken(HttpServletRequest request) {
        // 从Header中获取
        String token = request.getHeader("Authorization");
        if (StringUtils.isNotEmpty(token) && token.startsWith("Bearer ")) {
            return token.substring(7);
        }

        // 从参数中获取
        token = request.getParameter("token");
        if (StringUtils.isNotEmpty(token)) {
            return token;
        }

        return null;
    }

    /**
     * 执行远程权限验证
     */
    private boolean validateRemotePermission(String token, String expression) {
        try {
            AuthValidateRequest request = new AuthValidateRequest();
            request.setToken(token);
            request.setExpression(expression);

            AjaxResult result = remoteAuthFeignService.validatePermission(request);
            return result.isSuccess();
        } catch (Exception e) {
            // 记录日志
            System.err.println("远程权限验证失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 处理权限验证失败
     */
    private void handleAuthFailure(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> result = new HashMap<>();
        result.put("code", 403);
        result.put("msg", message);
        result.put("data", null);

        response.getWriter().write(JSON.toJSONString(result));
    }

    /**
     * 校验内部密钥
     *
     * @param request HTTP请求
     * @return true表示校验通过，false表示校验失败
     */
    private boolean validateInternalSecret(HttpServletRequest request) {
        String providedSecret = request.getHeader(INTERNAL_AUTH_HEADER);
        return remoteAuthProperties.getSecret().equals(providedSecret);
    }
}