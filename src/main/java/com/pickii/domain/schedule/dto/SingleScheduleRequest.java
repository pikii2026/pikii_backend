package com.pickii.domain.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * API_SPEC 7-6 개인 단발 일정 생성 요청
 */
public record SingleScheduleRequest(
        @NotBlank(message = "일정 제목을 입력해주세요.")
        String title,

        @NotNull(message = "일정 날짜를 입력해주세요.")
        LocalDate date,

        @NotNull(message = "시작 시간을 입력해주세요.")
        LocalTime startTime,

        @NotNull(message = "종료 시간을 입력해주세요.")
        LocalTime endTime,

        String content,

        Long categoryId
) {
}
