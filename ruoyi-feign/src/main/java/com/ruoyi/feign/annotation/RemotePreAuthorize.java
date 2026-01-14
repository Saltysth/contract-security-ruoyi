package com.ruoyi.feign.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 远程权限验证注解，用法与@PreAuthorize一致
 * 
 * @author Saltyfish
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RemotePreAuthorize {
    /**
     * 权限表达式，与@PreAuthorize语法相同
     * 例如："@ss.hasPermi('system:user:list')"
     */
    String value();
}