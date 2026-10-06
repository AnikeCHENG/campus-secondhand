package com.example.campussecondhand.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

@Component
public class JwtUtil {
    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.expiration:86400}")
    private long jwtExpiration;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private SecretKey getSigningKey() {
        try {
            byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
            // HS512 要求密钥 >= 64 字节；不足时用 SHA-512 摘要派生成 64 字节，
            // 保证任意长度的配置密钥都能安全用于 HS512，且结果稳定可复现
            if (keyBytes.length < 64) {
                keyBytes = MessageDigest.getInstance("SHA-512").digest(keyBytes);
            }
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("无法初始化 JWT 签名密钥", e);
        }
    }

    public String generateToken(String username) {
        return generateToken(username, 0); // 默认角色为普通用户
    }

    public String generateToken(String username, Integer role) {
        try {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + jwtExpiration * 1000);

            return Jwts.builder()
                    .setSubject(username)
                    .claim("role", role)
                    .setIssuedAt(now)
                    .setExpiration(expiryDate)
                    .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                    .compact();
        } catch (Exception e) {
            log.error("生成JWT令牌失败: ", e);
            throw new RuntimeException("生成JWT令牌失败");
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.error("验证JWT令牌失败: ", e);
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (Exception e) {
            log.error("从JWT令牌中提取用户名失败: ", e);
            return null;
        }
    }

    public Integer getRoleFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            Object roleClaim = claims.get("role");
            return roleClaim == null ? 0 : ((Number) roleClaim).intValue();
        } catch (Exception e) {
            log.error("从JWT令牌中提取角色失败: ", e);
            return 0; // 默认角色为普通用户
        }
    }
}
