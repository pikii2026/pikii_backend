package com.pickii.global.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yaml 의 gemini.* 설정 바인딩 (Gemini API 연동)
 *
 * @param apiKey 환경변수 GEMINI_API_KEY
 * @param model  Gemini 모델 ID (예: gemini-3.5-flash-lite)
 */
@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        String apiKey,
        String model
) {
}
