package com.pickii.domain.auth;

/**
 * Verification Token 종류 (ENUM.md 4)
 * Redis Key: auth:verify:{UUID}
 */
public enum VerificationType {
    EMAIL, NICKNAME
}
