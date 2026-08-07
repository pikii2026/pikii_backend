package com.pickii.global.fcm;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yaml 의 fcm.* 설정 바인딩 (FCM 백그라운드 푸시 연동)
 *
 * @param credentialsJson Firebase 서비스 계정 키 JSON 원문 (환경변수 FCM_CREDENTIALS_JSON).
 *                        Railway 등 파일 업로드가 번거로운 배포 환경을 고려해 파일 경로가 아닌
 *                        JSON 문자열 전체를 환경변수로 주입받는다. 미설정 시 FCM 연동은 비활성화된다.
 */
@ConfigurationProperties(prefix = "fcm")
public record FcmProperties(
        String credentialsJson
) {
}
