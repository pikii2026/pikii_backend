# Day 7 — 반복 일정 / 일정 수정삭제 / 회의 조율 개설 (7-7~7-9, 7-10)

## 오늘의 목표

| 엔드포인트 | Method/URL | DoD |
|---|---|---|
| 7-7 개인 반복 일정 생성 | `POST /users/me/schedules/recurring` | 201, RRULE 문법 검증 |
| 7-8 개인 일정 수정 | `PATCH /users/me/schedules/{scheduleId}` | 204 |
| 7-9 개인 일정 삭제 | `DELETE /users/me/schedules/{scheduleId}` | 204 |
| 7-10 회의 조율 개설 | `POST /projects/{projectId}/meeting-polls` | 201, 슬롯 자동 생성 |

**DoD**: 오늘부터 스프린트에서 가장 복잡한 도메인(회의 조율)이 시작됩니다. 7-10은 "슬롯을 30분 단위로 쪼개서 자동 생성"하는 로직이 핵심입니다.

---

## ⚠️ 오늘 추가해야 하는 의존성: ical4j

RRULE 문법 검증을 위해 `build.gradle`에 추가:
```groovy
implementation 'org.mnode.ical4j:ical4j:3.2.19'
```
직접 정규식으로 RRULE을 파싱하려고 하지 마세요 — RFC 5545는 생각보다 복잡합니다(BYDAY, BYMONTHDAY, UNTIL, COUNT 조합 등). ical4j의 `Recur` 클래스가 파싱 실패 시 예외를 던지므로 그걸 잡아서 `INVALID_RRULE`로 변환하면 됩니다.

```java
private void validateRrule(String rrule) {
    try {
        new Recur<>(rrule); // ical4j
    } catch (Exception e) {
        throw new BusinessException(ErrorCode.INVALID_RRULE);
    }
}
```

---

## 사전 확인 사항

- `MemberSchedule.isRecurring()` 이미 존재.
- `MeetingPollSlot(MeetingPoll poll, LocalDateTime startAt, LocalDateTime endAt)` 생성자 이미 존재.
- `MeetingPollMember(Long pollId, Long memberId)` 생성자 이미 존재 (복합키).
- 7-10은 **프로젝트장만** 호출 가능 — Day2에서 만든 리더 검증 로직(`getLeaderOwnedProject` 류)을 재사용하세요.

---

## 구현 순서

### 7-7 개인 반복 일정 생성

```java
@Transactional
public Long createRecurring(Long memberId, RecurringScheduleRequest request) {
    if (request.startDate().isAfter(request.endDate())) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
    if (!request.startTime().isBefore(request.endTime())) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
    validateRrule(request.rrule());
    ScheduleCategory category = request.categoryId() != null ? getOwnedCategory(memberId, request.categoryId()) : null;
    MemberSchedule schedule = new MemberSchedule(member, request.startDate(), request.endDate(),
            request.startTime(), request.endTime(), request.rrule(), null, request.title(), request.content(), category);
    return memberScheduleRepository.save(schedule).getId();
}
```

### 7-8, 7-9 수정/삭제

```java
@Transactional
public void update(Long memberId, Long scheduleId, ScheduleUpdateRequest request) {
    MemberSchedule schedule = getOwnedSchedule(memberId, scheduleId);
    if (request.rrule() != null) validateRrule(request.rrule());
    schedule.update(...); // MemberSchedule.java에 update() 메서드가 없으면 오늘 추가 (Setter 금지 규칙)
}

@Transactional
public void delete(Long memberId, Long scheduleId) {
    MemberSchedule schedule = getOwnedSchedule(memberId, scheduleId);
    memberScheduleRepository.delete(schedule);
}
```
> 단발↔반복 전환은 지원하지 않는다는 스펙 문구가 있으니, `update()`에서 종류(단발/반복) 자체를 바꾸는 검증은 필요 없습니다(그냥 필드값만 갱신).

### 7-10 회의 조율 개설 — 오늘의 핵심

**슬롯 생성 로직**: 탐색기간(`rangeStart`~`rangeEnd`) × 탐색시간대(`dayStart`~`dayEnd`)를 30분 단위로 쪼갭니다.

```java
@Transactional
public MeetingPollCreateResponse open(Long memberId, Long projectId, MeetingPollCreateRequest request) {
    Project project = getLeaderOwnedProject(memberId, projectId); // Day2 로직 재사용
    if (meetingPollRepository.existsByProjectIdAndStatus(projectId, MeetingPollStatus.COLLECTING)) {
        throw new BusinessException(ErrorCode.POLL_ALREADY_ACTIVE);
    }

    LocalDateTime deadline = LocalDateTime.now().plusHours(request.deadlineHours() != null ? request.deadlineHours() : 12);
    MeetingPoll poll = new MeetingPoll(project, leaderMember, request.title(), request.durationMin(),
            request.rangeStart(), request.rangeEnd(), request.dayStart(), request.dayEnd(), deadline);
    meetingPollRepository.save(poll);

    List<MeetingPollSlot> slots = generateSlots(poll, request);
    meetingPollSlotRepository.saveAll(slots);

    List<ProjectMember> participants = (request.memberIds() != null)
            ? projectMemberRepository.findAllByProjectIdAndLeftAtIsNull(projectId).stream()
                .filter(pm -> request.memberIds().contains(pm.getMember().getId())).toList()
            : projectMemberRepository.findAllByProjectIdAndLeftAtIsNull(projectId);
    participants.forEach(pm -> meetingPollMemberRepository.save(new MeetingPollMember(poll.getId(), pm.getMember().getId())));

    // TODO: 그룹 채팅방 공지 + 팀원 알림 발송 (Day9~10 Notification 완성 후 연동)

    return new MeetingPollCreateResponse(poll.getId(), poll.getStatus().name(), deadline,
            participants.size(), 0, slots.size());
}

private List<MeetingPollSlot> generateSlots(MeetingPoll poll, MeetingPollCreateRequest request) {
    List<MeetingPollSlot> slots = new ArrayList<>();
    for (LocalDate date = request.rangeStart(); !date.isAfter(request.rangeEnd()); date = date.plusDays(1)) {
        LocalDateTime cursor = LocalDateTime.of(date, request.dayStart());
        LocalDateTime dayEnd = LocalDateTime.of(date, request.dayEnd());
        while (!cursor.plusMinutes(30).isAfter(dayEnd)) {
            slots.add(new MeetingPollSlot(poll, cursor, cursor.plusMinutes(30)));
            cursor = cursor.plusMinutes(30);
        }
    }
    return slots;
}
```
> `MeetingPollRepository`에 `existsByProjectIdAndStatus(Long, MeetingPollStatus)` 쿼리 메서드 추가 필요.

---

## 예외 케이스

| 케이스 | 코드 |
|---|---|
| RRULE 문법 오류 | 400 `INVALID_RRULE` |
| startDate > endDate 등 | 400 `VALIDATION_FAILED` |
| 본인 일정 아님 | 403 `FORBIDDEN` |
| 리더 아닌데 조율 개설 | 403 `FORBIDDEN` |
| 이미 COLLECTING 상태 조율 존재 | 409 `POLL_ALREADY_ACTIVE` |

---

## 테스트 체크리스트

- [ ] `FREQ=WEEKLY;BYDAY=MO,WE` 같은 정상 RRULE로 생성 → 201
- [ ] `FREQ=주말마다` 같은 이상한 문자열로 생성 시도 → 400 `INVALID_RRULE`
- [ ] 반복 일정 수정/삭제 정상 동작
- [ ] 회의 조율 개설 → `slotCount`가 예상한 개수와 일치하는지 직접 계산해서 검증 (예: 5일 × 09:00~22:00(13시간=26슬롯) = 130개)
- [ ] 이미 COLLECTING 상태인 조율이 있는 프로젝트에서 재개설 시도 → 409
- [ ] 리더 아닌 팀원이 개설 시도 → 403
- [ ] `memberIds`를 일부만 지정했을 때 해당 인원만 `MeetingPollMember`로 등록되는지

---

## 커밋/PR 가이드

- 브랜치: `feat/schedule-recurring` + `feat/meeting-poll-open`
- 커밋: `feat: 개인 반복 일정 생성/수정/삭제 API 구현 (7-7~7-9)` → `feat: 회의 조율 개설 API 구현 (7-10)`

---

## 막힐 수 있는 포인트

- **슬롯 개수가 예상과 다르게 나옴**: 경계값 처리(마지막 슬롯이 `dayEnd`를 넘지 않는지)를 꼼꼼히 확인하세요. 위 코드의 `while (!cursor.plusMinutes(30).isAfter(dayEnd))` 조건이 핵심입니다.
- **`durationMin`(회의 소요시간)과 슬롯 단위(30분)는 다른 개념**입니다. 슬롯 자체는 항상 30분 단위로 쪼개고, `durationMin`은 나중에 확정(7-13) 단계에서 "이 슬롯부터 duration만큼"으로 해석하시면 됩니다. 오늘은 슬롯 생성에만 집중하세요.
- **ical4j 버전 호환성**: Java 17 환경에서 3.2.x 버전이 안정적입니다. 의존성 추가 후 빌드 에러 나면 버전을 조정하세요.
