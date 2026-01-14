package com.ruoyi.framework.web.service;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.GuestLoginRequest;

/**
 * 游客登录Service接口
 *
 * @author ruoyi
 */
public interface IGuestLoginService
{
    /**
     * 游客登录
     *
     * @param request 游客登录请求
     * @return 登录结果
     */
    AjaxResult guestLogin(GuestLoginRequest request);
}
