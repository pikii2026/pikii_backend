package com.pickii.domain.auth;

/**
 * 이메일 인증 목적 (ENUM.md 3)
 * Redis Key: auth:code:{Purpose}:{Email}
 */
public enum VerificationPurpose {
    SIGNUP, PW_RESET, WITHDRAWAL
}
