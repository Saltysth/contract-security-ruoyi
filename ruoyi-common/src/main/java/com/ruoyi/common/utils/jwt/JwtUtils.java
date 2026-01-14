package com.ruoyi.common.utils.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 *
 * 本类仅用于管理 Refresh Token 的生成、解析和验证
 *
 * 注意：Access Token 的生成和管理由 TokenService 负责，不使用本类
 * TokenService 生成的 Access Token 是基于 UUID 的传统 token，存储在 Redis 中
 * 本类生成的 Refresh Token 是基于 JWT 的标准 token，用于刷新 Access Token
 *
 * @author ruoyi
 */
@Component
public class JwtUtils
{
    /**
     * 密钥
     */
    @Value("${jwt.secret:ruoyi-secret-key-for-jwt-token-generation-and-validation}")
    private String secret;

    /**
     * Refresh Token有效期（默认7天，单位：分钟）
     */
    @Value("${jwt.refresh-token-expire:10080}")
    private int refreshTokenExpire;

    /**
     * Token类型标识
     */
    public static final String TOKEN_TYPE_KEY = "type";
    public static final String TOKEN_TYPE_REFRESH = "refresh";
    public static final String USER_ID_KEY = "userId";
    public static final String USERNAME_KEY = "username";

    /**
     * 生成Refresh Token
     *
     * @param userId 用户ID
     * @param username 用户名
     * @return Refresh Token
     */
    public String generateRefreshToken(Long userId, String username)
    {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_TYPE_KEY, TOKEN_TYPE_REFRESH);
        claims.put(USER_ID_KEY, userId);
        claims.put(USERNAME_KEY, username);
        return generateToken(claims, refreshTokenExpire);
    }

    /**
     * 从token中获取用户ID
     *
     * @param token token
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token)
    {
        try
        {
            Claims claims = parseToken(token);
            Object userIdObj = claims.get(USER_ID_KEY);
            if (userIdObj instanceof Integer)
            {
                return ((Integer) userIdObj).longValue();
            }
            else if (userIdObj instanceof Long)
            {
                return (Long) userIdObj;
            }
            else if (userIdObj instanceof String)
            {
                return Long.parseLong((String) userIdObj);
            }
            return null;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * 从token中获取用户名
     *
     * @param token token
     * @return 用户名
     */
    public String getUsernameFromToken(String token)
    {
        try
        {
            Claims claims = parseToken(token);
            return (String) claims.get(USERNAME_KEY);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * 从token中获取token类型
     *
     * @param token token
     * @return token类型
     */
    public String getTokenTypeFromToken(String token)
    {
        try
        {
            Claims claims = parseToken(token);
            return (String) claims.get(TOKEN_TYPE_KEY);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * 验证token是否为Refresh Token
     *
     * @param token token
     * @return 是否为Refresh Token
     */
    public boolean isRefreshToken(String token)
    {
        String tokenType = getTokenTypeFromToken(token);
        return TOKEN_TYPE_REFRESH.equals(tokenType);
    }



    /**
     * 验证token是否过期
     *
     * @param token token
     * @return 是否过期
     */
    public boolean isTokenExpired(String token)
    {
        try
        {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            return expiration.before(new Date());
        }
        catch (Exception e)
        {
            return true;
        }
    }


    /**
     * 获取Refresh Token有效时间（分钟）
     *
     * @return 有效时间
     */
    public int getRefreshTokenExpire()
    {
        return refreshTokenExpire;
    }

    /**
     * 生成token
     *
     * @param claims claims
     * @param expireMinutes 过期时间（分钟）
     * @return token
     */
    private String generateToken(Map<String, Object> claims, int expireMinutes)
    {
        Date expirationDate = new Date(System.currentTimeMillis() + expireMinutes * 60 * 1000L);

        return Jwts.builder()
                .setClaims(claims)
                .setExpiration(expirationDate)
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    /**
     * 从token中获取Claims
     *
     * @param token token
     * @return Claims
     */
    private Claims parseToken(String token)
    {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }
}