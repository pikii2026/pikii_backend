# Day 8 — 회의 조율 응답/확정 (7-11, 7-12, 7-13)

## 오늘의 목표

| 엔드포인트 | Method/URL | DoD |
|---|---|---|
| 7-11 응답 화면 조회 | `GET /meeting-polls/{pollId}` | 슬롯 + 캘린더 프리필 |
| 7-12 응답 제출 | `POST /meeting-polls/{pollId}/responses` | 200 |
| 7-13 최종 일정 확정 | `PATCH /meeting-polls/{pollId}/confirm` | 200, PartySchedule 생성 |

**DoD**: 오늘로 회의 조율의 핵심 흐름(개설→응답→확정)이 전부 완성됩니다.

---

## 사전 확인 사항

- 어제 추가한 ical4j로 **RRULE 전개**까지 오늘 실제로 사용합니다. `Recur.getDates(seed, periodStart, periodEnd)` 형태의 API로 특정 기간 내 반복 일정의 실제 발생 날짜들을 뽑아낼 수 있습니다(ical4j 버전에 따라 API가 조금씩 다르니 공식 문서/예제 참고).
- `MeetingPollAvailability(Long slotId, Long memberId, boolean available)` 생성자 이미 존재.
- `MeetingPollMember.markResponded()` 이미 존재.
- `PartySchedule(Project project, LocalDate startDate, LocalDate endDate, ...)` 생성자 이미 존재.

---

## 구현 순서

### 7-11 응답 화면 조회 — 캘린더 프리필이 오늘의 핵심 난이도

```java
public MeetingPollDetailResponse getDetail(Long memberId, Long pollId) {
    MeetingPoll poll = meetingPollRepository.findById(pollId)
            .orElseThrow(() -> new BusinessException(ErrorCode.POLL_NOT_FOUND));
    List<MeetingPollSlot> slots = meetingPollSlotRepository.findAllByPollId(pollId);

    // 내 개인 캘린더를 조율 기간(rangeStart~rangeEnd) 내에서 전개
    List<MemberSchedule> mySchedules = memberScheduleRepository
            .findAllByMemberIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(memberId, poll.getRangeEnd(), poll.getRangeStart());
    Set<LocalDateTime> busyStarts = expandToBusyRanges(mySchedules, poll.getRangeStart(), poll.getRangeEnd()); // 아래 참고

    List<SlotItem> slotItems = slots.stream().map(slot -> {
        boolean myResponseExists = availabilityRepository.existsById(new MeetingPollAvailability.Pk(slot.getId(), memberId));
        boolean prefilled = !myResponseExists && overlapsAnyBusyRange(slot, busyStarts);
        boolean myAvailable = myResponseExists
                ? availabilityRepository.findById(new MeetingPollAvailability.Pk(slot.getId(), memberId)).get().isAvailable()
                : !prefilled; // 기본값 true, 캘린더 겹치면 false
        long availableCount = availabilityRepository.countBySlotIdAndAvailableTrue(slot.getId());
        long unansweredCount = pollMemberCount - availabilityRepository.countBySlotId(slot.getId());
        return new SlotItem(slot.getId(), slot.getStartAt(), slot.getEndAt(), myAvailable, prefilled, availableCount, unansweredCount);
    }).toList();

    return new MeetingPollDetailResponse(poll, slotItems);
}

/** 반복 일정(RRULE 포함)을 실제 겹치는 시간대 목록으로 펼치는 헬퍼 */
private boolean overlapsAnyBusyRange(MeetingPollSlot slot, List<MemberSchedule> schedules) {
    for (MemberSchedule s : schedules) {
        if (!s.isRecurring()) {
            if (isSameDayAndOverlapping(slot, s.getStartDate(), s.getStartTime(), s.getEndTime())) return true;
        } else {
            // ical4j Recur로 slot 날짜가 반복 규칙에 해당하는 발생일인지 확인
            Recur<LocalDate> recur = new Recur<>(s.getRrule());
            // ... slot.getStartAt().toLocalDate()가 recur의 발생일에 포함되는지 + 시간대 겹침 확인
        }
    }
    return false;
}
```
> 이 부분이 오늘 가장 시간이 걸릴 수 있는 지점입니다. **완벽한 최적화보다 정확성 우선**으로, 슬롯 하나하나마다 내 일정과 겹치는지 단순 반복 비교해도 괜찮습니다(회의 조율 하나당 슬롯 수가 최대 수백 개 수준이라 성능 문제 없음).

### 7-12 응답 제출

```java
@Transactional
public MeetingPollResponseSubmitResponse submit(Long memberId, Long pollId, List<Long> unavailableSlotIds) {
    MeetingPoll poll = meetingPollRepository.findById(pollId).orElseThrow(() -> new BusinessException(ErrorCode.POLL_NOT_FOUND));
    if (!poll.isCollecting()) throw new BusinessException(ErrorCode.POLL_NOT_COLLECTING);

    List<MeetingPollSlot> allSlots = meetingPollSlotRepository.findAllByPollId(pollId);
    for (MeetingPollSlot slot : allSlots) {
        boolean available = !unavailableSlotIds.contains(slot.getId());
        availabilityRepository.deleteById(new MeetingPollAvailability.Pk(slot.getId(), memberId)); // 재제출 시 갱신
        availabilityRepository.save(new MeetingPollAvailability(slot.getId(), memberId, available));
    }
    MeetingPollMember pollMember = meetingPollMemberRepository.findById(new MeetingPollMember.Pk(pollId, memberId))
            .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
    pollMember.markResponded();

    long respondedCount = meetingPollMemberRepository.countByPollIdAndRespondedTrue(pollId);
    long totalMembers = meetingPollMemberRepository.countByPollId(pollId);
    if (respondedCount == totalMembers) {
        // TODO: 프로젝트장에게 '확정 가능' 알림 발송
    }
    return new MeetingPollResponseSubmitResponse(pollId, respondedCount, totalMembers);
}
```

### 7-13 최종 일정 확정

```java
@Transactional
public MeetingPollConfirmResponse confirm(Long memberId, Long pollId, Long slotId, boolean force) {
    MeetingPoll poll = getLeaderPoll(memberId, pollId); // 리더 검증
    if (!poll.isCollecting()) throw new BusinessException(ErrorCode.POLL_NOT_COLLECTING);
    MeetingPollSlot slot = meetingPollSlotRepository.findById(slotId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SLOT_NOT_FOUND));

    long totalMembers = meetingPollMemberRepository.countByPollId(pollId);
    long answeredForSlot = availabilityRepository.countBySlotId(slotId);
    if (!force && answeredForSlot < totalMembers) {
        throw new BusinessException(ErrorCode.UNANSWERED_EXISTS);
    }

    PartySchedule schedule = new PartySchedule(poll.getProject(), slot.getStartAt().toLocalDate(), slot.getStartAt().toLocalDate(),
            slot.getStartAt().toLocalTime(), slot.getEndAt().toLocalTime(), null, null, poll.getTitle(), null);
    partyScheduleRepository.save(schedule);
    poll.confirm(schedule); // MeetingPoll.java에 이미 있는 메서드

    // 전원 캘린더 반영: PartyScheduleAttendance를 팀원 전체에 기본 참석(true)으로 생성
    projectMemberRepository.findAllByProjectIdAndLeftAtIsNull(poll.getProject().getId())
            .forEach(pm -> attendanceRepository.save(new PartyScheduleAttendance(schedule.getId(), pm.getMember().getId(), true)));
    // TODO: 그룹 채팅방 공지 + 알림 발송

    return new MeetingPollConfirmResponse(pollId, poll.getStatus().name(), schedule.getId());
}
```

---

## 예외 케이스

| 케이스 | 코드 |
|---|---|
| 조율 없음 | 404 `POLL_NOT_FOUND` |
| 슬롯 없음 | 404 `SLOT_NOT_FOUND` |
| COLLECTING 아닌 상태에서 응답/확정 시도 | 409 `POLL_NOT_COLLECTING` |
| 미응답자 있는데 force 없이 확정 시도 | 409 `UNANSWERED_EXISTS` |
| 프로젝트 팀원 아님 | 403 `FORBIDDEN` |

---

## 테스트 체크리스트

- [ ] 개인 캘린더에 미리 일정을 넣어둔 뒤 응답화면 조회 → 겹치는 슬롯이 `prefilledByCalendar:true`, `myAvailable:false`로 나오는지
- [ ] 반복 일정(RRULE)도 프리필에 정확히 반영되는지 (이번 스프린트 최고 난이도 검증 포인트)
- [ ] 응답 제출 → 재조회 시 내 응답이 반영되어 있는지
- [ ] 전원 응답 완료 시 `respondedCount == totalMembers` 확인
- [ ] 미응답자 있는 슬롯 `force:false`로 확정 시도 → 409
- [ ] `force:true`로 확정 → 정상 진행, PartySchedule 생성 확인
- [ ] 확정 후 팀원 전원의 `PartyScheduleAttendance`가 기본 참석(true)으로 생성됐는지

---

## 커밋/PR 가이드

- 브랜치: `feat/meeting-poll-response`
- 커밋: `feat: 회의 조율 응답 화면/제출 API 구현 (7-11, 7-12)` → `feat: 회의 최종 확정 API 구현 (7-13)`

---

## 막힐 수 있는 포인트

- **RRULE 전개가 오늘 가장 큰 리스크**입니다. ical4j API가 버전마다 꽤 다르게 생겼으니(예전 버전은 `Recur.getDates(...)`가 `DateList` 반환, 최신 버전은 스트림 기반), 실제 추가한 버전의 공식 예제를 먼저 5분만 찾아보고 시작하세요. 시간이 너무 오래 걸리면 **"반복 일정은 프리필 생략, 단발 일정만 프리필"로 스코프를 줄이는 것도 현실적인 타협**입니다(단, 이 경우 실제 스펙과 다르다는 걸 문서에 남겨두세요).
- **응답 재제출 시 기존 데이터 갱신**: `deleteById` 후 `save`하는 방식 대신, 이미 있으면 `update`, 없으면 `insert`하는 방식(`findById` 후 분기)이 더 안전할 수 있습니다. 복합키 엔티티라 `save()`가 upsert처럼 동작하는지 미리 테스트해보세요.
