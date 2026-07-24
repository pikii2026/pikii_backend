package com.pickii.domain.auth;

/**
 * Redis Key 생성 유틸 (REDIS_POLICY.md, ENUM.md 18)
 */
public final class RedisKey {

    private RedisKey() {
    }

    /** Refresh Token: auth:refresh:{MemberId}:{DeviceId} — TTL 1일 / 30일(autoLogin) */
    public static String refreshToken(Long memberId, String deviceId) {
        return "auth:refresh:%d:%s".formatted(memberId, deviceId);
    }

    /** Access Token Blacklist: auth:blacklist:{AccessToken} — TTL 남은 만료시간 */
    public static String blacklist(String accessToken) {
        return "auth:blacklist:" + accessToken;
    }

    /** 이메일 인증코드: auth:code:{Purpose}:{Email} — TTL 180초 */
    public static String emailCode(VerificationPurpose purpose, String email) {
        return "auth:code:%s:%s".formatted(purpose.name(), email);
    }

    /** 이메일 인증 요청 IP 제한: auth:code:ip:{IP} — TTL 10분, 최대 5회 */
    public static String emailCodeIpLimit(String ip) {
        return "auth:code:ip:" + ip;
    }

    /** Verification Token: auth:verify:{UUID} — TTL 900초 */
    public static String verificationToken(String uuid) {
        return "auth:verify:" + uuid;
    }
}
