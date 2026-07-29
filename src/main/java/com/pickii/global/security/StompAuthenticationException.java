package com.pickii.global.security;

/** STOMP CONNECT 인증 실패 시 던지는 예외 (연결 자체가 거부된다) */
public class StompAuthenticationException extends RuntimeException {

    public StompAuthenticationException(String message) {
        super(message);
    }
}
