# Push Notification (시스템 푸시 알림) 도입 계획

## 1. 목표

현재 알림(Notification 도메인)은 `NotificationHistory`에 DB row로 저장되고 앱 내 알림함(9-1~9-7)으로만 확인 가능하다.

**앱이 종료된 상태에서도 OS 알림(시스템 푸시)으로 수신**할 수 있도록 확장한다. 대상 클라이언트는 **모바일 앱(Android/iOS)**.

이 문서는 구현 전 설계/계획 문서다. 실제 구현 착수 시 아래 "문서 반영 대상"에 따라 API_SPEC.md / DB_Schema.md / ERROR_CODE.md / ENUM.md를 갱신한다.

---

## 2. 현재 상태 (조사 결과)

- 알림 발송 로직이 `ApplyService`, `PartyScheduleService`, `MeetingPollService`, `ProjectService` 4곳에 각각 유사한 `notify()` private 메서드로 중복 존재.
- 공통 패턴: `NotificationSetting`에서 해당 타입 토글 확인 → 켜져 있으면 `NotificationHistory` 저장. 이 시점이 실제로는 트랜잭션 내부(동기)에서 실행됨.
- FCM/APNs, 디바이스 토큰 저장, 실제 푸시 발송 코드는 전무. (`fcm`, `firebase`, `deviceToken`, `apns` 키워드 검색 결과 없음)
- 채팅은 WebSocket(STOMP, `SimpMessagingTemplate`)으로 실시간 전송하지만, 이는 앱이 켜져 소켓이 연결된 동안만 동작 — 종료 상태의 시스템 푸시와는 별개.

---

## 3. 기술 선택: FCM (Firebase Cloud Messaging)

- Android는 FCM이 사실상 표준, iOS도 FCM이 APNs를 대신 중계하므로 **백엔드는 FCM 하나만 연동하면 두 플랫폼 모두 커버**된다.
- 앱이 완전 종료된 상태에서도 OS 레벨(Android FCM 서비스 / iOS APNs)이 알림을 직접 처리하므로 요구사항(앱을 닫아도 옴)을 만족하는 사실상 유일한 방법.
- 연동은 Firebase Admin SDK 사용을 기본으로 하되(OAuth 토큰 갱신을 자동 처리해줌), 팀이 기존에 `GeminiClient`/`BrevoMailClient`처럼 REST 직접 호출 방식을 선호한다면 FCM HTTP v1 API를 서비스 계정 OAuth로 직접 호출하는 방식도 대안으로 남겨둔다.

---

## 4. 설계 상세

### 4-1. DB: `DeviceToken` 테이블 (신규)

| 컬럼 | 타입 | 설명 |
|:--|:--|:--|
| id | BIGINT PK | |
| member_id | BIGINT FK | Member 참조 |
| fcm_token | VARCHAR, UNIQUE | 기기별 FCM 등록 토큰 |
| platform | VARCHAR (ANDROID / IOS) | |
| created_at | DATETIME | |
| updated_at | DATETIME | 토큰 갱신 시각 |

- 회원 1명이 여러 기기(폰+태블릿 등)를 쓸 수 있으므로 **회원당 N개** 허용.
- `fcm_token`에 UNIQUE 제약. 같은 토큰으로 재등록 요청이 오면(재설치, 다른 계정 로그인 등) 소유 회원을 교체하는 **upsert**로 처리.
- Soft Delete 불필요 — 토큰은 무효화되면 즉시 하드 삭제 대상(개인정보 성격이 아니라 단순 세션성 값).

### 4-2. API 2개 추가 (Notification 도메인)

| Method | Endpoint | 설명 |
|:--|:--|:--|
| POST | `/devices` | 앱 로그인/포그라운드 진입 시 FCM 토큰 등록(upsert) |
| DELETE | `/devices` | 로그아웃 시 해당 기기 토큰 삭제 (body 또는 쿼리로 token 전달) |

### 4-3. Firebase Admin SDK 연동

- `build.gradle`에 `com.google.firebase:firebase-admin` 의존성 추가.
- Firebase 콘솔에서 서비스 계정 키(JSON) 발급 → 기존 `.env` 패턴(`.env.example`에 플레이스홀더 추가)으로 관리, 절대 커밋하지 않음.
- `FcmProperties`(`GeminiProperties`/`MailProperties`와 동일한 패턴) + `FirebaseConfig`에서 앱 초기화 Bean 등록.

### 4-4. `PushNotificationSender` 컴포넌트 (신설)

- 입력: 회원, 제목, 본문, `NotificationType` / `NotificationReferenceType` / `referenceId`.
- 해당 회원의 모든 `DeviceToken` 조회 → FCM `sendEachForMulticast`로 발송.
- data payload에 `type` / `referenceType` / `referenceId`를 실어 앱이 탭했을 때 딥링크 라우팅 가능하게 함.
- 발송 결과에서 `UNREGISTERED` / `INVALID_ARGUMENT`로 실패한 토큰은 그 자리에서 `DeviceToken`에서 삭제(무효 토큰 자동 정리).

### 4-5. 기존 4곳 `notify()` 통합 + 트랜잭션 이후 비동기 발송

- `NotificationHistory` 저장(트랜잭션 내부, 동기)은 현행 유지.
- FCM 발송은 **트랜잭션 커밋 이후 + 비동기**로 분리한다: `ApplicationEventPublisher` + `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async`.
  - 이유: FCM 호출이 느리거나 실패해도 지원 수락/일정 확정 같은 핵심 비즈니스 트랜잭션에 영향이 없어야 함.
- 겸사겸사 4곳에 중복된 `notify()` 로직을 도메인 공통 `NotificationSender`로 통합 권장(신규 알림 추가 시마다 반복 코드가 생기지 않도록).

---

## 5. 문서 반영 대상 (구현 착수 시)

| 문서 | 반영 내용 |
|:--|:--|
| `docs/API_SPEC.md` | `POST /devices`, `DELETE /devices` 명세 추가 |
| `docs/DB_Schema.md` | `DeviceToken` 테이블 추가 |
| `docs/ERROR_CODE.md` | 토큰 등록/삭제 관련 에러코드 추가 |
| `docs/ENUM.md` | `Platform`(ANDROID/IOS) Enum 추가 |
| `docs/README.md` | 문서 목록 표에 본 문서 추가 |

---

## 6. 구현 순서 (제안)

1. `DeviceToken` 엔티티/레포지토리 + 토큰 등록/삭제 API
2. Firebase 프로젝트 생성 + 서비스 계정 키 발급 + Admin SDK 연동/설정
3. `PushNotificationSender` 작성 (발송 + 무효 토큰 정리)
4. 트랜잭션-커밋-후 비동기 발송 구조로 전환 + 기존 4곳 `notify()` 통합
5. 문서 반영 + 컴파일 확인 + 수동 테스트(Postman으로 토큰 등록 → 임의 알림 트리거 → 실제 기기 수신 확인)

---

## 7. 범위 밖 (앱/프론트 레포에서 별도 처리)

- Firebase SDK 초기화, 토큰 발급 후 `POST /devices` 호출
- Android 알림 채널 설정, iOS APNs 인증서/키 Firebase 콘솔 등록
- 알림 탭 시 payload 기반 딥링크 라우팅

---

## 8. 열린 질문 / 추후 결정 사항

- `notify()` 4곳 통합 리팩터링을 이번 작업에 포함할지, 우선 푸시 기능만 얹고 리팩터링은 별도 작업으로 미룰지.
- Firebase Admin SDK vs REST 직접 호출(팀 컨벤션 통일 여부).
- 발송 실패/재시도 정책 (현재 계획은 실패 시 무효 토큰만 정리하고 재시도는 하지 않음 — 필요시 추가 논의).
