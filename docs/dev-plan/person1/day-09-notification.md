# Day 9 — Notification (9-1 ~ 9-7)

## 오늘의 목표

| 엔드포인트 | Method/URL | DoD |
|---|---|---|
| 9-1 알림 목록 조회 | `GET /notifications` | Pagination |
| 9-2 알림 읽음 처리 | `PATCH /notifications/{notificationId}/read` | 204 |
| 9-3 전체 읽음 처리 | `PATCH /notifications/read-all` | 204 |
| 9-4 알림 삭제 | `DELETE /notifications/{notificationId}` | 204 |
| 9-5 알림 설정 조회 | `GET /users/me/notification-settings` | 200 |
| 9-6 알림 설정 수정 | `PATCH /users/me/notification-settings` | 204 |
| 9-7 안 읽은 알림 개수 | `GET /notifications/unread-count` | 200 |

**DoD**: 이 7개가 다 되면, 어제(Day7) 지원자 수락/거절이나 다른 도메인에서 "TODO: 알림 발송"으로 남겨뒀던 부분들을 실제로 채워 넣을 수 있는 기반이 완성됩니다.

---

## 사전 확인 사항

- `NotificationHistory.markRead()` 메서드는 이미 있습니다.
- `NotificationSetting`은 1-4(회원가입)에서 이미 생성 로직이 있습니다(`marketingNoti`만 회원가입 시 값 받고 나머지는 기본 true) — 오늘은 그걸 조회/수정하는 API만 만들면 됩니다.
- 결합 지점 없음(오늘은 "발송"이 아니라 "조회/설정" 위주). 다만 **다른 도메인(Recruit의 지원 알림, Project의 종료 알림 등)에서 알림을 실제로 만들어 넣으려면 공용 발송 메서드가 하나 있으면 편합니다** — 아래 "발송 헬퍼" 참고.

---

## 구현 순서

### 9-5, 9-6 — 알림 설정 조회/수정 (먼저 구현 — 제일 쉬움)

```java
public NotificationSettingResponse getSettings(Long memberId) {
    NotificationSetting setting = notificationSettingRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    return NotificationSettingResponse.from(setting);
}

@Transactional
public void updateSettings(Long memberId, NotificationSettingUpdateRequest request) {
    NotificationSetting setting = notificationSettingRepository.findById(memberId).orElseThrow(...);
    setting.update(request.chatNoti(), request.applicantNoti(), request.commentNoti(),
                    request.scheduleNoti(), request.matchNoti(), request.projectNoti(), request.marketingNoti());
    // NotificationSetting.java에 update() 메서드가 없으면 오늘 추가 (Setter 금지 규칙)
}
```

### 9-1, 9-7 — 조회 2종

```java
public PageResponse<NotificationResponse> getNotifications(Long memberId, Pageable pageable) {
    return PageResponse.from(notificationHistoryRepository.findByMemberId(memberId, pageable)
            .map(NotificationResponse::from));
}

public UnreadCountResponse getUnreadCount(Long memberId) {
    long count = notificationHistoryRepository.countByMemberIdAndIsReadFalse(memberId);
    return new UnreadCountResponse(count);
}
```
`NotificationHistoryRepository`에 `findByMemberId(Long, Pageable)`, `countByMemberIdAndIsReadFalse(Long)`, `findByMemberIdAndIsReadFalse(Long)` (9-3용) 쿼리 메서드 추가.

### 9-2, 9-3, 9-4 — 읽음/삭제 3종

```java
@Transactional
public void markRead(Long memberId, Long notificationId) {
    NotificationHistory noti = getOwnedNotification(memberId, notificationId);
    noti.markRead();
}

@Transactional
public void markAllRead(Long memberId) {
    notificationHistoryRepository.findByMemberIdAndIsReadFalse(memberId).forEach(NotificationHistory::markRead);
}

@Transactional
public void delete(Long memberId, Long notificationId) {
    NotificationHistory noti = getOwnedNotification(memberId, notificationId);
    notificationHistoryRepository.delete(noti);
}

private NotificationHistory getOwnedNotification(Long memberId, Long notificationId) {
    NotificationHistory noti = notificationHistoryRepository.findById(notificationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
    if (!noti.getMember().getId().equals(memberId)) {
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    return noti;
}
```

### 보너스: 다른 도메인에서 쓸 발송 헬퍼 (시간 남으면)

```java
// domain/notification/service/NotificationSender.java (신규, 시간 되면 추천)
@Component
@RequiredArgsConstructor
public class NotificationSender {
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final NotificationSettingRepository notificationSettingRepository;

    public void send(Member receiver, NotificationType type, String title, String content,
                      NotificationReferenceType refType, Long refId) {
        // 전역 알림 설정(type별)이 꺼져있으면 History만 남기고 실제 푸시는 안 보내는 정책도 가능
        notificationHistoryRepository.save(
                new NotificationHistory(receiver, title, content, type, refType, refId));
    }
}
```
이걸 만들어두면 Day7에서 남겨둔 "TODO: 알림 발송" 부분을 오늘 바로 채워 넣을 수 있습니다. 시간이 부족하면 이 헬퍼 없이 오늘의 7개 API만 완성하고, 발송 연동은 Day11(통합) 때 여유 봐서 채우세요.

---

## 예외 케이스

| 케이스 | 코드 |
|---|---|
| 본인 알림 아닌데 읽음/삭제 시도 | 403 `FORBIDDEN` |
| 존재하지 않는 알림 | 404 `NOTIFICATION_NOT_FOUND` |

---

## 테스트 체크리스트

- [ ] 알림 설정 조회 → 7개 필드 전부 나옴 (회원가입 때 marketingNoti가 pushNotiAgreed 값대로 저장됐는지도 재확인)
- [ ] 알림 설정 수정 → 재조회 시 반영
- [ ] (알림 데이터가 없으면) MySQL에 테스트용 NotificationHistory row 직접 INSERT 후 목록/안읽은개수 조회
- [ ] 알림 읽음 처리 → isRead true로 변경, readAt 기록
- [ ] 전체 읽음 처리 → 안읽은 개수가 0이 되는지
- [ ] 알림 삭제 → 목록에서 사라짐
- [ ] 남의 알림 읽음/삭제 시도 → 403

---

## 커밋/PR 가이드

- 브랜치: `feat/notification`
- 커밋: `feat: 알림 설정 조회/수정 API 구현 (9-5, 9-6)` → `feat: 알림 목록/읽음/삭제 API 구현 (9-1~9-4, 9-7)`

---

## 막힐 수 있는 포인트

- **테스트 데이터가 없어서 조회 API 검증이 애매할 수 있습니다.** 오늘까지 아직 아무 도메인도 실제로 알림을 "발송"하지 않았을 가능성이 높으니, MySQL에 직접 테스트 row를 넣어서 검증하는 게 제일 빠릅니다.
- **`isRead=false`인 알림 개수 세는 쿼리**는 `count` 메서드명 규칙(`countByMemberIdAndIsReadFalse`)을 Spring Data가 자동 구현해줍니다 — 오타 나면 애플리케이션 기동 시점에 에러가 나니 이름을 정확히 맞추세요.
