package com.pickii.domain.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * API_SPEC 7-8 개인 일정 수정 요청.
 * 단발 일정이면 7-6과 동일하게 date를, 반복 일정이면 7-7과 동일하게 startDate/endDate/rrule을 사용한다.
 * 어느 쪽 필드를 검증할지는 기존 일정의 종류(rrule 유무)에 따라 서비스에서 분기한다
 * (단발 ↔ 반복 전환은 지원하지 않으므로 종류별 필수값은 어노테이션이 아니라 서비스에서 확인한다).
 */
public record ScheduleUpdateRequest(
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

        String content,

        Long categoryId
) {
}
