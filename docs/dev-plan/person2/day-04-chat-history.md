# Day 4 (반나절) — 이전 채팅 조회 (8-3)

## 오늘의 목표

| 엔드포인트 | Method/URL | DoD |
|---|---|---|
| 8-3 이전 채팅 조회 | `GET /chatrooms/{chatRoomId}/messages?cursor=&size=` | 200, 커서 기반 페이징 정상 |

**타이트 여부 코멘트**: 오늘은 목/금 반나절 날입니다. 다행히 8-3 하나만 있는 날이고, `ChatMessageRepository`에 필요한 쿼리가 이미 준비돼 있어서 **반나절 안에 충분히 끝낼 수 있는 분량**입니다. 여유가 조금 남으면 내일(Day5) 분량을 살짝 당겨서 시작해도 좋습니다.

---

## 사전 확인 사항

- `ChatMessageRepository.findAllByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId, Pageable pageable)`가 이미 존재 — 오늘은 이걸 커서 기반으로 감싸기만 하면 됩니다.
- MongoDB의 `_id`는 문자열(ObjectId)이라 일반적인 `page/size` 방식이 아니라 **cursor(마지막으로 받은 메시지 ID) 기반**으로 동작합니다.

---

## 구현 순서

```java
public ChatMessagePageResponse getMessages(Long memberId, Long chatRoomId, String cursor, int size) {
    if (!chatRoomMemberRepository.existsByChatRoomIdAndMemberId(chatRoomId, memberId)) {
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    if (!chatRoomRepository.existsById(chatRoomId)) {
        throw new BusinessException(ErrorCode.CHATROOM_NOT_FOUND);
    }

    List<ChatMessage> messages;
    if (cursor == null) {
        // 최초 조회: 가장 최근 메시지부터 size개
        messages = chatMessageRepository.findAllByChatRoomIdOrderByCreatedAtDesc(chatRoomId, PageRequest.of(0, size)).getContent();
    } else {
        // cursor 이전 메시지들. MongoTemplate으로 직접 쿼리하거나,
        // 간단하게는 Repository에 findByChatRoomIdAndIdLessThanOrderByCreatedAtDesc(Long, String, Pageable) 추가
        messages = chatMessageRepository.findByChatRoomIdAndIdLessThanOrderByCreatedAtDesc(
                chatRoomId, cursor, PageRequest.of(0, size));
    }

    boolean hasNext = messages.size() == size; // size만큼 꽉 찼으면 다음 페이지가 있을 가능성 있음(단순화된 판단)
    String nextCursor = messages.isEmpty() ? null : messages.get(messages.size() - 1).getId();

    List<ChatMessageItem> items = messages.stream().map(ChatMessageItem::from).toList();
    return new ChatMessagePageResponse(items, nextCursor, hasNext);
}
```

`ChatMessageRepository`에 추가:
```java
List<ChatMessage> findByChatRoomIdAndIdLessThanOrderByCreatedAtDesc(Long chatRoomId, String id, Pageable pageable);
```
> MongoDB의 ObjectId는 생성 시각 순서를 포함하고 있어서 문자열 비교(`LessThan`)로도 시간 순서가 유지됩니다. Spring Data MongoDB가 이 메서드 이름을 보고 자동으로 쿼리를 만들어줍니다.

Controller: `GET /chatrooms/{chatRoomId}/messages?cursor=&size=` (size 기본값 20)

---

## 예외 케이스

| 케이스 | 코드 |
|---|---|
| 채팅방 참여자 아님 | 403 `FORBIDDEN` |
| 존재하지 않는 채팅방 | 404 `CHATROOM_NOT_FOUND` |

---

## 테스트 체크리스트

- [ ] MongoDB에 테스트 메시지 5~10개 직접 삽입 (`mongosh` 또는 `docker exec`로) 후 조회
- [ ] cursor 없이 첫 조회 시 최신순으로 size개 반환
- [ ] 받은 `nextCursor`로 재조회 시 그 이전 메시지들이 반환되는지
- [ ] 채팅방 참여자 아닌 계정으로 조회 시도 → 403
- [ ] **GET 요청이므로 읽음 상태를 변경하지 않는지 확인** (스펙 명시 사항 — 8-6 읽음처리와 혼동하지 않도록)

---

## 커밋/PR 가이드

- 브랜치: `feat/chat-history`
- 커밋: `feat: 이전 채팅 조회 API 구현 (8-3)`

---

## 막힐 수 있는 포인트

- **MongoDB 테스트 데이터를 어떻게 넣을지 모르겠으면**: `docker exec -it pickii-mongo mongosh pickii`로 접속해서 `db.chat_messages.insertOne({...})` 형태로 직접 넣으면 됩니다. 지금까지 MySQL/Redis에 직접 데이터 심었던 것과 같은 요령입니다.
- **`hasNext` 판단이 완벽하지 않을 수 있음**: `size`만큼 꽉 찼다고 무조건 다음 페이지가 있다고 판단하면, 정확히 그 개수만큼만 남아있는 경우 오탐이 있을 수 있습니다. 실용적으로는 이 정도 근사치로 충분하니 오늘은 넘어가도 됩니다.
