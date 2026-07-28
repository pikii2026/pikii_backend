package com.pickii.domain.project.dto;

import com.pickii.domain.project.entity.ProjectStatus;

import java.time.LocalDate;

/**
 * API_SPEC 6-2 프로젝트 상세 조회 응답
 */
public record ProjectDetailResponse(
        Long projectId,
        String title,
        ProjectStatus status,
        LocalDate startDate,
        LocalDate endDate,
        Long leaderId
) {
}
