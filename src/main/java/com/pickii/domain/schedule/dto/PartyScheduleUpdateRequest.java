package com.pickii.domain.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * API_SPEC 7-17 팀 일정 수정 요청.
 * 개인 일정 수정(7-8)과 동일한 구조(단발이면 date, 반복이면 startDate/endDate/rrule)이되
 * categoryId는 없다 (색상은 7-19로 팀원별 지정).
 */
public record PartyScheduleUpdateRequest(
        @NotBlank(message = "일정 제목을 입력해주세요.")
        String title,

        LocalDate date,

        LocalDate startDate,

        LocalDate endDate,

        @NotNull(message = "시작 시간을 입력해주세요.")
        LocalTime startTime,

        @NotNull(message = "종료 시간을 입력해주세요.")
        LocalTime endTime,

        String rrule,

        String content
) {
}
