package com.pickii.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 발급 / 검증 (API_SPEC 0.9, REDIS_POLICY.md)
 */
@Component
public class JwtTokenProvider {

    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "ACCESS";
    private static final String TYPE_REFRESH = "REFRESH";

    private final SecretKey key;
    private final JwtProperties properties;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long memberId) {
        return createToken(memberId, TYPE_ACCESS, properties.accessTokenValidity());
    }

    public String createRefreshToken(Long memberId, boolean autoLogin) {
        long validity = autoLogin
                ? properties.refreshTokenValidityAutoLogin()
                : properties.refreshTokenValidity();
        return createToken(memberId, TYPE_REFRESH, validity);
    }

    private String createToken(Long memberId, String type, long validitySeconds) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validitySeconds * 1000);
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /** 서명·만료 검증. 유효하지 않으면 false */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getMemberId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    /** 로그아웃 Blacklist TTL 계산용: 남은 만료 시간(밀리초) */
    public long getRemainingExpiration(String token) {
        return parseClaims(token).getExpiration().getTime() - System.currentTimeMillis();
    }

    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(parseClaims(token).get(CLAIM_TYPE, String.class));
    }

    /**
     * Silent Refresh(1-6) 전용: Access Token은 만료된 상태로 들어오는 것이 정상이므로
     * 서명은 검증하되 만료(exp)는 허용하고 memberId를 꺼낸다.
     * 서명이 잘못됐거나 형식이 다르면 JwtException을 그대로 던진다.
     */
    public Long getMemberIdAllowExpired(String token) {
        return Long.parseLong(parseClaimsAllowExpired(token).getSubject());
    }

    public boolean isAccessTokenAllowExpired(String token) {
        return TYPE_ACCESS.equals(parseClaimsAllowExpired(token).get(CLAIM_TYPE, String.class));
    }

    /** Refresh Token 발급 당시 autoLogin 여부를 유효기간(exp-iat)으로 역산한다 (RTR 시 동일 TTL 정책 유지용) */
    public boolean isAutoLoginRefreshToken(String token) {
        Claims claims = parseClaims(token);
        long validitySeconds = (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000;
        return validitySeconds > properties.refreshTokenValidity();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Claims parseClaimsAllowExpired(String token) {
        try {
            return parseClaims(token);
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}
