package com.pickii.domain.recruit.entity;

/**
 * 공고 모집 상태 (ENUM.md 5)
 *
 * <pre>
 * OPEN ──(마감)──> CLOSED ──(팀원 이탈로 추가 모집)──> ADDITIONAL ──(충원 완료)──> CLOSED
 * </pre>
 */
public enum RecruitStatus {
    OPEN, CLOSED, ADDITIONAL;

    /** 지원 가능 여부 (OPEN / ADDITIONAL만 지원 가능) */
    public boolean isApplicable() {
        return this != CLOSED;
    }
}
