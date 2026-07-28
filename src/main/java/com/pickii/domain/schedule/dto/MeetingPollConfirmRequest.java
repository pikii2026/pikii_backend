package com.pickii.domain.schedule.dto;

import jakarta.validation.constraints.NotNull;

/**
 * API_SPEC 7-13 최종 일정 확정 요청
 */
public record MeetingPollConfirmRequest(
        @NotNull(message = "확정할 슬롯을 선택해주세요.")
        Long slotId,

        Boolean force
) {
}
