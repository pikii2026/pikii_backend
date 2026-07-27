# Day 2 — 프로젝트 관리 마무리 (6-3 ~ 6-9)

## 오늘의 목표

| 엔드포인트 | Method/URL | DoD |
|---|---|---|
| 6-3 팀원 조회 | `GET /projects/{projectId}/members` | Pagination |
| 6-4 프로젝트 종료 | `PATCH /projects/{projectId}/close` | 204 |
| 6-5 진행기간 연장 | `PATCH /projects/{projectId}/extend` | 200 |
| 6-6 프로젝트 나가기 | `DELETE /projects/{projectId}/members/me` | 204, 리더는 불가 |
| 6-7 팀원 퇴출 | `DELETE /projects/{projectId}/members/{memberId}` | 204, 리더만 가능 |
| 6-8 프로젝트장 위임 | `PATCH /projects/{projectId}/leader` | 204 |
| 6-9 프로젝트 상태 조회 | `GET /projects/{projectId}/status` | 200, 진행률 계산 |

---

## ⚠️ 오늘 미리 추가해야 하는 것: `Recruit.decreaseCurrentCount()`

6-6(나가기), 6-7(퇴출)에서 `Recruit.CurrentCount`를 감소시켜야 하는데, 이 메서드가 아직 `Recruit.java`에 없을 가능성이 높습니다(인원1이 이 메서드의 짝인 `increaseCurrentCount()`를 자기 스케줄상 나중에 추가할 예정이라 시점이 안 맞습니다). **오늘 직접 추가하세요**:

```java
// Recruit.java에 추가
public void decreaseCurrentCount() {
    this.currentCount = Math.max(0, this.currentCount - 1);
}

public void increaseCurrentCount() {
    this.currentCount++;
}
```
두 메서드를 같이 추가해두고, 인원1에게 "이미 추가해놨다"고 알려주세요 (중복 작업 방지).

---

## 사전 확인 사항

- `Project.end()`, `Project.extend(LocalDate)`, `Project.isEnded()`, `Project.markEndChecked()` 이미 존재.
- `ProjectMember.leave()`, `isActive()`, `delegateLeader()`, `promoteLeader()` 이미 존재.
- `ProjectMemberRepository`의 `findByProjectIdAndMemberIdAndLeftAtIsNull`, `findAllByProjectIdAndLeftAtIsNull`, `existsByProjectIdAndMemberIdAndLeftAtIsNull` 이미 존재.

---

## 구현 순서

### 6-4, 6-5 — 종료/연장

```java
@Transactional
public void close(Long memberId, Long projectId) {
    Project project = getLeaderOwnedProject(memberId, projectId);
    if (project.isEnded()) throw new BusinessException(ErrorCode.ALREADY_ENDED);
    project.end();
    // TODO: 팀원 전체에게 종료 알림 발송 — 인원1 Day9 완성 후 NotificationSender 연동
}

@Transactional
public ProjectExtendResponse extend(Long memberId, Long projectId, ProjectExtendRequest request) {
    Project project = getLeaderOwnedProject(memberId, projectId);
    if (project.isEnded()) throw new BusinessException(ErrorCode.ALREADY_ENDED);
    if (request.endDate().isBefore(project.getEndDate())) {
        throw new BusinessException(ErrorCode.VALIDATION_FAILED);
    }
    project.extend(request.endDate());
    return new ProjectExtendResponse(project.getId(), project.getEndDate(), project.getStatus().name());
}

private Project getLeaderOwnedProject(Long memberId, Long projectId) {
    Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
    ProjectMember pm = projectMemberRepository.findByProjectIdAndMemberIdAndLeftAtIsNull(projectId, memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
    if (!pm.isLeader()) throw new BusinessException(ErrorCode.FORBIDDEN);
    return project;
}
```

### 6-6 나가기 / 6-7 퇴출 (거의 동일 로직)

```java
@Transactional
public void leave(Long memberId, Long projectId) {
    ProjectMember pm = projectMemberRepository.findByProjectIdAndMemberIdAndLeftAtIsNull(projectId, memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
    if (pm.isLeader()) throw new BusinessException(ErrorCode.LEADER_CANNOT_LEAVE);
    leaveInternal(pm);
}

@Transactional
public void kick(Long memberId, Long projectId, Long targetMemberId) {
    if (memberId.equals(targetMemberId)) throw new BusinessException(ErrorCode.CANNOT_KICK_SELF);
    getLeaderOwnedProject(memberId, projectId); // 리더 검증 재사용
    ProjectMember target = projectMemberRepository.findByProjectIdAndMemberIdAndLeftAtIsNull(projectId, targetMemberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_MEMBER_NOT_FOUND));
    leaveInternal(target);
    // TODO: 퇴출된 팀원에게 알림 발송
}

private void leaveInternal(ProjectMember pm) {
    pm.leave();
    ChatRoom groupChatRoom = chatRoomRepository.findByProjectId(pm.getProject().getId())
            .orElseThrow(() -> new IllegalStateException("GROUP 채팅방 없음"));
    chatRoomMemberRepository.findByChatRoomIdAndMemberId(groupChatRoom.getId(), pm.getMember().getId())
            .ifPresent(chatRoomMemberRepository::delete);
    pm.getProject().getRecruit().decreaseCurrentCount();
}
```

### 6-8 위임

```java
@Transactional
public void delegateLeader(Long memberId, Long projectId, LeaderDelegateRequest request) {
    Project project = getLeaderOwnedProject(memberId, projectId);
    ProjectMember currentLeader = projectMemberRepository.findByProjectIdAndMemberIdAndLeftAtIsNull(projectId, memberId).orElseThrow();
    ProjectMember newLeader = projectMemberRepository.findByProjectIdAndMemberIdAndLeftAtIsNull(projectId, request.newLeaderId())
            .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_MEMBER_NOT_FOUND));
    currentLeader.delegateLeader();
    newLeader.promoteLeader();
}
```

### 6-3, 6-9 — 조회 2종

6-3은 `findAllByProjectIdAndLeftAtIsNull` + Pagination(수동으로 List를 잘라서 PageResponse 형태로 감싸거나, Repository 메서드를 Pageable 받는 버전으로 하나 추가).

6-9 진행률: `progressRate = (오늘 - startDate) / (endDate - startDate) * 100`으로 단순 계산.

---

## 예외 케이스

| 케이스 | 코드 |
|---|---|
| 리더 아닌데 종료/연장/퇴출/위임 시도 | 403 `FORBIDDEN` |
| 이미 종료된 프로젝트 재종료/연장 | 409 `ALREADY_ENDED` |
| 연장 날짜가 기존보다 이전 | 400 `VALIDATION_FAILED` |
| 리더가 나가기 시도 | 409 `LEADER_CANNOT_LEAVE` |
| 자기 자신 퇴출 시도 | 400 `CANNOT_KICK_SELF` |
| 퇴출 대상이 팀원 아님 | 404 `PROJECT_MEMBER_NOT_FOUND` |

---

## 테스트 체크리스트

- [ ] 팀원 목록 조회 정상
- [ ] 종료 → 상태 END로 변경, 재종료 시도 → 409
- [ ] 연장 → endDate 갱신, 과거 날짜로 연장 시도 → 400
- [ ] 일반 팀원 나가기 → ProjectMember.leftAt 기록 + Recruit.currentCount -1 + ChatRoomMember 삭제 확인
- [ ] 리더 나가기 시도 → 409 `LEADER_CANNOT_LEAVE`
- [ ] 리더가 팀원 퇴출 → 정상, 자기 자신 퇴출 시도 → 400
- [ ] 위임 후 새 리더가 종료/연장 권한 갖는지, 기존 리더는 이제 나가기 가능한지
- [ ] 상태 조회 시 progressRate가 0~100 사이로 정상 계산

---

## 커밋/PR 가이드

- 브랜치: `feat/project-manage`
- 커밋: `feat: 프로젝트 종료/연장 API 구현 (6-4, 6-5)` → `feat: 팀원 나가기/퇴출/위임 API 구현 (6-6~6-8)` → `feat: 팀원조회/상태조회 API 구현 (6-3, 6-9)`

---

## 막힐 수 있는 포인트

- **`decreaseCurrentCount()`를 오늘 먼저 추가하면서 인원1의 Day7 작업과 겹칠 수 있음** — 위에서 설명한 대로 오늘 미리 만들어두고 공유하면 됩니다. 나중에 인원1이 또 만들려고 하면 "이미 있다"고 알려주세요.
- **리더 위임 후 기존 리더가 즉시 나갈 수 있는지 확인**: `delegateLeader()`가 `isLeader`를 false로 바꾸는 메서드라면, 위임 직후 6-6(나가기) 호출 시 정상적으로 나갈 수 있어야 합니다. 이 흐름을 꼭 테스트하세요.
