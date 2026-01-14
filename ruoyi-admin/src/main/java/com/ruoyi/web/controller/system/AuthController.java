package com.ruoyi.web.controller.system;

import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysMenu;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.MessageUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.ip.AddressUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.common.utils.jwt.JwtUtils;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.framework.manager.factory.AsyncFactory;
import com.ruoyi.framework.web.service.SysPermissionService;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysMenuService;
import com.ruoyi.system.service.ISysRefreshTokenService;
import com.ruoyi.system.service.ISysUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 认证控制器（JWT + Refresh Token）
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/auth")
public class AuthController
{
    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ISysMenuService menuService;

    @Autowired
    private SysPermissionService permissionService;

    @Autowired
    private ISysRefreshTokenService refreshTokenService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private TokenService tokenService;

    /**
     * 刷新访问令牌
     *
     * @param requestBody 请求体包含refreshToken
     * @return 结果
     */
    @Anonymous
    @PostMapping("/refresh")
    public AjaxResult refreshToken(@RequestBody Map<String, String> requestBody)
    {
        String refreshToken = requestBody != null ? requestBody.get("refreshToken") : null;

        // 验证刷新令牌格式
        if (StringUtils.isEmpty(refreshToken))
        {
            return AjaxResult.error("刷新令牌不能为空");
        }

        // 验证是否为有效的JWT Refresh Token
        if (!jwtUtils.isRefreshToken(refreshToken))
        {
            return AjaxResult.error("无效的刷新令牌");
        }

        // 验证刷新令牌是否过期
        if (jwtUtils.isTokenExpired(refreshToken))
        {
            return AjaxResult.error("刷新令牌已过期");
        }

        // 从刷新令牌中获取用户信息
        Long userId = jwtUtils.getUserIdFromToken(refreshToken);
        String username = jwtUtils.getUsernameFromToken(refreshToken);

        if (userId == null || StringUtils.isEmpty(username))
        {
            return AjaxResult.error("刷新令牌信息无效");
        }

        // 验证数据库中的刷新令牌
        if (!refreshTokenService.validateRefreshToken(refreshToken))
        {
            return AjaxResult.error("刷新令牌无效或已被撤销");
        }

        try
        {
            // 获取用户信息
            SysUser user = userService.selectUserById(userId);
            if (user == null || !"0".equals(user.getStatus()))
            {
                return AjaxResult.error("用户不存在或已被禁用");
            }

            if (!StringUtils.equals(username, user.getUserName()))
            {
                return AjaxResult.error("用户信息不匹配");
            }

            // 创建LoginUser用于生成新的Access Token
            SysUser accessUser = userService.selectUserByUserName(username);
            LoginUser loginUser = new LoginUser(accessUser, permissionService.getMenuPermission(accessUser));
            
            // 使用TokenService生成新的Access Token，确保与原有认证机制兼容
            String newAccessToken = tokenService.createToken(loginUser);

            // 创建新的Refresh Token
            String newRefreshToken = jwtUtils.generateRefreshToken(userId, username);
            Date newExpireTime = new Date(System.currentTimeMillis() + jwtUtils.getRefreshTokenExpire() * 60 * 1000L);

            // 获取设备信息和IP地址
            ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = servletRequestAttributes.getRequest();
            String userAgent = request.getHeader("User-Agent");
            String ip = IpUtils.getIpAddr();
            String ipAddress = AddressUtils.getRealAddressByIP(ip);

            // 简化的设备信息
            String deviceInfo = userAgent != null ? userAgent : "Unknown";

            // 更新数据库中的刷新令牌
            refreshTokenService.refreshToken(userId, username, newRefreshToken, newExpireTime, deviceInfo, ipAddress);

            // 构建返回结果
            AjaxResult ajax = AjaxResult.success();
            ajax.put("accessToken", newAccessToken);
            ajax.put("refreshToken", newRefreshToken);
            ajax.put("expiresIn", tokenService.getExpireTime() * 60); // 转换为秒

            return ajax;
        }
        catch (Exception e)
        {
            return AjaxResult.error("刷新令牌失败：" + e.getMessage());
        }
    }

    /**
     * 游客登录
     *
     * @return 结果
     */
    @PostMapping("/guest")
    public AjaxResult guestLogin()
    {
        // 检查是否开启游客登录
        String guestLoginEnabled = configService.selectConfigByKey("sys.account.guestLogin");
        if (!"true".equals(guestLoginEnabled))
        {
            return AjaxResult.error("系统未开启游客登录功能");
        }

        try
        {
            // 创建游客用户信息
            SysUser guestUser = createGuestUser();
            Set<String> guestPermissions = getGuestPermissions();

            // 创建LoginUser
            LoginUser loginUser = new LoginUser(guestUser, guestPermissions);

            // 获取请求信息
            ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = servletRequestAttributes.getRequest();
            String userAgent = request.getHeader("User-Agent");
            String ip = IpUtils.getIpAddr();

            loginUser.setIpaddr(ip);
            loginUser.setLoginLocation(AddressUtils.getRealAddressByIP(ip));

            // 使用TokenService生成token，确保与原有认证机制兼容
            String accessToken = tokenService.createToken(loginUser);
            String refreshTokenStr = jwtUtils.generateRefreshToken(guestUser.getUserId(), guestUser.getUserName());
            Date expireTime = new Date(System.currentTimeMillis() + jwtUtils.getRefreshTokenExpire() * 60 * 1000L);

            // 简化的设备信息
            String deviceInfo = userAgent != null ? userAgent : "Unknown";
            String ipAddress = AddressUtils.getRealAddressByIP(ip);

            // 保存刷新令牌到数据库
            refreshTokenService.createRefreshToken(guestUser.getUserId(), guestUser.getUserName(), refreshTokenStr, expireTime, deviceInfo, ipAddress);

            // 构建返回结果
            AjaxResult ajax = AjaxResult.success();
            ajax.put("user", buildUserResponse(guestUser, guestPermissions));
            ajax.put("accessToken", accessToken);
            ajax.put("refreshToken", refreshTokenStr);
            ajax.put("expiresIn", tokenService.getExpireTime() * 60);

            return ajax;
        }
        catch (Exception e)
        {
            return AjaxResult.error("游客登录失败：" + e.getMessage());
        }
    }

    /**
     * 获取当前用户信息
     *
     * @return 用户信息
     */
    @PreAuthorize("@ss.hasAnyRoles('admin,common,guest')")
    @GetMapping("/me")
    public AjaxResult getInfo()
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null)
        {
            return AjaxResult.error("用户未登录");
        }

        SysUser user = loginUser.getUser();
        Set<String> permissions = permissionService.getMenuPermission(user);
        Set<String> roles = permissionService.getRolePermission(user);

        AjaxResult ajax = AjaxResult.success();
        ajax.put("user", buildUserResponse(user, permissions));
        ajax.put("roles", roles);
        ajax.put("permissions", permissions);

        return ajax;
    }

    /**
     * 获取用户路由信息
     *
     * @return 路由信息
     */
    @GetMapping("/routers")
    public AjaxResult getRouters()
    {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menus = menuService.selectMenuTreeByUserId(userId);
        return AjaxResult.success(menuService.buildMenus(menus));
    }

    /**
     * 登出
     *
     * @param refreshToken 刷新令牌
     * @return 结果
     */
    @PostMapping("/logout")
    public AjaxResult logout(String refreshToken)
    {
        // 获取当前请求的 Access Token
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = servletRequestAttributes.getRequest();
        String accessToken = request.getHeader("Authorization");

        if (StringUtils.isNotEmpty(accessToken) && accessToken.startsWith("Bearer "))
        {
            accessToken = accessToken.substring(7);
        }

        Long userId = null;
        String userName = null;

        // 从 SecurityUtils 获取当前登录用户信息
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (StringUtils.isNotNull(loginUser))
        {
            userId = loginUser.getUserId();
            userName = loginUser.getUsername();
            // 删除用户缓存记录
            tokenService.delLoginUser(loginUser.getToken());
        }

        if (userId != null && userName != null)
        {
            // 删除刷新令牌
            if (StringUtils.isNotEmpty(refreshToken))
            {
                refreshTokenService.deleteSysRefreshTokenByRefreshToken(refreshToken);
            }
            // 记录用户退出日志
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(userName, Constants.LOGOUT, MessageUtils.message("user.logout.success")));
            // 记录登录信息
            recordLoginInfo(userId);
        }

        return AjaxResult.success(MessageUtils.message("user.logout.success"));
    }

    /**
     * 创建游客用户
     */
    private SysUser createGuestUser()
    {
        SysUser guestUser = new SysUser();
        guestUser.setUserId(0L); // 使用特殊ID标识游客
        guestUser.setUserName("GUEST_" + System.currentTimeMillis());
        guestUser.setNickName("访客");
        guestUser.setEmail("");
        guestUser.setPhonenumber("");
        guestUser.setSex("0");
        guestUser.setAvatar("");
        guestUser.setPassword("");
        guestUser.setStatus("0");
        guestUser.setDelFlag("0");
        guestUser.setLoginIp(IpUtils.getIpAddr());
        guestUser.setLoginDate(DateUtils.getNowDate());

        // 游客部门ID（特殊部门）
        guestUser.setDeptId(100L);

        return guestUser;
    }

    /**
     * 获取游客权限
     */
    private Set<String> getGuestPermissions()
    {
        Set<String> permissions = new java.util.HashSet<>();
        // 游客只能查看和基础分析权限
        permissions.add("contract:view");
        permissions.add("contract:analyze");
        return permissions;
    }

    /**
     * 构建用户响应对象
     */
    private Object buildUserResponse(SysUser user, Set<String> permissions)
    {
        java.util.Map<String, Object> userResponse = new java.util.HashMap<>();
        userResponse.put("id", user.getUserId());
        userResponse.put("username", user.getUserName());
        userResponse.put("email", user.getEmail());
        userResponse.put("phone", user.getPhonenumber());
        userResponse.put("avatar", user.getAvatar());
        userResponse.put("nickname", user.getNickName());
        userResponse.put("roles", user.getRoles().toArray());
        userResponse.put("permissions", permissions);
        userResponse.put("createdAt", user.getCreateTime() != null ? user.getCreateTime().toString() : DateUtils.getNowDate().toString());

        return userResponse;
    }

    /**
     * 记录登录信息
     */
    private void recordLoginInfo(Long userId)
    {
        // 更新用户登录信息
        if (userId != null && userId > 0)
        {
            userService.updateLoginInfo(userId, IpUtils.getIpAddr(), DateUtils.getNowDate());
        }
    }
}