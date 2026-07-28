package com.pickii.domain.apply.dto;

import com.pickii.domain.apply.entity.ApplyStatus;
import jakarta.validation.constraints.NotNull;

/**
 * API_SPEC 4-8 지원자 수락 / 거절 요청
 */
public record ApplyStatusUpdateRequest(
        @NotNull(message = "변경할 상태를 입력해주세요.")
        ApplyStatus status
) {
}
