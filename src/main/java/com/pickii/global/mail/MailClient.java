package com.pickii.global.mail;

/**
 * 메일 발송 추상화.
 *
 * <p>로컬은 SMTP(Gmail), 배포 환경(Railway)은 아웃바운드 SMTP 포트가 차단되어
 * HTTPS API(Brevo)로 발송한다. 구현체는 app.mail.provider 값으로 선택된다.</p>
 */
public interface MailClient {

    void send(String to, String subject, String text);
}
