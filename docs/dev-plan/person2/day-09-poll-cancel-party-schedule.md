# Day 9 — 조율 취소/재조율 + 팀 일정 조회/직접등록 (7-14, 7-15, 7-16)

## 오늘의 목표

| 엔드포인트 | Method/URL | DoD |
|---|---|---|
| 7-14 조율 취소/재조율 | `DELETE /meeting-polls/{pollId}` | 204 |
| 7-15 팀 일정 조회 | `GET /projects/{projectId}/schedules?year=&month=` | 200 |
| 7-16 팀 일정 직접 등록 | `POST /projects/{projectId}/schedules/single`, `/recurring` | 201 |

---

## 사전 확인 사항

- `MeetingPoll.cancel()` 메서드 이미 존재.
- 7-15는 7-5(개인 일정 조회, 인원1 담당이 아니라 어제까지 여러분이 만든 것)와 응답 구조가 동일합니다 — 로직을 최대한 재사용하세요.
- 7-16은 회의 조율 없이 프로젝트장이 팀 일정을 강제로 등록하는 예외 API입니다. **7-6/7-7(개인 일정 생성)과 로직이 거의 동일**하니 그 코드를 참고해서 `Member` 대신 `Project`를 대상으로 바꾸면 됩니다.

---

## 구현 순서

### 7-14 조율 취소/재조율

```java
@Transactional
public void cancel(Long memberId, Long pollId) {
    MeetingPoll poll = getLeaderPoll(memberId, pollId); // Day8에서 만든 헬퍼 재사용
    poll.cancel();
    if (poll.getSchedule() != null) {
        partyScheduleRepository.delete(poll.getSchedule());
        attendanceRepository.deleteAllByScheduleId(poll.getSchedule().getId());
        // 팀원 캘린더에서도 제거 — PartySchedule을 캘린더에 별도 테이블로 복제 저장하지 않고
        // "PartySchedule 자체가 곧 팀 캘린더"라는 구조라면 이 delete 한 줄로 충분합니다
    }
    // TODO: 팀원에게 취소 알림 발송
}
```
> 취소 후 프로젝트장이 다시 7-10을 호출하면 새 조율이 열립니다(재조율은 별도 API 없이 그냥 다시 개설하는 것).

### 7-15 팀 일정 조회

```java
public List<ScheduleItem> getTeamSchedules(Long memberId, Long projectId, int year, int month) {
    if (!projectMemberRepository.existsByProjectIdAndMemberIdAndLeftAtIsNull(projectId, memberId)) {
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    LocalDate rangeStart = LocalDate.of(year, month, 1);
    LocalDate rangeEnd = rangeStart.withDayOfMonth(rangeStart.lengthOfMonth());
    List<PartySchedule> schedules = partyScheduleRepository
            .findAllByProjectIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(projectId, rangeEnd, rangeStart);
    // 팀원이 지정한 ProjectScheduleCategory 색상 적용 (없으면 기본 색상)
    ProjectScheduleCategory myColor = projectScheduleCategoryRepository
            .findByProjectIdAndMemberId... // 아직 없으면 오늘 추가, 또는 Day10에서 7-19와 함께 처리
    return schedules.stream().map(s -> ScheduleItem.of(s, resolveColor(myColor))).toList();
}
```
> `PartyScheduleRepository`에 범위 조회 쿼리 메서드가 없으면 오늘 추가하세요(7-5에서 만든 `MemberScheduleRepository`의 쿼리와 동일한 패턴).

### 7-16 팀 일정 직접 등록

```java
@Transactional
public Long createTeamSchedule(Long memberId, Long projectId, TeamScheduleRequest request, boolean isRecurring) {
    getLeaderOwnedProject(memberId, projectId); // Day2 헬퍼 재사용 — 리더만 가능
    if (isRecurring) validateRrule(request.rrule());
    PartySchedule schedule = new PartySchedule(project, request.startDate(), request.endDate(),
            request.startTime(), request.endTime(), isRecurring ? request.rrule() : null, null,
            request.title(), request.content());
    partyScheduleRepository.save(schedule);
    // 팀원 전원에게 알림 발송 + 기본 참석(true)으로 PartyScheduleAttendance 생성 (7-13 확정 로직과 동일 패턴)
    projectMemberRepository.findAllByProjectIdAndLeftAtIsNull(projectId)
            .forEach(pm -> attendanceRepository.save(new PartyScheduleAttendance(schedule.getId(), pm.getMember().getId(), true)));
    return schedule.getId();
}
```
> 참석/불참(7-20)은 "조율로 확정한 회의와 동일하게 적용"된다는 스펙 문구대로, `PartyScheduleAttendance` 생성 로직을 7-13과 통일해서 재사용하는 게 깔끔합니다.

---

## 예외 케이스

| 케이스 | 코드 |
|---|---|
| 리더 아닌데 취소/직접등록 시도 | 403 `FORBIDDEN` |
| 조율 없음 | 404 `POLL_NOT_FOUND` |
| 팀 일정 RRULE 오류 | 400 `INVALID_RRULE` |
| 프로젝트 참여자 아닌데 조회 | 403 `FORBIDDEN` |

---

## 테스트 체크리스트

- [ ] 확정 전 조율 취소 → 상태 CANCELLED
- [ ] 확정된 조율 취소 → PartySchedule도 같이 삭제되고 팀원 캘린더 조회 시 사라지는지
- [ ] 취소 후 같은 프로젝트에서 재조율(7-10 재호출) 가능한지
- [ ] 팀 일정 조회(7-15)가 개인 일정 조회(7-5)와 동일한 응답 구조로 나오는지
- [ ] 팀 일정 직접 등록(리더) → 팀원 전원 캘린더에 반영되는지
- [ ] 리더 아닌 팀원이 직접등록 시도 → 403

---

## 커밋/PR 가이드

- 브랜치: `feat/party-schedule`
- 커밋: `feat: 회의 조율 취소/재조율 API 구현 (7-14)` → `feat: 팀 일정 조회/직접등록 API 구현 (7-15, 7-16)`

---

## 막힐 수 있는 포인트

- **`ProjectScheduleCategory`(개인별 색상 지정, 7-19)가 아직 안 만들어져 있어서 7-15에서 색상 로직이 애매할 수 있습니다.** 오늘은 일단 "지정된 색상 없으면 기본색"으로 단순 처리하고, 실제 지정 기능(7-19)은 내일(Day10)에 마저 만드세요.
- **조율 취소 시 캘린더 반영 제거 로직**: `PartySchedule`을 삭제하는 순간 7-15 조회 쿼리에서 자동으로 안 나오게 되니, 별도로 "캘린더에서 제거"하는 추가 작업은 사실 필요 없습니다(같은 테이블을 그대로 조회하는 구조라면). 이 구조가 맞는지 오늘 한번 점검하세요.
