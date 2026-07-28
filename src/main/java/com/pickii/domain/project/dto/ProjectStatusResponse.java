package com.pickii.domain.project.dto;

import com.pickii.domain.project.entity.ProjectStatus;

import java.time.LocalDate;

/**
 * API_SPEC 6-9 프로젝트 상태 조회 응답
 */
public record ProjectStatusResponse(
        ProjectStatus status,
        int progressRate,
        LocalDate endDate
) {
}
