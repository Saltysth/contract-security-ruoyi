package com.ruoyi.common.core.domain.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 游客登录对象
 *
 * @author ruoyi
 */
public class GuestLoginRequest
{
    /**
     * 游客UUID（不超过20位）
     */
    @NotBlank(message = "游客UUID不能为空")
    @Size(min = 1, max = 20, message = "游客UUID长度不能超过20位")
    private String guestUuid;

    public String getGuestUuid()
    {
        return guestUuid;
    }

    public void setGuestUuid(String guestUuid)
    {
        this.guestUuid = guestUuid;
    }
}
