package com.pickii.global.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * SMTP(spring.mail.*) 기반 발송. 로컬 개발 기본값.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.mail", name = "provider", havingValue = "smtp", matchIfMissing = true)
public class SmtpMailClient implements MailClient {

    private final JavaMailSender mailSender;

    @Override
    public void send(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
