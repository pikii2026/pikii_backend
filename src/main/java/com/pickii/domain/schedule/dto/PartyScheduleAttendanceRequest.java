package com.pickii.domain.schedule.dto;

import jakarta.validation.constraints.NotNull;

/**
 * API_SPEC 7-20 회의 참석 / 불참 변경 요청
 */
public record PartyScheduleAttendanceRequest(
        @NotNull(message = "참석 여부를 입력해주세요.")
        Boolean attending
) {
}
