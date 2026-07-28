package com.pickii.domain.project.dto;

import com.pickii.domain.project.entity.ProjectStatus;

import java.time.LocalDate;

/**
 * API_SPEC 6-5 프로젝트 진행기간 연장 응답
 */
public record ProjectExtendResponse(
        Long projectId,
        LocalDate endDate,
        ProjectStatus status
) {
}
