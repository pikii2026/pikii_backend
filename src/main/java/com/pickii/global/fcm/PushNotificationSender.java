package com.pickii.global.fcm;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.pickii.domain.notification.entity.DeviceToken;
import com.pickii.domain.notification.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * DeviceToken을 조회해 FCM으로 시스템 푸시를 보낸다. Firebase 미설정 상태에서는 조용히 스킵한다.
 * TODO: 재시도 로직은 없다 - 실패 시 무효 토큰(UNREGISTERED/INVALID_ARGUMENT)만 정리한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PushNotificationSender {

    private final DeviceTokenRepository deviceTokenRepository;

    @Transactional
    public void send(Long memberId, String title, String content) {
        if (FirebaseApp.getApps().isEmpty()) {
            return;
        }
        List<DeviceToken> tokens = deviceTokenRepository.findAllByMemberId(memberId);
        if (tokens.isEmpty()) {
            return;
        }

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens.stream().map(DeviceToken::getFcmToken).toList())
                .setNotification(Notification.builder().setTitle(title).setBody(content).build())
                .build();

        try {
            BatchResponse response = FirebaseMessaging.getInstance(FirebaseApp.getInstance())
                    .sendEachForMulticast(message);
            cleanupInvalidTokens(tokens, response);
        } catch (FirebaseMessagingException e) {
            log.warn("FCM 발송 실패 (memberId={})", memberId, e);
        }
    }

    private void cleanupInvalidTokens(List<DeviceToken> tokens, BatchResponse response) {
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);
            if (sendResponse.isSuccessful()) {
                continue;
            }
            MessagingErrorCode errorCode = sendResponse.getException() == null
                    ? null
                    : sendResponse.getException().getMessagingErrorCode();
            if (errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                deviceTokenRepository.deleteByFcmToken(tokens.get(i).getFcmToken());
            }
        }
    }
}
