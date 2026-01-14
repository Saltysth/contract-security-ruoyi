package com.ruoyi.feign.config;

import com.ruoyi.common.utils.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 远程鉴权配置属性
 *
 * @author Saltyfish
 */
@ConfigurationProperties(prefix = "ruoyi.remote-auth")
public class RemoteAuthProperties {

    /**
     * 内部鉴权密钥
     * 不配置或为空则不开启内部鉴权
     */
    private String secret;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    /**
     * 判断是否开启内部鉴权
     *
     * @return true表示开启，false表示不开启
     */
    public boolean isEnabled() {
        return StringUtils.isNotEmpty(secret);
    }
}
