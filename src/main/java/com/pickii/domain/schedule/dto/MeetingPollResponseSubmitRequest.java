package com.pickii.domain.schedule.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * API_SPEC 7-12 응답 제출 요청
 */
public record MeetingPollResponseSubmitRequest(
        @NotNull(message = "불가한 슬롯 목록을 입력해주세요.")
        List<Long> unavailableSlotIds
) {
}
