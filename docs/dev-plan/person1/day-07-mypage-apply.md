# Day 7 — 마이페이지 지원 관련 (4-4 ~ 4-8)

## ⚠️ 오늘 두 번째로 중요한 결합 지점이 나옵니다 (4-8)

4-8(지원자 수락/거절) 스펙을 잘 보면 이런 문장이 있습니다:

> **"이미 Project가 생성된 공고라면**, 수락 즉시 ProjectMember로 등록하고 그룹 채팅방에 자동 초대한다"

이건 인원1이 **인원2의 도메인(Project, ChatRoom)을 직접 호출해야 하는 코드**라는 뜻입니다. Day11에서 정리할 결합 지점이 하나 더 늘어난 셈이니, 오늘 이 부분을 구현할 때 **인원2에게 `ProjectRepository.findByRecruitId(Long)` 같은 조회 메서드가 있는지, 팀원 등록용 메서드 이름이 뭔지 미리 한 번 확인**하고 진행하세요. (인원2는 이 시점에 Project 도메인을 이미 완성해뒀을 겁니다 — 인원2 Day1~2 참고)

## 오늘의 목표

| 엔드포인트 | Method/URL | DoD |
|---|---|---|
| 4-4 지원 현황 조회 | `GET /users/me/applies` | Pagination 정상 |
| 4-5 작성한 공고 조회 | `GET /users/me/recruits` | Pagination 정상 |
| 4-6 작성한 댓글 조회 | `GET /users/me/comments` | Pagination, 삭제된 것 제외 |
| 4-7 지원자 목록 조회 | `GET /recruits/{recruitId}/applicants` | 작성자만 조회 가능 |
| 4-8 지원자 수락/거절 | `PATCH /applies/{applyId}/status` | 204, 정원 체크, **Project 있으면 자동 합류** |

---

## 사전 확인 사항

- `Apply.accept()`, `Apply.reject()` 메서드는 이미 있습니다.
- **오늘 새로 추가해야 하는 것**: `Recruit`에 `increaseCurrentCount()` 메서드 (currently 없음 — `close()`, `openAdditional()`, `softDelete()`만 있음). 정원 초과 체크에도 이 카운트가 필요합니다.
- **오늘 새로 추가해야 하는 ErrorCode**: `RECRUIT_FULL` — 현재 `ErrorCode.java`에 없습니다. `docs/ERROR_CODE.md`와 `ErrorCode.java`에 **먼저** 추가하세요(팀 컨벤션: 문서 먼저, 코드 나중).
  ```java
  RECRUIT_FULL(HttpStatus.CONFLICT, "모집 정원이 마감되었습니다."),
  ```

---

## 구현 순서

### 4-4, 4-5, 4-6 — 조회 3종 (묶어서 처리)

전부 "로그인 사용자 기준으로 목록 조회 + Pagination" 패턴이라 빠르게 끝낼 수 있습니다.

- 4-4: `applyRepository.findByMemberId(memberId, pageable)`
- 4-5: `recruitRepository.findByMemberIdAndDeletedAtIsNull(memberId, pageable)`
- 4-6: `commentRepository.findByMemberIdAndDeletedAtIsNull(memberId, pageable)` — **응답에 `recruitTitle`, `recruitStatus`도 포함**해야 하므로 Comment→Recruit join이 필요합니다. 삭제된 공고에 달린 댓글은 제외.

셋 다 `PageResponse.from(page)`로 감싸서 반환 (기존 컨벤션 그대로).

### 4-7 지원자 목록 조회

```java
public PageResponse<ApplicantResponse> getApplicants(Long memberId, Long recruitId, Pageable pageable) {
    Recruit recruit = recruitRepository.findById(recruitId)
            .orElseThrow(() -> new BusinessException(ErrorCode.RECRUIT_NOT_FOUND));
    if (!recruit.getMember().getId().equals(memberId)) {
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    Page<Apply> applies = applyRepository.findByRecruitId(recruitId, pageable);
    return PageResponse.from(applies.map(ApplicantResponse::from));
}
```

### 4-8 지원자 수락/거절 — 오늘의 핵심

```java
@Transactional
public void updateStatus(Long memberId, Long applyId, ApplyStatusUpdateRequest request) {
    Apply apply = applyRepository.findById(applyId)
            .orElseThrow(() -> new BusinessException(ErrorCode.APPLY_NOT_FOUND));
    Recruit recruit = apply.getRecruit();
    if (!recruit.getMember().getId().equals(memberId)) {
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    if (request.status() == ApplyStatus.ACCEPTED) {
        if (recruit.getAvailableSlots() <= 0) {
            throw new BusinessException(ErrorCode.RECRUIT_FULL);
        }
        apply.accept();
        recruit.increaseCurrentCount(); // Recruit.java에 오늘 새로 추가

        // 결합 지점: 이미 Project가 생성된 공고인지 확인 후, 있으면 자동 합류
        projectRepository.findByRecruitId(recruit.getId()).ifPresent(project -> {
            projectMemberRepository.save(new ProjectMember(project, apply.getMember(), false));
            ChatRoom groupChatRoom = chatRoomRepository.findByProjectId(project.getId())
                    .orElseThrow(() -> new IllegalStateException("GROUP 채팅방이 없는 프로젝트입니다."));
            chatRoomMemberRepository.save(new ChatRoomMember(groupChatRoom, apply.getMember()));
        });
    } else {
        apply.reject();
    }
    // TODO: 지원자에게 알림 발송 (Day9 Notification 완성 후 연결)
}
```

> **주의**: `ProjectRepository.findByRecruitId(Long)`, `ChatRoomRepository.findByProjectId(Long)` 같은 메서드가 실제로 그 이름/시그니처로 존재하는지는 인원2의 구현에 달려있습니다. 오늘 이 부분 짤 때 **Slack/톡으로 인원2한테 정확한 메서드명을 물어보고 맞춰서 쓰세요.** 없으면 인원2에게 추가해달라고 요청하거나, 직접 추가해도 됩니다(같은 패키지 규칙만 지키면 누가 만들어도 무방).

Controller: `PATCH /applies/{applyId}/status`

---

## 예외 케이스

| 케이스 | 코드 |
|---|---|
| 작성자 아닌데 지원자 목록/수락·거절 시도 | 403 `FORBIDDEN` |
| 정원 초과 상태에서 수락 시도 | 409 `RECRUIT_FULL` (오늘 신규 추가) |

---

## 테스트 체크리스트

- [ ] 지원 현황(4-4), 작성한 공고(4-5), 작성한 댓글(4-6) 각각 정상 조회 + Pagination 필드 확인
- [ ] 삭제된 공고에 달았던 댓글이 4-6 목록에서 빠지는지 확인
- [ ] 작성자 본인이 지원자 목록(4-7) 조회 → 정상
- [ ] 작성자 아닌 사람이 지원자 목록 조회 시도 → 403
- [ ] 지원자 수락 → Apply.status가 ACCEPTED로 바뀌고 Recruit.currentCount +1
- [ ] 정원이 꽉 찬 상태에서 추가 수락 시도 → 409 `RECRUIT_FULL`
- [ ] **(통합 확인, 인원2 코드 필요)** 이미 Project가 있는 공고에서 추가 지원자를 수락하면 ProjectMember + ChatRoomMember로 자동 등록되는지 — 이건 인원2의 Project 생성(6-1)이 먼저 되어 있어야 테스트 가능하므로, 안 되면 Day11로 미뤄도 됩니다.

---

## 커밋/PR 가이드

- 브랜치: `feat/mypage-apply`
- 커밋: `feat: 마이페이지 조회 API 구현 (4-4~4-6)` → `feat: 지원자 목록 조회 API 구현 (4-7)` → `feat: 지원자 수락/거절 API 구현 (4-8)`
- **`ErrorCode.java`에 `RECRUIT_FULL` 추가하는 커밋은 별도로 분리**: `docs: ERROR_CODE.md에 RECRUIT_FULL 추가` 커밋을 먼저 만들고, 그 다음 기능 커밋을 올리세요 (팀 컨벤션: 문서 먼저 고치는 습관).

---

## 막힐 수 있는 포인트

- **오늘의 최대 리스크는 4-8의 Project/ChatRoom 연동 부분**입니다. 인원2의 관련 코드가 아직 없거나 메서드명이 다르면 컴파일이 안 될 수 있어요. 이 경우 **일단 해당 블록을 통째로 주석 처리하고 `// TODO: Day11에 인원2 코드와 연동`으로 남겨둔 채 나머지(수락/거절 기본 로직)부터 먼저 커밋**하세요. 완벽하게 붙이는 건 Day11 통합 테스트에서 해도 늦지 않습니다.
- **`increaseCurrentCount()`를 Recruit에 추가할 때 decrease도 같이 만들어두면 좋습니다** (나중에 인원2가 프로젝트 나가기/퇴출 로직에서 `decreaseCurrentCount()`가 필요합니다 — 인원2 Day2 가이드에도 이 내용이 있습니다).
