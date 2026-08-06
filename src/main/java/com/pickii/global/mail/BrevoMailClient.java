package com.pickii.global.mail;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Brevo HTTPS API 기반 발송 (Railway 등 아웃바운드 SMTP 포트가 차단된 환경용).
 *
 * <p>443 포트 HTTPS 호출이므로 방화벽 영향을 받지 않는다.
 * 발신자(from)는 Brevo에 인증된 주소여야 한다.</p>
 */
@Component
@ConditionalOnProperty(prefix = "app.mail", name = "provider", havingValue = "brevo")
public class BrevoMailClient implements MailClient {

    private static final String SEND_URI = "https://api.brevo.com/v3/smtp/email";

    private final RestClient restClient;
    private final MailProperties properties;

    public BrevoMailClient(RestClient.Builder restClientBuilder, MailProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    @Override
    public void send(String to, String subject, String text) {
        Map<String, Object> body = Map.of(
                "sender", Map.of("email", properties.from(), "name", properties.senderName()),
                "to", List.of(Map.of("email", to)),
                "subject", subject,
                "textContent", text
        );
        restClient.post()
                .uri(SEND_URI)
                .header("api-key", properties.brevoApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
