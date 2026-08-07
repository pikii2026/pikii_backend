package com.pickii.domain.notification.listener;

import com.pickii.domain.notification.event.NotificationCreatedEvent;
import com.pickii.global.fcm.PushNotificationSender;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * NotificationHistory 저장 트랜잭션이 커밋된 이후, 별도 스레드에서 시스템 푸시(FCM)를 발송한다.
 * 인앱 알림함(NotificationHistory)은 이미 저장이 끝난 상태이므로, 여기서 실패해도
 * 지원 수락/일정 확정 같은 핵심 비즈니스 트랜잭션에는 영향이 없다.
 */
@Component
@RequiredArgsConstructor
public class PushNotificationEventListener {

    private final PushNotificationSender pushNotificationSender;

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreated(NotificationCreatedEvent event) {
        pushNotificationSender.send(event.memberId(), event.title(), event.content());
    }
}
