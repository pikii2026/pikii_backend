package com.pickii.domain.schedule.dto;

/**
 * API_SPEC 7-6 개인 단발 일정 생성 / 7-7 개인 반복 일정 생성 응답
 */
public record ScheduleCreateResponse(
        Long scheduleId
) {
}
