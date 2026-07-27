# Day 10 — Feedback (4-9 ~ 4-12)

## 오늘의 목표

| 엔드포인트 | Method/URL | DoD |
|---|---|---|
| 4-9 평가 대상 팀원 조회 | `GET /feedbacks/projects/{projectId}/members` | 종료 시점 팀원만, 본인 제외 |
| 4-10 상호평가 작성 | `POST /feedbacks` | 201, 중복평가 방지 |
| 4-11 AI 피드백 목록 조회 | `GET /feedbacks` | evaluatedCount/requiredCount 집계 |
| 4-12 AI 피드백 상세 조회 | `GET /feedbacks/ai/{projectId}` | 최소 인원 미달 시 에러 |

> **좋은 소식**: `FeedbackRepository`, `ProjectMemberRepository`에 오늘 필요한 쿼리 메서드(`findAllByProjectIdAndRevieweeId`, `existsByProjectIdAndReviewerIdAndRevieweeId`, `countByProjectIdAndRevieweeId`, `findAllByProjectIdAndLeftAtIsNull` 등)가 **이미 다 만들어져 있습니다.** 오늘은 이 쿼리들을 조합해서 비즈니스 로직만 짜면 됩니다.

---

## 사전 확인 사항

- 이 도메인은 **Project가 END 상태여야만 동작을 테스트할 수 있습니다.** 인원2가 Project 종료(6-4) API를 완성해뒀는지, 안 됐으면 MySQL에서 직접 `UPDATE project SET status='END' WHERE id=?`로 테스트하세요.
- 실제 AI 요약(4-12의 `strengthSummary`, `weaknessSummary`, `keywords`)은 AI 미연동이면 **목업으로 구현**하세요 (다른 AI 기능들과 동일한 패턴).
- 배치(3일 경과 시 AI 피드백 자동 생성)는 오늘 스코프에서는 **생략하거나 아주 단순하게**만 구현하는 걸 추천합니다 — 아래 "AI 피드백 생성 트리거" 참고.

---

## 구현 순서

### 4-9 평가 대상 팀원 조회

```java
public FeedbackTargetResponse getEvaluationTargets(Long memberId, Long projectId) {
    Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
    if (!projectMemberRepository.existsByProjectIdAndMemberIdAndLeftAtIsNull(projectId, memberId)) {
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    if (!project.isEnded()) {  // Project.java에 이미 있는 메서드
        throw new BusinessException(ErrorCode.PROJECT_NOT_ENDED);
    }
    LocalDateTime deadline = project.getUpdatedAt().plusDays(3); // 종료 시각 + 3일. Project에 종료시각을 별도 필드로 안 쓴다면 updatedAt 활용
    if (LocalDateTime.now().isAfter(deadline)) {
        throw new BusinessException(ErrorCode.EVALUATION_PERIOD_EXPIRED);
    }
    List<ProjectMember> members = projectMemberRepository.findAllByProjectIdAndLeftAtIsNull(projectId).stream()
            .filter(pm -> !pm.getMember().getId().equals(memberId))
            .toList();
    List<TargetMember> targets = members.stream()
            .map(pm -> new TargetMember(pm.getMember().getId(), pm.getMember().getNickname(),
                    feedbackRepository.existsByProjectIdAndReviewerIdAndRevieweeId(projectId, memberId, pm.getMember().getId())))
            .toList();
    return new FeedbackTargetResponse(projectId, deadline, targets);
}
```

### 4-10 상호평가 작성

```java
@Transactional
public void write(Long memberId, FeedbackCreateRequest request) {
    Project project = projectRepository.findById(request.projectId())
            .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
    if (!project.isEnded()) throw new BusinessException(ErrorCode.PROJECT_NOT_ENDED);
    // 4-9와 동일한 마감(3일) 체크 재사용
    if (feedbackRepository.existsByProjectIdAndReviewerIdAndRevieweeId(
            request.projectId(), memberId, request.revieweeId())) {
        throw new BusinessException(ErrorCode.ALREADY_EVALUATED);
    }
    Feedback feedback = Feedback.builder()
            .project(project).reviewer(reviewer).reviewee(reviewee)
            .commitScore(request.scores().responsibility())
            .commScore(request.scores().communication())
            .deadlineScore(request.scores().deadline())
            .cooperateScore(request.scores().cooperation())
            .contributeScore(request.scores().contribution())
            .strengthText(request.strength()).weaknessText(request.weakness())
            .build();
    feedbackRepository.save(feedback);
    checkAndGenerateAiFeedback(project, request.revieweeId()); // 아래 "조기완료 트리거" 참고
}
```

### 4-11, 4-12 — AI 피드백 조회

**최소 평가 인원 계산 로직** (문서 표 그대로 코드화):
```java
private int requiredEvaluatorCount(int teamSize) {
    if (teamSize < 3) return Integer.MAX_VALUE; // 2인 이하는 생성 대상 아님
    return (int) Math.ceil(teamSize / 2.0);
}
```

4-11(목록)은 내가 참여했던 END 프로젝트를 순회하며 `countByProjectIdAndRevieweeId(projectId, myMemberId)`로 `evaluatedCount`를 구하고, `requiredEvaluatorCount`와 비교해 `isAiFeedbackAvailable` 계산.

4-12(상세)는 `AIFeedbackRepository.findByProjectIdAndMemberId(projectId, memberId)`로 조회 — 없으면 404 `EVALUATION_NOT_FOUND`, 있어도 최소 인원 미달이면 409 `INSUFFICIENT_EVALUATION`.

### AI 피드백 생성 트리거 — 오늘 스코프 제안

정식 스펙은 "조기 완료 시 이벤트로 즉시 생성" + "3일 경과 시 배치로 생성" 두 가지인데, 12일 스코프에서는 **조기 완료(이벤트) 트리거만 구현**하는 걸 권장합니다. 3일 경과 배치(`@Scheduled`)는 실제 운영에선 필요하지만, 데모/제출 시점엔 "평가 마감 전에 몰아서 테스트"하는 시나리오가 대부분이라 우선순위가 낮습니다.

```java
private void checkAndGenerateAiFeedback(Project project, Long revieweeId) {
    int teamSize = projectMemberRepository.findAllByProjectIdAndLeftAtIsNull(project.getId()).size();
    long evaluatedCount = feedbackRepository.countByProjectIdAndRevieweeId(project.getId(), revieweeId);
    if (evaluatedCount >= teamSize - 1 && teamSize >= 3) { // 본인 제외 전원이 평가 완료
        generateAiFeedbackMock(project, revieweeId);
    }
}

private void generateAiFeedbackMock(Project project, Long revieweeId) {
    if (aiFeedbackRepository.findByProjectIdAndMemberId(project.getId(), revieweeId).isPresent()) return; // 이미 생성됨
    aiFeedbackRepository.save(AIFeedback.builder()
            .project(project).member(revieweeMember)
            .strength("(AI 생성 목업) 팀원들의 평가를 종합하면 책임감과 협업 능력이 뛰어납니다.")
            .weakness("(AI 생성 목업) 마감 기한 관리에 조금 더 신경쓰면 좋겠습니다.")
            .build());
}
```
시간이 남으면 `@Scheduled(cron = "...")`로 매일 자정에 "종료+3일 지난 프로젝트"를 찾아 배치 생성하는 로직을 Day11~12에 추가하세요.

---

## 예외 케이스

| 케이스 | 코드 |
|---|---|
| 프로젝트 참여자 아님 | 403 `FORBIDDEN` |
| 아직 종료 안 된 프로젝트 | 409 `PROJECT_NOT_ENDED` |
| 평가 기간(3일) 지남 | 409 `EVALUATION_PERIOD_EXPIRED` |
| 이미 평가한 팀원 재평가 | 400 `ALREADY_EVALUATED` |
| AI 피드백 아직 없음 | 404 `EVALUATION_NOT_FOUND` |
| 평가 기간 안 끝남(4-12) | 409 `EVALUATION_NOT_COMPLETE` |
| 최소 인원 미달 | 409 `INSUFFICIENT_EVALUATION` |

---

## 테스트 체크리스트

- [ ] END 상태 프로젝트(3인 이상, SQL로 직접 세팅)에서 평가 대상 조회 → 본인 제외 팀원만 나옴
- [ ] 상호평가 작성 → 재작성 시도 → 400 `ALREADY_EVALUATED`
- [ ] 아직 IN_PROGRESS인 프로젝트에서 평가 시도 → 409 `PROJECT_NOT_ENDED`
- [ ] 3인 팀에서 본인 제외 2명이 모두 어떤 팀원 A를 평가 완료 → A의 AI 피드백이 자동 생성되는지(DB 직접 확인)
- [ ] 2인 팀에서는 AI 피드백이 아예 생성 대상이 아닌지 확인
- [ ] AI 피드백 상세조회 — 생성 안 된 상태에서 조회 시 404

---

## 커밋/PR 가이드

- 브랜치: `feat/feedback`
- 커밋: `feat: 평가 대상 팀원 조회 API 구현 (4-9)` → `feat: 상호평가 작성 API 구현 (4-10)` → `feat: AI 피드백 조회 API 구현 (4-11, 4-12)`

---

## 막힐 수 있는 포인트

- **"평가 기간 3일"의 기산점**: `Project.EndCheckedAt`이 아니라 **실제 종료(`Status=END`)된 시각**부터 3일입니다. `Project` 엔티티에 종료 시각을 별도로 기록하는 필드가 없다면 `updatedAt`(END로 바뀐 시점)을 임시로 써도 되지만, 정확하려면 `Project`에 `endedAt` 필드를 추가하는 게 깔끔합니다 — 인원2가 Project 도메인 담당이니, 필요하면 요청하거나 직접 추가하세요(간단한 컬럼 추가라 충돌 위험은 낮습니다).
- **`teamSize`를 어느 시점 기준으로 셀지**: 스펙상 "프로젝트 **종료 시점**에 참여 중이던 팀원" 기준이라, 종료 이후에 발생하는 변화(사실상 없겠지만)는 무시하고 `LeftAt IS NULL`인 현재 상태를 그대로 종료 시점 기준으로 간주해도 무방합니다(종료 후엔 팀원 변경이 불가능하다는 전제).
