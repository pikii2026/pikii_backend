package com.pickii.domain.member.entity;

/**
 * 로그인 제공자 (ENUM.md 2)
 * 현재 KAKAO만 지원. GOOGLE / NAVER는 추후 확장용.
 */
public enum LoginProvider {
    LOCAL, KAKAO, GOOGLE, NAVER
}
