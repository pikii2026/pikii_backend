package com.pickii.global.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yaml 의 jwt.* 설정 바인딩
 *
 * @param secret                        서명 시크릿 (환경변수 JWT_SECRET)
 * @param accessTokenValidity           Access Token 유효시간(초) - 30분
 * @param refreshTokenValidity          Refresh Token 유효시간(초) - autoLogin=false 1일
 * @param refreshTokenValidityAutoLogin Refresh Token 유효시간(초) - autoLogin=true 30일
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessTokenValidity,
        long refreshTokenValidity,
        long refreshTokenValidityAutoLogin
) {
}
