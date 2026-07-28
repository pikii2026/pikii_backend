package com.pickii.domain.recruit.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * API_SPEC 3-2 공고 작성 요청
 */
public record RecruitCreateRequest(
        @NotBlank(message = "제목을 입력해주세요.")
        @Size(min = 2, max = 20, message = "제목은 2자 이상 20자 이하로 입력해주세요.")
        String title,

        @NotNull(message = "모집 인원을 입력해주세요.")
        @Min(value = 1, message = "모집 인원은 최소 1명 이상이어야 합니다.")
        @Max(value = 7, message = "모집 인원은 최대 7명까지 가능합니다.")
        Integer maxMembers,

        @NotNull(message = "onCampus 값을 지정해주세요.")
        Boolean onCampus,

        @NotEmpty(message = "카테고리를 1개 이상 선택해주세요.")
        List<Long> categoryIds,

        List<Long> topicIds,

        @NotNull(message = "시작일을 입력해주세요.")
        LocalDate startDate,

        @NotNull(message = "종료일을 입력해주세요.")
        LocalDate endDate,

        @Size(max = 50, message = "간단 소개는 50자 이하로 입력해주세요.")
        String simpleDesc,

        @NotBlank(message = "상세 내용을 입력해주세요.")
        @Size(max = 1000, message = "상세 내용은 1000자 이하로 입력해주세요.")
        String content
) {
}
