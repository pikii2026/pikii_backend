# Day 10 — 팀 일정 마무리 (7-17 ~ 7-20)

## 오늘의 목표

| 엔드포인트 | Method/URL | DoD |
|---|---|---|
| 7-17 팀 일정 수정 | `PATCH /party-schedules/{scheduleId}` | 204 |
| 7-18 팀 일정 삭제 | `DELETE /party-schedules/{scheduleId}` | 204 |
| 7-19 프로젝트 색상 지정 | `PUT /projects/{projectId}/schedule-category` | 204 |
| 7-20 회의 참석/불참 변경 | `PATCH /party-schedules/{scheduleId}/attendance` | 204, 채팅방 시스템 메시지 발송 |

**DoD**: 오늘로 Schedule 도메인(7장 전체, 20개 엔드포인트)이 완결됩니다.

---

## 사전 확인 사항

- `PartyScheduleAttendance` 관련 로직은 Day8(7-13), Day9(7-16)에서 이미 만든 패턴을 그대로 재사용합니다.
- 7-19의 `ProjectScheduleCategory`는 Day9에서 "내일 마저 만들기"로 미뤄둔 것입니다 — 오늘 먼저 만들고 나서, Day9에서 단순화했던 7-15의 색상 조회 로직도 오늘 마저 완성하세요.

---

## 구현 순서

### 7-19 프로젝트 색상 지정 (먼저 구현 — 7-15 완성도에 영향)

```java
@Transactional
public void setCategory(Long memberId, Long projectId, Long categoryId) {
    if (!projectMemberRepository.existsByProjectIdAndMemberIdAndLeftAtIsNull(projectId, memberId)) {
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    ScheduleCategory category = scheduleCategoryRepository.findById(categoryId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_CATEGORY_NOT_FOUND));
    if (!category.getMember().getId().equals(memberId)) {
        throw new BusinessException(ErrorCode.SCHEDULE_CATEGORY_NOT_FOUND); // 남의 카테고리는 "없는 것"처럼 처리
    }
    // 기존 매핑 있으면 갱신, 없으면 생성 (복합키 upsert)
    projectScheduleCategoryRepository.save(new ProjectScheduleCategory(categoryId, projectId));
}
```
`ProjectScheduleCategoryRepository` 신규 생성 필요 (복합키: SCId + ProjectId).

**어제(Day9) 미뤄뒀던 7-15 색상 로직 마저 완성**:
```java
private String resolveColor(Long memberId, Long projectId) {
    return projectScheduleCategoryRepository.findByProjectIdAndMemberId(projectId, memberId) // Repository에 추가
            .map(mapping -> scheduleCategoryRepository.findById(mapping.getScId()).map(ScheduleCategory::getColor).orElse(DEFAULT_COLOR))
            .orElse(DEFAULT_COLOR);
}
```

### 7-17, 7-18 팀 일정 수정/삭제

```java
@Transactional
public void update(Long memberId, Long scheduleId, TeamScheduleRequest request) {
    PartySchedule schedule = getParticipatingSchedule(memberId, scheduleId);
    if (request.rrule() != null) validateRrule(request.rrule());
    schedule.update(...); // PartySchedule.java에 update() 없으면 오늘 추가
}

@Transactional
public void delete(Long memberId, Long scheduleId) {
    PartySchedule schedule = getParticipatingSchedule(memberId, scheduleId);
    partyScheduleRepository.delete(schedule);
}

private PartySchedule getParticipatingSchedule(Long memberId, Long scheduleId) {
    PartySchedule schedule = partyScheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PARTY_SCHEDULE_NOT_FOUND));
    if (!projectMemberRepository.existsByProjectIdAndMemberIdAndLeftAtIsNull(schedule.getProject().getId(), memberId)) {
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    return schedule;
}
```
> 스펙상 수정/삭제는 "프로젝트 참여 여부"만 확인하고 리더 제한이 없습니다 — 팀원 누구나 가능하다는 뜻이니 리더 검증을 넣지 마세요(6-4, 7-16과 다른 점).

### 7-20 참석/불참 변경

```java
@Transactional
public void updateAttendance(Long memberId, Long scheduleId, boolean attending) {
    PartySchedule schedule = getParticipatingSchedule(memberId, scheduleId);
    PartyScheduleAttendance attendance = attendanceRepository
            .findById(new PartyScheduleAttendance.Pk(scheduleId, memberId))
            .orElseThrow(() -> new BusinessException(ErrorCode.PARTY_SCHEDULE_NOT_FOUND));
    attendance.update(attending); // 엔티티에 update() 없으면 추가

    ChatRoom groupRoom = chatRoomRepository.findByProjectId(schedule.getProject().getId()).orElseThrow();
    String message = "%s님이 '%s' 회의에 %s합니다".formatted(member.getNickname(), schedule.getTitle(), attending ? "참여" : "불참");
    chatMessageRepository.save(ChatMessage.builder()
            .chatRoomId(groupRoom.getId()).memberId(null).type(ChatMessageType.TEXT).message(message).build());
    // memberId=null → 시스템 메시지로 취급. 실시간 반영하려면 Day6에서 만든 SimpMessagingTemplate으로도 전송
    messagingTemplate.convertAndSend("/sub/chatrooms/" + groupRoom.getId(), ChatMessageItem.systemMessage(message));
}
```

---

## 예외 케이스

| 케이스 | 코드 |
|---|---|
| 프로젝트 참여자 아님 | 403 `FORBIDDEN` |
| 팀 일정 없음 | 404 `PARTY_SCHEDULE_NOT_FOUND` |
| 카테고리 없음/남의 것 | 404 `SCHEDULE_CATEGORY_NOT_FOUND` |
| 프로젝트 없음 | 404 `PROJECT_NOT_FOUND` |

---

## 테스트 체크리스트

- [ ] 색상 지정 후 7-15(팀 일정 조회) 응답에 지정한 색상이 반영되는지
- [ ] 지정 안 한 팀원은 기본 색상으로 나오는지
- [ ] 같은 프로젝트라도 팀원마다 다른 색상 지정 가능한지
- [ ] 팀 일정 수정/삭제가 리더 아닌 일반 팀원도 가능한지 (403 안 뜨는지 확인 — 여기가 실수하기 쉬운 지점)
- [ ] 불참으로 변경 → 그룹 채팅방에 시스템 메시지가 실제로 쌓이는지 + WebSocket 구독 중인 클라이언트에 실시간 전달되는지
- [ ] 불참 처리해도 회의 일정 자체는 삭제되지 않고 유지되는지

---

## 커밋/PR 가이드

- 브랜치: `feat/party-schedule-finish`
- 커밋: `feat: 프로젝트 색상 지정 API 구현 (7-19)` → `feat: 팀 일정 수정/삭제 API 구현 (7-17, 7-18)` → `feat: 회의 참석/불참 변경 API 구현 (7-20)`
- PR 제목: `feat: Schedule 도메인 완결 (API_SPEC 7장 전체)` — Schedule 20개 엔드포인트가 오늘로 다 끝나니 이렇게 마무리 PR로 묶어도 좋습니다

---

## 막힐 수 있는 포인트

- **7-17/7-18에 리더 검증을 실수로 넣지 않도록 주의**하세요 — 6-4(프로젝트 종료)나 7-16(직접등록)과 달리 이 둘은 팀원 누구나 가능합니다. 헷갈리기 쉬운 포인트입니다.
- **시스템 메시지의 `memberId=null` 처리**: `ChatMessage`의 `memberId`가 `Long`(nullable) 타입인지 확인하세요. 프론트에서 "시스템 메시지"로 구분해서 다르게 렌더링하려면, `type`을 별도로 `SYSTEM`으로 추가하는 게 더 명확할 수 있지만(현재 `ChatMessageType`은 `TEXT`/`IMAGE`만 있음), 오늘 스코프에서는 TEXT + memberId null 조합으로 충분히 표현 가능합니다.
