package com.pickii.global.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yaml 의 app.mail.* 설정 바인딩 (메일 발송 방식 선택)
 *
 * @param provider    smtp(기본) | brevo
 * @param from        발신자 이메일 (brevo는 Brevo에 인증된 주소여야 함)
 * @param senderName  받는 사람에게 표시되는 발신자 이름
 * @param brevoApiKey 환경변수 BREVO_API_KEY (provider=brevo일 때만 사용)
 */
@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(
        String provider,
        String from,
        String senderName,
        String brevoApiKey
) {
}
