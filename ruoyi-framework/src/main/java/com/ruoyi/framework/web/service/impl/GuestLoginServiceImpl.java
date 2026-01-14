package com.ruoyi.framework.web.service.impl;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.GuestLoginRequest;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.MessageUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.ip.AddressUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.common.utils.jwt.JwtUtils;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.framework.manager.factory.AsyncFactory;
import com.ruoyi.framework.web.service.IGuestLoginService;
import com.ruoyi.framework.web.service.SysPermissionService;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.service.ISysRefreshTokenService;
import com.ruoyi.system.service.ISysUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 游客登录Service实现
 *
 * @author ruoyi
 */
@Service
public class GuestLoginServiceImpl implements IGuestLoginService
{
    private static final Logger log = LoggerFactory.getLogger(GuestLoginServiceImpl.class);

    /** 游客角色名称 */
    private static final String GUEST_ROLE_NAME = "guest";

    /** 游客用户名前缀 */
    private static final String GUEST_USERNAME_PREFIX = "游客_";

    /** 游客默认昵称 */
    private static final String GUEST_NICKNAME = "游客";

    /** 游客默认密码 */
    private static final String GUEST_DEFAULT_PASSWORD = "123456";

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private SysPermissionService permissionService;

    @Autowired
    private ISysRefreshTokenService refreshTokenService;

    /**
     * 游客登录
     *
     * @param request 游客登录请求
     * @return 登录结果
     */
    @Override
    @Transactional
    public AjaxResult guestLogin(GuestLoginRequest request)
    {
        String guestUuid = request.getGuestUuid();
        String username = GUEST_USERNAME_PREFIX + guestUuid;

        log.info("游客登录开始，游客UUID: {}", guestUuid);

        // 查询游客用户是否存在
        SysUser existUser = userMapper.selectUserByUserName(username);
        boolean isFirstLogin = (existUser == null);

        if (isFirstLogin)
        {
            log.info("首次游客登录，开始创建游客账号，游客UUID: {}", guestUuid);
            // 首次登录，创建游客账号
            existUser = createGuestUser(guestUuid, username);
            log.info("游客账号创建成功，用户ID: {}, 用户名: {}", existUser.getUserId(), username);
        }
        else
        {
            log.info("游客已存在，用户ID: {}, 用户名: {}", existUser.getUserId(), username);

            // 检查用户状态
            if (UserConstants.USER_DISABLE.equals(existUser.getStatus()))
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, "游客账号已停用"));
                throw new ServiceException("游客账号已停用");
            }

            // 检查删除标志
            if ("2".equals(existUser.getDelFlag()))
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, "游客账号已删除"));
                throw new ServiceException("游客账号已删除");
            }
        }

        // 记录登录信息
        recordLoginInfo(existUser);
        AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success")));

        // 获取用户权限
        Set<String> permissions = permissionService.getMenuPermission(existUser);
        Set<String> roles = permissionService.getRolePermission(existUser);

        // 创建LoginUser对象
        LoginUser loginUser = new LoginUser(existUser.getUserId(), existUser.getDeptId(), existUser, permissions);

        // 生成Access Token
        String accessToken = tokenService.createToken(loginUser);

        // 生成Refresh Token
        String refreshToken = jwtUtils.generateRefreshToken(existUser.getUserId(), username);
        Date expireTime = new Date(System.currentTimeMillis() + jwtUtils.getRefreshTokenExpire() * 60 * 1000L);

        // 获取设备信息和IP地址
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest httpRequest = servletRequestAttributes != null ? servletRequestAttributes.getRequest() : null;
        String userAgent = httpRequest != null ? httpRequest.getHeader("User-Agent") : null;
        String ip = IpUtils.getIpAddr();
        String ipAddress = AddressUtils.getRealAddressByIP(ip);
        String deviceInfo = userAgent != null ? userAgent : "Unknown";

        // 保存刷新令牌到数据库
        refreshTokenService.createRefreshToken(existUser.getUserId(), username, refreshToken, expireTime, deviceInfo, ipAddress);

        log.info("游客登录成功，游客UUID: {}", guestUuid);

        // 构建返回结果
        AjaxResult ajax = AjaxResult.success();

        // 构建用户信息
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", existUser.getUserId().toString());
        userInfo.put("username", existUser.getUserName());
        userInfo.put("email", existUser.getEmail());
        userInfo.put("phone", existUser.getPhonenumber());
        userInfo.put("avatar", existUser.getAvatar());
        userInfo.put("nickname", existUser.getNickName());
        userInfo.put("roles", roles.toArray());
        userInfo.put("permissions", permissions);
        userInfo.put("createdAt", existUser.getCreateTime() != null ? existUser.getCreateTime().toString() : "");

        ajax.put("user", userInfo);
        ajax.put("accessToken", accessToken);
        ajax.put("refreshToken", refreshToken);
        ajax.put("expiresIn", tokenService.getExpireTime() * 60); // 转换为秒

        return ajax;
    }

    /**
     * 创建游客用户
     *
     * @param guestUuid 游客UUID
     * @param username 用户名
     * @return 游客用户
     */
    private SysUser createGuestUser(String guestUuid, String username)
    {
        // 查询guest角色
        SysRole guestRole = roleMapper.checkRoleNameUnique(GUEST_NICKNAME);
        if (guestRole == null)
        {
            log.error("游客角色不存在，角色名称: {}", GUEST_NICKNAME);
            throw new ServiceException("系统配置错误：游客角色不存在，请联系管理员");
        }

        // 检查角色状态
        if (UserConstants.ROLE_DISABLE.equals(guestRole.getStatus()))
        {
            log.error("游客角色已停用，角色名称: {}", GUEST_NICKNAME);
            throw new ServiceException("系统配置错误：游客角色已停用，请联系管理员");
        }

        // 检查角色删除标志
        if ("2".equals(guestRole.getDelFlag()))
        {
            log.error("游客角色已删除，角色名称: {}", GUEST_NICKNAME);
            throw new ServiceException("系统配置错误：游客角色已删除，请联系管理员");
        }

        // 创建游客用户
        SysUser guestUser = new SysUser();
        guestUser.setUserName(username);
        guestUser.setNickName(GUEST_USERNAME_PREFIX + guestUuid);
        guestUser.setPassword(SecurityUtils.encryptPassword(GUEST_DEFAULT_PASSWORD));
        guestUser.setStatus("0");
        guestUser.setDelFlag("0");
        guestUser.setCreateBy(GUEST_ROLE_NAME);
        guestUser.setCreateTime(DateUtils.getNowDate());
        guestUser.setRoleIds(new Long[] { guestRole.getRoleId() });

        // 插入用户（使用 userService.insertUser 会自动插入角色关联）
        int rows = userService.insertUser(guestUser);
        if (rows <= 0)
        {
            log.error("创建游客账号失败，游客UUID: {}", guestUuid);
            throw new ServiceException("创建游客账号失败，请稍后重试");
        }

        // 重新查询用户，获取完整的用户信息（包括userId和角色）
        guestUser = userMapper.selectUserByUserName(username);
        if (guestUser == null)
        {
            log.error("创建游客账号后查询失败，游客UUID: {}", guestUuid);
            throw new ServiceException("创建游客账号失败，请稍后重试");
        }

        log.info("游客账号角色绑定完成，用户ID: {}, 角色ID: {}", guestUser.getUserId(), guestRole.getRoleId());

        return guestUser;
    }

    /**
     * 记录登录信息
     *
     * @param user 用户
     */
    private void recordLoginInfo(SysUser user)
    {
        user.setLoginIp(IpUtils.getIpAddr());
        user.setLoginDate(DateUtils.getNowDate());
        userService.updateLoginInfo(user.getUserId(), user.getLoginIp(), user.getLoginDate());
    }
}
