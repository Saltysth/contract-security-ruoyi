package com.ruoyi.feign.service;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.feign.config.AuthFeignConstants;
import com.ruoyi.feign.dto.AuthValidateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 远程鉴权Feign客户端
 * 
 * @author Saltyfish
 */
@FeignClient(name = AuthFeignConstants.FEIGN_NAME, contextId = "remoteAuthFeignService", path = AuthFeignConstants.FEIGN_PREFIX)
public interface RemoteAuthFeignService {
    
    /**
     * 远程权限验证
     */
    @PostMapping("/remote/auth/validate")
    AjaxResult validatePermission(@RequestBody AuthValidateRequest request);
}