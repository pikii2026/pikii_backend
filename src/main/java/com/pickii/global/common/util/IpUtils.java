package com.pickii.global.common.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 클라이언트 IP 추출 유틸 (프록시/로드밸런서 환경 고려)
 */
public final class IpUtils {

    private static final String[] HEADER_CANDIDATES = {
            "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "X-Real-IP"
    };

    private IpUtils() {
    }

    public static String resolve(HttpServletRequest request) {
        for (String header : HEADER_CANDIDATES) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value)) {
                // X-Forwarded-For는 "client, proxy1, proxy2" 형태이므로 첫 값(client)만 사용
                return value.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
