package com.skishop.auth.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class JwtUtil {

    private final SecretKey jwtSecret;
    private final int jwtExpirationInMinutes;
    private final int refreshExpirationInMinutes;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration:60}") int jwtExpirationInMinutes,
                   @Value("${jwt.refresh-expiration:1440}") int refreshExpirationInMinutes) {
        this.jwtSecret = Keys.hmacShaKeyFor(secret.getBytes());
        this.jwtExpirationInMinutes = jwtExpirationInMinutes;
        this.refreshExpirationInMinutes = refreshExpirationInMinutes;
    }

    public String generateToken(String userId, String role) {
        return generateToken(userId, role, Map.of());
    }

    public String generateToken(String userId, String role, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        Instant expiration = now.plus(jwtExpirationInMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .claims(extraClaims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(jwtSecret, Jwts.SIG.HS512)
                .compact();
    }

    public String generateRefreshToken(String userId) {
        Instant now = Instant.now();
        Instant expiration = now.plus(refreshExpirationInMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(jwtSecret, Jwts.SIG.HS512)
                .compact();
    }

    public String generateTempToken(String userId, String purpose) {
        Instant now = Instant.now();
        Instant expiration = now.plus(15, ChronoUnit.MINUTES); // 15分間有効

        return Jwts.builder()
                .subject(userId)
                .claim("purpose", purpose)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(jwtSecret, Jwts.SIG.HS512)
                .compact();
    }

    // UUIDを文字列に変換してからトークン生成
    public String generateTempToken(UUID userId, String purpose) {
        return generateTempToken(userId.toString(), purpose);
    }

    // アクセストークン生成メソッドを追加
    public String generateAccessToken(UUID userId, String role) {
        return generateToken(userId.toString(), role);
    }

    // テンポラリトークンの検証
    public boolean validateTempToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return !isTokenExpired(claims) && claims.get("purpose") != null;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid temp token: {}", e.getMessage());
            return false;
        }
    }

    // リフレッシュトークンの検証
    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return !isTokenExpired(claims);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid refresh token: {}", e.getMessage());
            return false;
        }
    }

    public String getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();
    }

    public String getRoleFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("role", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return !isTokenExpired(claims);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return isTokenExpired(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    private boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(jwtSecret)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Instant getExpirationFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration().toInstant();
    }

    // テスト用メソッド - extractUserId (テストで使用されている)
    public String extractUserId(String token) {
        return getUserIdFromToken(token);
    }

    // テスト用メソッド - validateToken (テストで使用されている)
    public boolean validateToken(String token) {
        return isTokenValid(token);
    }

    // テスト用メソッド - extractAllClaims (テストで使用されている)
    public Claims extractAllClaims(String token) {
        return getClaimsFromToken(token);
    }
}