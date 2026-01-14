package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.SysRefreshToken;

/**
 * 刷新令牌Mapper接口
 *
 * @author ruoyi
 * @date 2024-01-01
 */
public interface SysRefreshTokenMapper
{
    /**
     * 查询刷新令牌
     *
     * @param tokenId 刷新令牌主键
     * @return 刷新令牌
     */
    public SysRefreshToken selectSysRefreshTokenByTokenId(Long tokenId);

    /**
     * 根据refreshToken查询
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
     * 删除刷新令牌
     *
     * @param tokenId 刷新令牌主键
     * @return 结果
     */
    public int deleteSysRefreshTokenByTokenId(Long tokenId);

    /**
     * 批量删除刷新令牌
     *
     * @param tokenIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysRefreshTokenByTokenIds(Long[] tokenIds);

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
}