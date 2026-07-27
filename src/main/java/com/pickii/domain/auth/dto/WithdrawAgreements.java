package com.pickii.domain.auth.dto;

import jakarta.validation.constraints.NotNull;

/**
 * API_SPEC 1-9 회원 탈퇴 필수 동의 항목
 */
public record WithdrawAgreements(
        @NotNull(message = "데이터 삭제 동의 여부를 지정해주세요.")
        Boolean dataDeletionAgreed,

        @NotNull(message = "재가입 정책 동의 여부를 지정해주세요.")
        Boolean rejoinPolicyAgreed
) {
    public boolean allAgreed() {
        return Boolean.TRUE.equals(dataDeletionAgreed) && Boolean.TRUE.equals(rejoinPolicyAgreed);
    }
}