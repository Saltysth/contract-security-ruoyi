package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.SysRefreshToken;

/**
 * 刷新令牌Service接口
 *
 * @author ruoyi
 * @date 2024-01-01
 */
public interface ISysRefreshTokenService
{
    /**
     * 查询刷新令牌
     *
     * @param tokenId 刷新令牌主键
     * @return 刷新令牌
     */
    public SysRefreshToken selectSysRefreshTokenByTokenId(Long tokenId);

    /**
     * 根据refreshToken查询有效的令牌
     *
     * @param refreshToken 刷新令牌
     * @return 刷新令牌
     */
    public SysRefreshToken selectSysRefreshTokenByRefreshToken(String refreshToken);

    /**
     * 查询刷新令牌列表
     *
     * @param sysRefreshToken 刷新令牌
     * @return 刷新令牌集合
     */
    public List<SysRefreshToken> selectSysRefreshTokenList(SysRefreshToken sysRefreshToken);

    /**
     * 根据用户ID查询有效的刷新令牌列表
     *
     * @param userId 用户ID
     * @return 刷新令牌集合
     */
    public List<SysRefreshToken> selectSysRefreshTokenByUserId(Long userId);

    /**
     * 新增刷新令牌
     *
     * @param sysRefreshToken 刷新令牌
     * @return 结果
     */
    public int insertSysRefreshToken(SysRefreshToken sysRefreshToken);

    /**
     * 修改刷新令牌
     *
     * @param sysRefreshToken 刷新令牌
     * @return 结果
     */
    public int updateSysRefreshToken(SysRefreshToken sysRefreshToken);

    /**
     * 批量删除刷新令牌
     *
     * @param tokenIds 需要删除的刷新令牌主键集合
     * @return 结果
     */
    public int deleteSysRefreshTokenByTokenIds(Long[] tokenIds);

    /**
     * 删除刷新令牌信息
     *
     * @param tokenId 刷新令牌主键
     * @return 结果
     */
    public int deleteSysRefreshTokenByTokenId(Long tokenId);

    /**
     * 根据用户ID删除该用户的所有刷新令牌
     *
     * @param userId 用户ID
     * @return 结果
     */
    public int deleteSysRefreshTokenByUserId(Long userId);

    /**
     * 删除过期的刷新令牌
     *
     * @return 结果
     */
    public int deleteExpiredRefreshTokens();

    /**
     * 根据refreshToken删除令牌
     *
     * @param refreshToken 刷新令牌
     * @return 结果
     */
    public int deleteSysRefreshTokenByRefreshToken(String refreshToken);

    /**
     * 创建新的刷新令牌
     *
     * @param userId 用户ID
     * @param username 用户名
     * @param refreshToken 刷新令牌
     * @param expireTime 过期时间
     * @param deviceInfo 设备信息
     * @param ipAddress IP地址
     * @return 刷新令牌
     */
    public SysRefreshToken createRefreshToken(Long userId, String username, String refreshToken,
                                           java.util.Date expireTime, String deviceInfo, String ipAddress);

    /**
     * 刷新用户的令牌（删除旧的，创建新的）
     *
     * @param userId 用户ID
     * @param username 用户名
     * @param newRefreshToken 新的刷新令牌
     * @param newExpireTime 新的过期时间
     * @param deviceInfo 设备信息
     * @param ipAddress IP地址
     * @return 新的刷新令牌
     */
    public SysRefreshToken refreshToken(Long userId, String username, String newRefreshToken,
                                     java.util.Date newExpireTime, String deviceInfo, String ipAddress);

    /**
     * 验证刷新令牌是否有效
     *
     * @param refreshToken 刷新令牌
     * @return 是否有效
     */
    public boolean validateRefreshToken(String refreshToken);

    /**
     * 禁用用户的所有刷新令牌（用于修改密码等敏感操作）
     *
     * @param userId 用户ID
     * @return 结果
     */
    public int disableUserRefreshTokens(Long userId);
}