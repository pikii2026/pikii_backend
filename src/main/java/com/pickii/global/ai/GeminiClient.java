package com.pickii.global.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pickii.global.exception.BusinessException;
import com.pickii.global.exception.ErrorCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.List;
import java.util.Map;

/**
 * Gemini API(generateContent) 연동 (공고초안/지원메시지/자기소개/상호평가요약 등 AI 생성 기능 공통 클라이언트).
 *
 * <p>KakaoOAuthClient 패턴과 동일하게 RestClient.Builder를 그대로 받아 빌드하고,
 * 실패 시 {@code ErrorCode.AI_GENERATION_FAILED}로 변환한다.</p>
 */
@Component
public class GeminiClient {

    private static final String GENERATE_URI = "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent";

    private final RestClient restClient;
    private final GeminiProperties properties;

    public GeminiClient(RestClient.Builder restClientBuilder, GeminiProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 연결 타임아웃: 10초
        factory.setReadTimeout(120000);   // 읽기 타임아웃: 120초 (AI 응답을 충분히 기다려줌)

        this.restClient = restClientBuilder
                .requestFactory(factory)
                .build();
        this.properties = properties;
    }
    /** 자유 텍스트 생성 (리터칭/요약 등 단일 문자열 결과가 필요할 때) */
    public String generateText(String prompt) {
        return generate(prompt, null);
    }

    /** JSON 구조화 출력 생성. responseSchema는 Gemini의 OpenAPI 서브셋 스키마(Map)이며, 반환값은 원본 JSON 문자열이다. */
    public String generateJson(String prompt, Map<String, Object> responseSchema) {
        return generate(prompt, responseSchema);
    }

    private String generate(String prompt, Map<String, Object> responseSchema) {
        GenerationConfig generationConfig = responseSchema == null
                ? null
                : new GenerationConfig("application/json", responseSchema);
        GeminiRequest request = new GeminiRequest(
                List.of(new Content(List.of(new Part(prompt)))),
                generationConfig
        );

        try {
            GeminiResponse response = restClient.post()
                    .uri(GENERATE_URI, properties.model())
                    .header("x-goog-api-key", properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GeminiResponse.class);

            String text = extractText(response);
            if (!StringUtils.hasText(text)) {
                throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
            }
            return text;
        } catch (org.springframework.web.client.RestClientResponseException e) {
            // 구글 서버가 반환한 진짜 상태 코드와 에러 원문을 로그에 출력합니다.
            System.err.println(" [Gemini API 응답 에러] ");
            System.err.println("상태 코드: " + e.getStatusCode());
            System.err.println("에러 원문: " + e.getResponseBodyAsString());
            
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
        } catch (Exception e) {
            // 외부 API 요청 전/후의 네트워크 문제나 기타 예외
            System.err.println(" [네트워크/기타 에러]  : " + e.getMessage());
            
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
        }
    }

    private String extractText(GeminiResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            return null;
        }
        Content content = response.candidates().get(0).content();
        if (content == null || content.parts() == null || content.parts().isEmpty()) {
            return null;
        }
        return content.parts().get(0).text();
    }

    // ===== Gemini generateContent 요청/응답 wire 포맷 =====

    private record GeminiRequest(List<Content> contents, GenerationConfig generationConfig) {
    }

    private record Content(List<Part> parts) {
    }

    private record Part(String text) {
    }

    private record GenerationConfig(String responseMimeType, Map<String, Object> responseSchema) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiResponse(List<Candidate> candidates) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Candidate(Content content) {
    }
}
