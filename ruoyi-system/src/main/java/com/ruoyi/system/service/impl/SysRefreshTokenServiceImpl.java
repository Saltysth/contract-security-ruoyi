package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.SysRefreshTokenMapper;
import com.ruoyi.system.domain.SysRefreshToken;
import com.ruoyi.system.service.ISysRefreshTokenService;

/**
 * 刷新令牌Service业务层处理
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Service
public class SysRefreshTokenServiceImpl implements ISysRefreshTokenService
{
    @Autowired
    private SysRefreshTokenMapper sysRefreshTokenMapper;

    /**
     * 查询刷新令牌
     *
     * @param tokenId 刷新令牌主键
     * @return 刷新令牌
     */
    @Override
    public SysRefreshToken selectSysRefreshTokenByTokenId(Long tokenId)
    {
        return sysRefreshTokenMapper.selectSysRefreshTokenByTokenId(tokenId);
    }

    /**
     * 根据refreshToken查询有效的令牌
     *
     * @param refreshToken 刷新令牌
     * @return 刷新令牌
     */
    @Override
    public SysRefreshToken selectSysRefreshTokenByRefreshToken(String refreshToken)
    {
        return sysRefreshTokenMapper.selectSysRefreshTokenByRefreshToken(refreshToken);
    }

    /**
     * 查询刷新令牌列表
     *
     * @param sysRefreshToken 刷新令牌
     * @return 刷新令牌
     */
    @Override
    public List<SysRefreshToken> selectSysRefreshTokenList(SysRefreshToken sysRefreshToken)
    {
        return sysRefreshTokenMapper.selectSysRefreshTokenList(sysRefreshToken);
    }

    /**
     * 根据用户ID查询有效的刷新令牌列表
     *
     * @param userId 用户ID
     * @return 刷新令牌集合
     */
    @Override
    public List<SysRefreshToken> selectSysRefreshTokenByUserId(Long userId)
    {
        return sysRefreshTokenMapper.selectSysRefreshTokenByUserId(userId);
    }

    /**
     * 新增刷新令牌
     *
     * @param sysRefreshToken 刷新令牌
     * @return 结果
     */
    @Override
    public int insertSysRefreshToken(SysRefreshToken sysRefreshToken)
    {
        sysRefreshToken.setCreateTime(DateUtils.getNowDate());
        return sysRefreshTokenMapper.insertSysRefreshToken(sysRefreshToken);
    }

    /**
     * 修改刷新令牌
     *
     * @param sysRefreshToken 刷新令牌
     * @return 结果
     */
    @Override
    public int updateSysRefreshToken(SysRefreshToken sysRefreshToken)
    {
        sysRefreshToken.setUpdateTime(DateUtils.getNowDate());
        return sysRefreshTokenMapper.updateSysRefreshToken(sysRefreshToken);
    }

    /**
     * 批量删除刷新令牌
     *
     * @param tokenIds 需要删除的刷新令牌主键
     * @return 结果
     */
    @Override
    public int deleteSysRefreshTokenByTokenIds(Long[] tokenIds)
    {
        return sysRefreshTokenMapper.deleteSysRefreshTokenByTokenIds(tokenIds);
    }

    /**
     * 删除刷新令牌信息
     *
     * @param tokenId 刷新令牌主键
     * @return 结果
     */
    @Override
    public int deleteSysRefreshTokenByTokenId(Long tokenId)
    {
        return sysRefreshTokenMapper.deleteSysRefreshTokenByTokenId(tokenId);
    }

    /**
     * 根据用户ID删除该用户的所有刷新令牌
     *
     * @param userId 用户ID
     * @return 结果
     */
    @Override
    public int deleteSysRefreshTokenByUserId(Long userId)
    {
        return sysRefreshTokenMapper.deleteSysRefreshTokenByUserId(userId);
    }

    /**
     * 删除过期的刷新令牌
     *
     * @return 结果
     */
    @Override
    public int deleteExpiredRefreshTokens()
    {
        return sysRefreshTokenMapper.deleteExpiredRefreshTokens();
    }

    /**
     * 根据refreshToken删除令牌
     *
     * @param refreshToken 刷新令牌
     * @return 结果
     */
    @Override
    public int deleteSysRefreshTokenByRefreshToken(String refreshToken)
    {
        return sysRefreshTokenMapper.deleteSysRefreshTokenByRefreshToken(refreshToken);
    }

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
    @Override
    public SysRefreshToken createRefreshToken(Long userId, String username, String refreshToken,
                                           Date expireTime, String deviceInfo, String ipAddress)
    {
        SysRefreshToken sysRefreshToken = new SysRefreshToken();
        sysRefreshToken.setUserId(userId);
        sysRefreshToken.setUsername(username);
        sysRefreshToken.setRefreshToken(refreshToken);
        sysRefreshToken.setExpireTime(expireTime);
        sysRefreshToken.setDeviceInfo(deviceInfo);
        sysRefreshToken.setIpAddress(ipAddress);
        sysRefreshToken.setStatus("0"); // 正常状态
        sysRefreshToken.setCreateTime(DateUtils.getNowDate());

        sysRefreshTokenMapper.insertSysRefreshToken(sysRefreshToken);
        return sysRefreshToken;
    }

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
    @Override
    public SysRefreshToken refreshToken(Long userId, String username, String newRefreshToken,
                                     Date newExpireTime, String deviceInfo, String ipAddress)
    {
        // 先删除该用户的旧令牌
        deleteSysRefreshTokenByUserId(userId);

        // 创建新令牌
        return createRefreshToken(userId, username, newRefreshToken, newExpireTime, deviceInfo, ipAddress);
    }

    /**
     * 验证刷新令牌是否有效
     *
     * @param refreshToken 刷新令牌
     * @return 是否有效
     */
    @Override
    public boolean validateRefreshToken(String refreshToken)
    {
        SysRefreshToken token = selectSysRefreshTokenByRefreshToken(refreshToken);
        if (token == null)
        {
            return false;
        }

        // 检查状态是否正常
        if (!"0".equals(token.getStatus()))
        {
            return false;
        }

        // 检查是否过期
        return token.getExpireTime().after(new Date());
    }

    /**
     * 禁用用户的所有刷新令牌（用于修改密码等敏感操作）
     *
     * @param userId 用户ID
     * @return 结果
     */
    @Override
    public int disableUserRefreshTokens(Long userId)
    {
        List<SysRefreshToken> tokens = selectSysRefreshTokenByUserId(userId);
        int count = 0;
        for (SysRefreshToken token : tokens)
        {
            token.setStatus("1"); // 停用状态
            token.setUpdateTime(DateUtils.getNowDate());
            updateSysRefreshToken(token);
            count++;
        }
        return count;
    }
}