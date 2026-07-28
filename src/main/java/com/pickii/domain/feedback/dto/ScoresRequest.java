package com.pickii.domain.feedback.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * API_SPEC 4-10 상호평가 5개 문항 점수 (각 1~5점)
 */
public record ScoresRequest(
        @NotNull @Min(1) @Max(5) Integer responsibility,
        @NotNull @Min(1) @Max(5) Integer communication,
        @NotNull @Min(1) @Max(5) Integer deadline,
        @NotNull @Min(1) @Max(5) Integer cooperation,
        @NotNull @Min(1) @Max(5) Integer contribution
) {
}
