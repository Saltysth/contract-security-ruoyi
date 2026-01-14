package com.ruoyi.feign.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 远程鉴权配置属性自动配置
 *
 * @author Saltyfish
 */
@Configuration
@EnableConfigurationProperties(RemoteAuthProperties.class)
public class RemoteAuthPropertiesConfiguration {
}
