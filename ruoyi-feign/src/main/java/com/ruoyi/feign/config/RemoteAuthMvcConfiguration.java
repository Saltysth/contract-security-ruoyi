package com.ruoyi.feign.config;

import com.ruoyi.feign.filter.RemoteAuthFilter;
import com.ruoyi.feign.service.RemoteAuthFeignService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * MVC环境下的远程鉴权配置类
 *
 * @author Saltyfish
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class RemoteAuthMvcConfiguration {

    /**
     * MVC环境下的Filter配置
     */
    @Bean
    public RemoteAuthFilter remoteAuthFilter(@Lazy RemoteAuthFeignService remoteAuthFeignService,
        @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
        RemoteAuthProperties remoteAuthProperties) {
        return new RemoteAuthFilter(remoteAuthFeignService, handlerMapping, remoteAuthProperties);
    }
}
