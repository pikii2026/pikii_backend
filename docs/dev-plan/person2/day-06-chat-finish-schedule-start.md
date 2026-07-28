# Day 6 — Chat 마무리 + WebSocket + 개인일정 시작 (8-6~8-8, 7-1~7-6)

오늘은 주말 풀데이라 분량이 많습니다. **순서대로** 진행하세요: Chat REST 마무리 → WebSocket 실시간 통신 → 일정 카테고리 → 개인 단발 일정.

## 오늘의 목표

| 엔드포인트 | Method/URL | DoD |
|---|---|---|
| 8-6 채팅방 읽음 처리 | `PATCH /chatrooms/{chatRoomId}/read` | 204 |
| 8-7 채팅방 나가기 | `DELETE /chatrooms/{chatRoomId}/members/me` | 204, DIRECT/GROUP 분기 |
| 8-8 채팅방 알림 설정 | `PATCH /chatrooms/{chatRoomId}/notification` | 204 |
| (비번호) WebSocket 실시간 메시지 송수신 | STOMP `/pub/...`, `/sub/...` | 텍스트 메시지 실시간 전달 |
| 7-1~7-4 일정 카테고리 CRUD | `/users/me/schedule-categories` | 200/201/204 |
| 7-6 개인 단발 일정 생성 | `POST /users/me/schedules/single` | 201 |

---

## 사전 확인 사항

- `ChatRoomMember.updateReadCursor(String)`, `toggleNoti(boolean)` 메서드 이미 존재.
- `ScheduleCategory.update(String, String)` 이미 존재. `ScheduleCategoryRepository.findAllByMemberId` 이미 존재.
- 8-7의 GROUP 나가기는 **Day2에서 만든 `leaveInternal(ProjectMember)` 로직과 완전히 동일**합니다. 코드 중복을 줄이려면 `ProjectService`의 해당 로직을 재사용하거나 공통 메서드로 뽑아내세요.

---

## 구현 순서

### 8-6 읽음 처리

```java
@Transactional
public void markRead(Long memberId, Long chatRoomId, String lastReadMessageId) {
    ChatRoomMember crm = chatRoomMemberRepository.findByChatRoomIdAndMemberId(chatRoomId, memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
    crm.updateReadCursor(lastReadMessageId);
}
```

### 8-7 채팅방 나가기 — DIRECT/GROUP 분기

```java
@Transactional
public void leave(Long memberId, Long chatRoomId) {
    ChatRoom room = chatRoomRepository.findById(chatRoomId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHATROOM_NOT_FOUND));
    ChatRoomMember crm = chatRoomMemberRepository.findByChatRoomIdAndMemberId(chatRoomId, memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));

    if (room.getType() == ChatRoomType.DIRECT) {
        chatRoomMemberRepository.delete(crm); // 메시지는 MongoDB에 그대로 남음
        return;
    }
    // GROUP: 프로젝트 나가기(6-6)와 완전히 동일한 로직 — projectService의 leave 재사용
    projectService.leave(memberId, room.getProject().getId()); // 이 안에서 LEADER_CANNOT_LEAVE 체크까지 포함
}
```
> Day2에서 만든 `ProjectService.leave()`를 그대로 호출하면 중복 코드 없이 끝납니다. `LEADER_CANNOT_LEAVE` 에러도 자동으로 따라옵니다.

### 8-8 알림 설정

```java
@Transactional
public void updateNotification(Long memberId, Long chatRoomId, boolean enabled) {
    ChatRoomMember crm = chatRoomMemberRepository.findByChatRoomIdAndMemberId(chatRoomId, memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
    crm.toggleNoti(enabled);
}
```

### WebSocket 실시간 메시지 송수신 (신규 인프라)

STOMP 설정부터 추가해야 합니다 (`spring-boot-starter-websocket`은 이미 의존성에 있음).

```java
// global/config/WebSocketConfig.java (신규)
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/pub");
        registry.enableSimpleBroker("/sub"); // 개발 단계에선 내장 브로커로 충분
    }
}
```

```java
// domain/chat/controller/ChatMessageController.java (신규)
@Controller
@RequiredArgsConstructor
public class ChatMessageController {
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;

    @MessageMapping("/chatrooms/{chatRoomId}/messages") // 클라이언트가 /pub/chatrooms/{id}/messages 로 발행
    public void sendMessage(@DestinationVariable Long chatRoomId, @Payload ChatMessageSendRequest request) {
        ChatMessage saved = chatMessageRepository.save(ChatMessage.builder()
                .chatRoomId(chatRoomId).memberId(request.senderId())
                .type(request.type()).message(request.message()).imageUrl(request.imageUrl())
                .build());
        messagingTemplate.convertAndSend("/sub/chatrooms/" + chatRoomId, ChatMessageItem.from(saved));
        // 구독자(/sub/chatrooms/{id})가 실시간으로 수신
    }
}
```

**인증**: WebSocket 핸드셰이크 단계에서 JWT 검증이 필요한데, 이건 오늘 스코프에서 **가장 간단한 방식**으로 하세요 — STOMP CONNECT 프레임의 헤더에 Access Token을 실어보내고, `ChannelInterceptor`로 검증하는 방식이 정석이지만 시간이 부족하면 **연결 URL의 쿼리파라미터로 토큰을 받아 검증**하는 임시방편도 괜찮습니다 (보안은 데모 이후 보강).

---

### 7-1~7-4 일정 카테고리 CRUD

패턴은 Day1(Master Data)과 거의 같습니다.

```java
public List<ScheduleCategoryResponse> getCategories(Long memberId) {
    return scheduleCategoryRepository.findAllByMemberId(memberId).stream()
            .map(ScheduleCategoryResponse::from).toList();
}
@Transactional
public Long create(Long memberId, ScheduleCategoryRequest request) {
    return scheduleCategoryRepository.save(new ScheduleCategory(member, request.title(), request.color())).getId();
}
@Transactional
public void update(Long memberId, Long categoryId, ScheduleCategoryRequest request) {
    ScheduleCategory category = getOwnedCategory(memberId, categoryId);
    category.update(request.title(), request.color());
}
@Transactional
public void delete(Long memberId, Long categoryId) {
    ScheduleCategory category = getOwnedCategory(memberId, categoryId);
    scheduleCategoryRepository.delete(category);
    // 이 카테고리를 쓰던 MemberSchedule.category는 자동으로 NULL이 되는지 FK 설정 확인 (ON DELETE SET NULL)
}
```

### 7-6 개인 단발 일정 생성

```java
@Transactional
public Long createSingle(Long memberId, SingleScheduleRequest request) {
    if (!request.startTime().isBefore(request.endTime())) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
    ScheduleCategory category = request.categoryId() != null ? getOwnedCategory(memberId, request.categoryId()) : null;
    MemberSchedule schedule = new MemberSchedule(member, request.date(), request.date(),
            request.startTime(), request.endTime(), null, null, request.title(), request.content(), category);
    // 생성자 파라미터 순서는 MemberSchedule.java 실제 시그니처 확인 후 맞추기 (rrule=null, exDate=null로 단발 처리)
    return memberScheduleRepository.save(schedule).getId();
}
```

---

## 예외 케이스

| 케이스 | 코드 |
|---|---|
| 채팅방 참여자 아님(8-6,8-8) | 403 `FORBIDDEN` |
| GROUP 리더가 나가기 시도(8-7) | 409 `LEADER_CANNOT_LEAVE` |
| 카테고리 본인 것 아님 | 403 `FORBIDDEN` |
| 존재하지 않는 카테고리 | 404 `SCHEDULE_CATEGORY_NOT_FOUND` |
| startTime >= endTime | 400 `VALIDATION_FAILED` |

---

## 테스트 체크리스트

- [ ] 8-6: 읽음 커서 갱신 후 8-1(채팅방목록)의 unreadCount가 줄어드는지
- [ ] 8-7: DIRECT 방 나가기 → 내 목록에서 사라지고, 상대 쪽엔 '알 수 없음'으로 표시되는지
- [ ] 8-7: GROUP 방에서 일반 팀원 나가기 → Day2의 프로젝트 나가기와 동일하게 동작하는지
- [ ] 8-7: GROUP 방에서 리더가 나가기 시도 → 409
- [ ] 8-8: 알림 끄기/켜기 반영 확인
- [ ] WebSocket: 브라우저 콘솔이나 간단한 STOMP 클라이언트(Postman의 WebSocket 기능 등)로 연결 → 메시지 발행 → 구독 중인 다른 클라이언트가 실시간으로 받는지 확인. MongoDB에도 저장되는지 확인
- [ ] 일정 카테고리 CRUD 전체 사이클
- [ ] 단발 일정 생성 → DB에 `RRULE=NULL`, `StartDate=EndDate`로 저장되는지 확인

---

## 커밋/PR 가이드

- 브랜치: `feat/chat-finish` + `feat/schedule-personal-start` (WebSocket은 별도 커밋으로 분리 추천)
- 커밋: `feat: 채팅방 읽음/나가기/알림설정 API 구현 (8-6~8-8)` → `feat: WebSocket 실시간 채팅 인프라 구축` → `feat: 일정 카테고리 CRUD 구현 (7-1~7-4)` → `feat: 개인 단발 일정 생성 API 구현 (7-6)`

---

## 막힐 수 있는 포인트

- **오늘이 스프린트 전체에서 가장 새로운 기술(WebSocket/STOMP)을 다루는 날**이라 시간이 예상보다 걸릴 수 있습니다. 인증 부분은 위에서 말한 대로 "일단 되게 만들고 보안은 나중"으로 접근해도 괜찮습니다.
- **`MemberSchedule` 생성자 파라미터 순서**를 실제 파일에서 반드시 재확인하세요(위 예시는 추정 순서입니다).
- 8-7 GROUP 로직을 `ProjectService.leave()`로 그대로 위임할 때 **패키지 간 의존성 방향**에 주의하세요(`domain.chat` → `domain.project`를 의존하는 건 자연스럽지만, 반대 방향으로 의존이 생기지 않게).
