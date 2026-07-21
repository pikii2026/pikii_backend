package com.pickii.global.exception;

import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * 공통 에러 응답 (API_SPEC 0.6)
 *
 * <pre>
 * {
 *     "error": { "code": "ERROR_CODE", "message": "Error Message" },
 *     "timestamp": "2026-07-06T19:30:00+09:00"
 * }
 * </pre>
 */
public record ErrorResponse(
        ErrorDetail error,
        OffsetDateTime timestamp
) {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    public record ErrorDetail(String code, String message) {
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return of(errorCode, errorCode.getMessage());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(
                new ErrorDetail(errorCode.name(), message),
                OffsetDateTime.now(KST)
        );
    }
}
