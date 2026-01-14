package com.ruoyi.feign.filter;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.feign.annotation.RemotePreAuthorize;
import com.ruoyi.feign.config.RemoteAuthProperties;
import com.ruoyi.feign.dto.AuthValidateRequest;
import com.ruoyi.feign.service.RemoteAuthFeignService;
import com.ruoyi.feign.util.WebFluxRouteUtil;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 远程鉴权WebFlux过滤器，在Filter层执行权限验证
 *
 * @author Saltyfish
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RemoteAuthWebFilter implements WebFilter {

    private static final String INTERNAL_AUTH_HEADER = "X-Internal-Auth-Secret";

    private final RemoteAuthFeignService remoteAuthFeignService;
    private final RemoteAuthProperties remoteAuthProperties;

    public RemoteAuthWebFilter(RemoteAuthFeignService remoteAuthFeignService,
        RemoteAuthProperties remoteAuthProperties) {
        this.remoteAuthFeignService = remoteAuthFeignService;
        this.remoteAuthProperties = remoteAuthProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        // 1. 获取请求映射的方法
        Method handlerMethod = WebFluxRouteUtil.getHandlerMethod(exchange);
        if (handlerMethod == null) {
            return chain.filter(exchange);
        }

        // 2. 检查方法或类上是否有@RemotePreAuthorize注解
        RemotePreAuthorize annotation = getRemotePreAuthorizeAnnotation(handlerMethod);
        if (annotation == null) {
            // 没有@RemotePreAuthorize，检查是否有@Anonymous
            if (hasAnonymousAnnotation(handlerMethod)) {
                return chain.filter(exchange);
            }
            // 既没有@RemotePreAuthorize也没有@Anonymous，拒绝访问
            return handleAuthFailure(response, "接口未配置鉴权注解");
        }

        // 2.5. 内部密钥校验（如果开启）
        if (remoteAuthProperties.isEnabled()) {
            if (validateInternalSecret(request)) {
                // 内部密钥校验通过，直接放行
                return chain.filter(exchange);
            } else {
                // 内部密钥校验失败，拒绝访问
                return handleAuthFailure(response, "内部鉴权失败");
            }
        }

        // 3. 提取token
        String token = extractToken(request);
        if (StringUtils.isEmpty(token)) {
            return handleAuthFailure(response, "未提供认证token");
        }

        // 4. 执行远程权限验证
        return validateRemotePermission(token, annotation.value())
            .flatMap(hasPermission -> {
                if (hasPermission) {
                    return chain.filter(exchange);
                } else {
                    return handleAuthFailure(response, "权限不足");
                }
            })
            .onErrorResume(throwable -> {
                System.err.println("远程权限验证异常: " + throwable.getMessage());
                return handleAuthFailure(response, "权限验证异常");
            });
    }

    /**
     * 获取@RemotePreAuthorize注解
     */
    private RemotePreAuthorize getRemotePreAuthorizeAnnotation(Method handlerMethod) {
        // 先检查方法上的注解
        RemotePreAuthorize methodAnnotation = handlerMethod.getAnnotation(RemotePreAuthorize.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }

        // 再检查类上的注解
        return handlerMethod.getDeclaringClass().getAnnotation(RemotePreAuthorize.class);
    }

    /**
     * 检查是否有@Anonymous注解
     */
    private boolean hasAnonymousAnnotation(Method handlerMethod) {
        // 先检查方法上的注解
        Anonymous methodAnnotation = handlerMethod.getAnnotation(Anonymous.class);
        if (methodAnnotation != null) {
            return true;
        }

        // 再检查类上的注解
        return handlerMethod.getDeclaringClass().getAnnotation(Anonymous.class) != null;
    }

    /**
     * 从请求中提取token
     */
    private String extractToken(ServerHttpRequest request) {
        // 从Header中获取
        String token = request.getHeaders().getFirst("Authorization");
        if (StringUtils.isNotEmpty(token) && token.startsWith("Bearer ")) {
            return token.substring(7);
        }

        // 从参数中获取
        token = request.getQueryParams().getFirst("token");
        if (StringUtils.isNotEmpty(token)) {
            return token;
        }

        return null;
    }

    /**
     * 执行远程权限验证
     * 使用 subscribeOn(Schedulers.boundedElastic()) 将阻塞调用卸载到独立线程池
     */
    private Mono<Boolean> validateRemotePermission(String token, String expression) {
        return Mono.fromCallable(() -> {
            try {
                AuthValidateRequest request = new AuthValidateRequest();
                request.setToken(token);
                request.setExpression(expression);

                AjaxResult result = remoteAuthFeignService.validatePermission(request);
                return result.isSuccess();
            } catch (Exception e) {
                System.err.println("远程权限验证失败: " + e.getMessage());
                return false;
            }
        }).subscribeOn(Schedulers.boundedElastic());  // 关键修复：卸载到弹性线程池
    }

    /**
     * 处理权限验证失败
     */
    private Mono<Void> handleAuthFailure(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");

        Map<String, Object> result = new HashMap<>();
        result.put("code", 403);
        result.put("msg", message);
        result.put("data", null);

        String responseBody = JSON.toJSONString(result);
        DataBuffer buffer = response.bufferFactory().wrap(responseBody.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * 校验内部密钥
     *
     * @param request HTTP请求
     * @return true表示校验通过，false表示校验失败
     */
    private boolean validateInternalSecret(ServerHttpRequest request) {
        String providedSecret = request.getHeaders().getFirst(INTERNAL_AUTH_HEADER);
        return remoteAuthProperties.getSecret().equals(providedSecret);
    }
}