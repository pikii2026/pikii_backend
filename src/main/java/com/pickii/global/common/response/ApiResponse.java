package com.pickii.global.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * 공통 성공 응답 래퍼 (API_SPEC 0.5)
 *
 * <pre>
 * {
 *     "data": {},
 *     "timestamp": "2026-07-06T19:30:00+09:00"
 * }
 * </pre>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final T data;
    private final OffsetDateTime timestamp;

    private ApiResponse(T data) {
        this.data = data;
        this.timestamp = OffsetDateTime.now(KST);
    }

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data);
    }
}
