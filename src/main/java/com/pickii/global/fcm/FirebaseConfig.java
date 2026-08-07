package com.pickii.global.fcm;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * FCM_CREDENTIALS_JSON이 설정된 경우에만 FirebaseApp을 초기화한다.
 * 로컬 개발 환경 등 Firebase 서비스 계정 키가 없는 경우에도 서버 부팅에는 영향을 주지 않도록,
 * FirebaseApp을 Spring 빈으로 등록하지 않고 Firebase Admin SDK 자체의 정적 앱 레지스트리에만 등록한다
 * (Spring 빈으로 등록하면 required 의존성이 있는 컴포넌트가 "빈이 없을 수도 있는 상태"를 표현할 방법이
 * 마땅치 않다 — Optional&lt;FirebaseApp&gt;은 원래 빈 타입을 못 찾아서 항상 비어버리고,
 * null을 반환하는 @Bean은 필수 생성자 주입 시점에 후보 빈을 못 찾은 것으로 처리되어 부팅 자체가 실패한다).
 * 사용하는 쪽({@link PushNotificationSender})은 {@code FirebaseApp.getApps().isEmpty()}로 직접 확인한다.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class FirebaseConfig {

    private final FcmProperties properties;

    @PostConstruct
    public void initFirebaseApp() {
        if (!StringUtils.hasText(properties.credentialsJson())) {
            log.info("FCM_CREDENTIALS_JSON이 설정되지 않아 시스템 푸시(FCM) 발송이 비활성화됩니다.");
            return;
        }
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }
        try (ByteArrayInputStream in = new ByteArrayInputStream(
                properties.credentialsJson().getBytes(StandardCharsets.UTF_8))) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(in);
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();
            FirebaseApp app = FirebaseApp.initializeApp(options);
            log.info("FirebaseApp 초기화 완료 - 시스템 푸시(FCM) 발송이 활성화됩니다. (projectId={})", app.getOptions().getProjectId());
        } catch (IOException e) {
            log.warn("FCM 서비스 계정 키 파싱에 실패해 시스템 푸시(FCM) 발송이 비활성화됩니다.", e);
        }
    }
}
