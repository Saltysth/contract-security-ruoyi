package com.ruoyi.feign.config;

import feign.Logger;
import feign.Retryer;
import feign.codec.Decoder;
import feign.codec.Encoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/**
 * Feign配置类
 *
 * @author Saltyfish
 */
@Configuration(value = "RuoyiUserFeignConfiguration")
public class FeignConfiguration
{
    /**
     * Feign日志级别
     * NONE：不记录任何日志（默认值）
     * BASIC：仅记录请求方法、URL、响应状态代码以及执行时间
     * HEADERS：记录BASIC级别的基础上，记录请求和响应的header
     * FULL：记录请求和响应的header，body和元数据
     */
    @Bean
    public Logger.Level feignLoggerLevel()
    {
        return Logger.Level.BASIC;
    }

    /**
     * Feign重试机制
     * 默认情况下，Feign会重试5次，每次间隔100ms
     * 这里设置为不重试
     *
     * 条件：容器中没有其他 Retryer Bean（避免与下游模块冲突）
     */
    @Bean
    @ConditionalOnMissingBean(Retryer.class)
    public Retryer feignRetryer()
    {
        return new Retryer.Default(100, 1000, 0);
    }

    /**
     * WebFlux环境下的Feign编码器
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    public Encoder feignEncoder(ObjectFactory<HttpMessageConverters> messageConverters)
    {
        return new SpringEncoder(messageConverters);
    }

    /**
     * WebFlux环境下的Feign解码器
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    public Decoder feignDecoder(ObjectFactory<HttpMessageConverters> messageConverters)
    {
        return new ResponseEntityDecoder(new SpringDecoder(messageConverters));
    }
}