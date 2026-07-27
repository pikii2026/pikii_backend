# Day 3 — Chat 채팅방 목록/상세/1:1 생성 (8-1, 8-2, 8-5)

## 오늘의 목표

| 엔드포인트 | Method/URL | DoD |
|---|---|---|
| 8-1 채팅방 목록 | `GET /chatrooms?type=` | DIRECT/GROUP 필터, unreadCount 계산 |
| 8-2 채팅방 상세 | `GET /chatrooms/{chatRoomId}` | GROUP이면 프로젝트 정보 포함 |
| 8-5 1:1 채팅방 생성 | `POST /chatrooms/direct` | 있으면 200, 없으면 201 |

**DoD**: 오늘은 REST 조회/생성만 다룹니다. 실제 메시지 송수신(WebSocket)은 Day6에서 다룹니다.

---

## 사전 확인 사항

- **좋은 소식**: MongoDB 쪽이 이미 잘 준비되어 있습니다. `ChatMessageRepository.findAllByChatRoomIdOrderByCreatedAtDesc()`, `countByChatRoomIdAndIdGreaterThan()`(읽음 커서 이후 메시지 수 = unreadCount)가 이미 구현되어 있습니다.
- `ChatRoomMemberRepository`의 `findAllByMemberId`, `findAllByChatRoomId`, `findByChatRoomIdAndMemberId`, `existsByChatRoomIdAndMemberId` 이미 존재.
- Day1~2에서 프로젝트를 만들었으면 GROUP 채팅방이 이미 하나 생성돼 있을 겁니다 — 오늘 그걸로 8-1, 8-2를 테스트할 수 있습니다.

---

## 구현 순서

### 8-1 채팅방 목록

```java
public PageResponse<ChatRoomListItem> getChatRooms(Long memberId, ChatRoomType type, Pageable pageable) {
    List<ChatRoomMember> memberships = chatRoomMemberRepository.findAllByMemberId(memberId);
    List<ChatRoomListItem> items = memberships.stream()
            .map(ChatRoomMember::getChatRoom)
            .filter(room -> type == null || room.getType() == type)
            .map(room -> {
                long unread = room.getMessages... // countByChatRoomIdAndIdGreaterThan(room.getId(), 내 lastReadMessageId)
                String title = resolveTitle(room, memberId); // DIRECT면 상대 닉네임, GROUP이면 프로젝트명
                return ChatRoomListItem.of(room, title, unread, lastMessage);
            })
            .toList();
    // Pageable은 코드상 직접 페이지네이션 처리 (List를 잘라서 PageResponse로 감싸기)
}

private String resolveTitle(ChatRoom room, Long memberId) {
    if (room.getType() == ChatRoomType.GROUP) {
        return room.getProject().getRecruit().getTitle(); // 또는 project.getName()
    }
    // DIRECT: 나 아닌 상대방 닉네임. 상대가 나갔으면(ChatRoomMember row 없음) '알 수 없음'
    return chatRoomMemberRepository.findAllByChatRoomId(room.getId()).stream()
            .filter(m -> !m.getMember().getId().equals(memberId))
            .findFirst()
            .map(m -> m.getMember().getNickname())
            .orElse("알 수 없음");
}
```

**최근 메시지(`lastMessage`, `lastMessageAt`)**: `ChatMessageRepository.findAllByChatRoomIdOrderByCreatedAtDesc(roomId, PageRequest.of(0,1))`로 1건만 가져오면 됩니다.

### 8-2 채팅방 상세

```java
public ChatRoomDetailResponse getDetail(Long memberId, Long chatRoomId) {
    ChatRoom room = chatRoomRepository.findById(chatRoomId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHATROOM_NOT_FOUND));
    if (!chatRoomMemberRepository.existsByChatRoomIdAndMemberId(chatRoomId, memberId)) {
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    List<MemberSummary> members = chatRoomMemberRepository.findAllByChatRoomId(chatRoomId).stream()
            .map(m -> new MemberSummary(m.getMember().getId(), m.getMember().getNickname()))
            .toList();
    // GROUP이면 프로젝트 정보(팀원 수, 진행기간, 상태, 내가 리더인지)도 함께 조립해서 반환
}
```
> 문서에 "프로젝트 관리(팀원 확인/회의/종료/연장/위임/퇴출)의 진입점이 그룹 채팅방"이라고 나와있습니다. 프론트가 이 응답을 보고 상단 메뉴를 구성하니, GROUP 타입일 때 최소한 `projectId`, `isLeader` 정도는 꼭 포함하세요.

### 8-5 1:1 채팅방 생성

```java
@Transactional
public DirectChatRoomResponse createOrGet(Long memberId, Long targetMemberId) {
    if (memberId.equals(targetMemberId)) throw new BusinessException(ErrorCode.CANNOT_CHAT_SELF);
    if (!memberRepository.existsById(targetMemberId)) throw new BusinessException(ErrorCode.USER_NOT_FOUND);

    Optional<ChatRoom> existing = findExistingDirectRoom(memberId, targetMemberId);
    if (existing.isPresent()) {
        return new DirectChatRoomResponse(existing.get().getId(), "DIRECT", false); // isNew=false → Controller에서 200
    }
    ChatRoom room = new ChatRoom(ChatRoomType.DIRECT, null);
    chatRoomRepository.save(room);
    chatRoomMemberRepository.save(new ChatRoomMember(room, member));
    chatRoomMemberRepository.save(new ChatRoomMember(room, target));
    return new DirectChatRoomResponse(room.getId(), "DIRECT", true); // isNew=true → Controller에서 201
}

private Optional<ChatRoom> findExistingDirectRoom(Long a, Long b) {
    // 두 사용자가 공통으로 속한 DIRECT 채팅방 찾기.
    // ChatRoomMemberRepository에 커스텀 쿼리 추가 필요:
    // @Query("SELECT crm1.chatRoom FROM ChatRoomMember crm1 JOIN ChatRoomMember crm2 " +
    //        "ON crm1.chatRoom = crm2.chatRoom " +
    //        "WHERE crm1.member.id = :a AND crm2.member.id = :b AND crm1.chatRoom.type = 'DIRECT'")
}
```
Controller에서 `isNew` 값에 따라 200/201을 분기해야 합니다:
```java
@PostMapping("/chatrooms/direct")
public ResponseEntity<ApiResponse<DirectChatRoomResponse>> createDirect(...) {
    DirectChatRoomResponse response = chatRoomService.createOrGet(memberId, request.targetMemberId());
    HttpStatus status = response.isNew() ? HttpStatus.CREATED : HttpStatus.OK;
    return ResponseEntity.status(status).body(ApiResponse.of(response));
}
```

---

## 예외 케이스

| 케이스 | 코드 |
|---|---|
| 채팅방 참여자 아닌데 상세조회 | 403 `FORBIDDEN` |
| 존재하지 않는 채팅방 | 404 `CHATROOM_NOT_FOUND` |
| 자기 자신과 채팅 시도 | 400 `CANNOT_CHAT_SELF` |
| 존재하지 않는 대상 회원 | 404 `USER_NOT_FOUND` |

---

## 테스트 체크리스트

- [ ] `type=GROUP` 필터로 조회 시 Day1~2에서 만든 프로젝트 채팅방이 나오는지
- [ ] `type=DIRECT` 필터, 전체 조회(type 없음) 각각 확인
- [ ] 1:1 채팅방 처음 생성 → 201, 같은 상대로 재요청 → 200 (같은 chatRoomId)
- [ ] 자기 자신에게 채팅 시도 → 400
- [ ] 존재하지 않는 회원과 채팅 시도 → 404
- [ ] 채팅방 상세조회 시 참여자가 아니면 403

---

## 커밋/PR 가이드

- 브랜치: `feat/chat-rooms`
- 커밋: `feat: 채팅방 목록/상세 조회 API 구현 (8-1, 8-2)` → `feat: 1:1 채팅방 생성 API 구현 (8-5)`

---

## 막힐 수 있는 포인트

- **두 사용자 간 기존 DIRECT 채팅방 찾기 쿼리**가 오늘 제일 까다로운 부분입니다. JPQL로 직접 짜는 게 제일 확실합니다(위 예시 참고). Native Query로 짜도 무방합니다.
- **`ChatRoom(ChatRoomType.DIRECT, null)`처럼 `project`에 null을 넣는 부분** — `@JoinColumn`이 `nullable=false`로 걸려있으면 에러가 납니다. DIRECT는 원래 `ProjectId = NULL`이어야 하니(DB_Schema 기준), nullable 처리가 안 돼 있으면 오늘 고치세요.
