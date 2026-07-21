package com.pickii.global.exception;

import lombok.Getter;

/**
 * 비즈니스 로직 예외.
 *
 * <pre>
 * throw new BusinessException(ErrorCode.RECRUIT_NOT_FOUND);
 * </pre>
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** 기본 메시지 대신 상황별 메시지를 쓰고 싶을 때 */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
