package com.example.campussecondhand.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {
    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private static final long jwtExpiration = 86400;

    private static final String jwtSecret = "CampusSecondhandSecretKeyForJWTTokenGeneration2024SecureKey123456";

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
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
            return (Integer) claims.get("role");
        } catch (Exception e) {
            log.error("从JWT令牌中提取角色失败: ", e);
            return 0; // 默认角色为普通用户
        }
    }
}
