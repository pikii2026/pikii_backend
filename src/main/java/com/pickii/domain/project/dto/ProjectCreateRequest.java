package com.pickii.domain.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * API_SPEC 6-1 프로젝트 생성(그룹 채팅 생성) 요청
 */
public record ProjectCreateRequest(
        @NotBlank(message = "팀명을 입력해주세요.")
        @Size(max = 30, message = "팀명은 30자 이하여야 합니다.")
        String name
) {
}
