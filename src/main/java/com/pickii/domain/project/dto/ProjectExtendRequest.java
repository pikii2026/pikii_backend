package com.pickii.domain.project.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * API_SPEC 6-5 프로젝트 진행기간 연장 요청
 */
public record ProjectExtendRequest(
        @NotNull(message = "새로운 종료일을 입력해주세요.")
        LocalDate endDate
) {
}
