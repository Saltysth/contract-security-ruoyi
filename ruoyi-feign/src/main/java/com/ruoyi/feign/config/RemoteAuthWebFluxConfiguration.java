package com.ruoyi.feign.config;

import com.ruoyi.feign.filter.RemoteAuthWebFilter;
import com.ruoyi.feign.service.RemoteAuthFeignService;
import com.ruoyi.feign.util.WebFluxRouteUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.List;

/**
 * WebFlux环境下的远程鉴权配置类
 *
 * @author Saltyfish
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class RemoteAuthWebFluxConfiguration {

    private final ApplicationContext applicationContext;

    public RemoteAuthWebFluxConfiguration(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * WebFlux环境下的Filter配置
     */
    @Bean
    public RemoteAuthWebFilter remoteAuthWebFilter(@Lazy RemoteAuthFeignService remoteAuthFeignService,
        RemoteAuthProperties remoteAuthProperties) {
        return new RemoteAuthWebFilter(remoteAuthFeignService, remoteAuthProperties);
    }

    /**
     * 初始化WebFluxRouteUtil工具类
     */
    @PostConstruct
    public void initializeWebFluxRouteUtil() {
        WebFluxRouteUtil.setApplicationContext(applicationContext);
    }

    /**
     * 提供HttpMessageConverters Bean，解决WebFlux环境下Feign客户端的问题
     */
    @Bean
    public HttpMessageConverters httpMessageConverters() {
        List<HttpMessageConverter<?>> converters = List.of(new MappingJackson2HttpMessageConverter());
        return new HttpMessageConverters(converters);
    }
}
