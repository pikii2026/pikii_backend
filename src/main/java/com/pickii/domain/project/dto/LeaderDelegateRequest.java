package com.pickii.domain.project.dto;

import jakarta.validation.constraints.NotNull;

/**
 * API_SPEC 6-8 프로젝트장 위임 요청
 */
public record LeaderDelegateRequest(
        @NotNull(message = "위임할 팀원을 입력해주세요.")
        Long newLeaderId
) {
}
