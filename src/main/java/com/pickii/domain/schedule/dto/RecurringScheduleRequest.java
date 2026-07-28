package com.pickii.domain.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * API_SPEC 7-7 개인 반복 일정 생성 요청
 */
public record RecurringScheduleRequest(
        @NotBlank(message = "일정 제목을 입력해주세요.")
        String title,

        @NotNull(message = "반복 시작일을 입력해주세요.")
        LocalDate startDate,

        @NotNull(message = "반복 종료일을 입력해주세요.")
        LocalDate endDate,

        @NotNull(message = "시작 시간을 입력해주세요.")
        LocalTime startTime,

        @NotNull(message = "종료 시간을 입력해주세요.")
        LocalTime endTime,

        @NotBlank(message = "반복 규칙(RRULE)을 입력해주세요.")
        String rrule,

        String content,

        Long categoryId
) {
}
