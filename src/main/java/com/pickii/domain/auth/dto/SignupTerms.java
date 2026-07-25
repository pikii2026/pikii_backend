package com.pickii.domain.auth.dto;

import jakarta.validation.constraints.NotNull;

/**
 * API_SPEC 1-4 회원가입 약관 동의 항목
 *
 * <p>{@code pushNotiAgreed}(마케팅/광고성 정보 수신 동의)만 선택 동의이며,
 * NotificationSetting의 marketingNoti 초기값으로 그대로 저장된다.
 * 나머지 5개 항목은 서비스 이용에 필수인 동의로 전부 true여야 가입할 수 있다.</p>
 */
public record SignupTerms(
        @NotNull Boolean ageAgreed,
        @NotNull Boolean termsAgreed,
        @NotNull Boolean privacyAgreed,
        @NotNull Boolean profileShareAgreed,
        @NotNull Boolean dataCollectionAgreed,
        @NotNull Boolean pushNotiAgreed
) {
    public boolean allRequiredAgreed() {
        return Boolean.TRUE.equals(ageAgreed)
                && Boolean.TRUE.equals(termsAgreed)
                && Boolean.TRUE.equals(privacyAgreed)
                && Boolean.TRUE.equals(profileShareAgreed)
                && Boolean.TRUE.equals(dataCollectionAgreed);
    }
}
