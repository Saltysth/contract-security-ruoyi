package com.ruoyi.web.controller.system;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginBody;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.ip.AddressUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.common.utils.jwt.JwtUtils;
import com.ruoyi.framework.web.service.SysLoginService;
import com.ruoyi.framework.web.service.SysPermissionService;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.system.service.ISysRefreshTokenService;
import com.ruoyi.system.service.ISysUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Contract前端认证控制器
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/contract/auth")
public class ContractAuthController
{
    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private SysPermissionService permissionService;

    @Autowired
    private ISysRefreshTokenService refreshTokenService;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private RedisCache redisCache;


    @Autowired
    private SysLoginService loginService;

    /**
     * Contract前端专用登录接口
     *
     * @param loginBody 登录信息
     * @return 结果
     */
    @PostMapping("/login")
    public AjaxResult contractLogin(@RequestBody LoginBody loginBody)
    {
        String username = loginBody.getUsername();

        // 这里调用登录服务验证用户名密码
        String accessToken = loginService.login(username, loginBody.getPassword(), loginBody.getCode(),
            loginBody.getUuid());

        SysUser user = userService.selectUserByUserName(username);
        if (user == null || !"0".equals(user.getStatus()))
        {
            return AjaxResult.error("用户不存在或已被禁用");
        }

        // 获取用户权限
        LoginUser loginUser = new LoginUser(user, permissionService.getMenuPermission(user));
        Set<String> permissions = permissionService.getMenuPermission(user);
        Set<String> roles = permissionService.getRolePermission(user);

        // 生成Refresh Token
        String refreshTokenStr = jwtUtils.generateRefreshToken(user.getUserId(), username);
        Date expireTime = new Date(System.currentTimeMillis() + jwtUtils.getRefreshTokenExpire() * 60 * 1000L);

        // 获取设备信息和IP地址
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = servletRequestAttributes.getRequest();
        String userAgent = request.getHeader("User-Agent");
        String ip = IpUtils.getIpAddr();
        String ipAddress = AddressUtils.getRealAddressByIP(ip);
        String deviceInfo = userAgent != null ? userAgent : "Unknown";

        // 保存刷新令牌到数据库
        refreshTokenService.createRefreshToken(user.getUserId(), username, refreshTokenStr, expireTime, deviceInfo, ipAddress);

        // 构建返回结果
        AjaxResult ajax = AjaxResult.success();

        // 构建用户信息
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getUserId().toString());
        userInfo.put("username", user.getUserName());
        userInfo.put("email", user.getEmail());
        userInfo.put("phone", user.getPhonenumber());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("nickname", user.getNickName());
        userInfo.put("roles", roles.toArray());
        userInfo.put("permissions", permissions);
        userInfo.put("createdAt", user.getCreateTime() != null ? user.getCreateTime().toString() : "");

        ajax.put("user", userInfo);
        ajax.put("accessToken", accessToken);
        ajax.put("refreshToken", refreshTokenStr);
        ajax.put("expiresIn", tokenService.getExpireTime() * 60); // 转换为秒

        return ajax;
    }
}