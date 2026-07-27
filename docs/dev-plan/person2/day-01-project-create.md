# Day 1 — 프로젝트 생성 (6-1) + 상세조회 (6-2)

## 오늘의 목표

| 엔드포인트 | Method/URL | DoD |
|---|---|---|
| 6-1 프로젝트 생성 (그룹채팅 생성) | `POST /recruits/{recruitId}/project` | 201, Project+ChatRoom(GROUP)+ProjectMember 동시 생성 |
| 6-2 프로젝트 상세 조회 | `GET /projects/{projectId}` | 200 |

**DoD**: 이 스프린트에서 가장 중요한 엔드포인트입니다. 6-1이 곧 "공고→프로젝트 전환" + "채팅 도메인의 시작점"이라 이후 Chat/Schedule/Feedback 전부가 여기서 만들어지는 `Project`, `ChatRoom(GROUP)`을 전제로 동작합니다. 오늘 확실하게 만들어두세요.

---

## 사전 확인 사항

- **좋은 소식**: `ProjectRepository.findByRecruitId()`, `existsByRecruitId()`가 이미 구현되어 있습니다. `ChatRoomRepository.findByProjectId()`도 이미 있습니다.
- `Project` 생성자: `Project(Recruit recruit, String name, LocalDate startDate, LocalDate endDate)` — 이미 존재.
- `ProjectMember` 생성자: `ProjectMember(Project project, Member member, boolean isLeader)` — 이미 존재.
- `ChatRoom` 생성자: `ChatRoom(ChatRoomType type, Project project)` — 이미 존재.
- `ChatRoomMember` 생성자: `ChatRoomMember(ChatRoom chatRoom, Member member)` — 이미 존재.
- **의존 관계**: 이 API는 `Recruit`에 **ACCEPTED 상태의 Apply가 최소 1개** 있어야 동작합니다. 인원1이 아직 3-11(지원하기)/4-8(수락)을 못 끝냈을 수 있으니, **오늘은 MySQL에 직접 테스트 데이터를 심어서 진행하세요**:
  ```sql
  -- 1. 공고 하나 만들기 (또는 인원1이 이미 만든 공고 활용)
  -- 2. 지원 데이터를 ACCEPTED 상태로 직접 삽입
  INSERT INTO apply (recruit_id, member_id, message, status, created_at, updated_at)
  VALUES (1, 2, 'test', 'ACCEPTED', NOW(), NOW());
  ```
- **결합 지점 (미리 알아두기)**: 오늘 만드는 이 API가 완성되면, 인원1이 Day7(4-8 지원자 수락)에서 "이미 Project가 있으면 자동 합류"하는 로직을 짤 때 여러분이 만든 `ProjectRepository.findByRecruitId()`, `ChatRoomRepository.findByProjectId()`를 그대로 갖다 씁니다. **메서드 시그니처를 바꾸지 마세요** (바꿔야 하면 인원1에게 미리 알려주세요).

---

## 구현 순서

### 6-1 프로젝트 생성

**요청**: `{ "name": "제일기획 공모전 팀" }`
**응답**: `201` + `{ "projectId":1, "chatRoomId":10, "recruitStatus":"CLOSED", "memberCount":3 }`

> ⚠️ 스펙 문서 예시 응답엔 `recruitStatus":"CLOSED"`라고 나와있지만, **Business Logic 10번 항목에는 "Recruit.Status는 변경하지 않는다"**고 명시되어 있습니다. 이건 문서 자체의 모순처럼 보이는 부분이라 — **Business Logic 설명(상태 유지)을 따르는 게 맞습니다.** 응답 필드의 `recruitStatus`는 그냥 "현재 공고 상태를 그대로 반환"하는 필드로 구현하세요(전환 없이). 헷갈리면 인원1과 상의해서 `docs/API_SPEC.md`의 예시를 수정해두세요(문서 먼저 고치는 팀 규칙).

```java
@Transactional
public ProjectCreateResponse createProject(Long memberId, Long recruitId, ProjectCreateRequest request) {
    Recruit recruit = recruitRepository.findById(recruitId)
            .orElseThrow(() -> new BusinessException(ErrorCode.RECRUIT_NOT_FOUND));
    if (!recruit.getMember().getId().equals(memberId)) {
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    if (projectRepository.existsByRecruitId(recruitId)) {
        throw new BusinessException(ErrorCode.PROJECT_ALREADY_EXISTS);
    }
    List<Apply> accepted = applyRepository.findAllByRecruitId(recruitId).stream()
            .filter(a -> a.getStatus() == ApplyStatus.ACCEPTED)
            .toList();
    if (accepted.isEmpty()) {
        throw new BusinessException(ErrorCode.NO_ACCEPTED_APPLICANT);
    }

    Project project = new Project(recruit, request.name(), recruit.getStartDate(), recruit.getEndDate());
    projectRepository.save(project);

    ChatRoom chatRoom = new ChatRoom(ChatRoomType.GROUP, project);
    chatRoomRepository.save(chatRoom);

    projectMemberRepository.save(new ProjectMember(project, recruit.getMember(), true)); // 작성자 = 리더
    chatRoomMemberRepository.save(new ChatRoomMember(chatRoom, recruit.getMember()));

    for (Apply apply : accepted) {
        projectMemberRepository.save(new ProjectMember(project, apply.getMember(), false));
        chatRoomMemberRepository.save(new ChatRoomMember(chatRoom, apply.getMember()));
    }

    int memberCount = accepted.size() + 1;
    return new ProjectCreateResponse(project.getId(), chatRoom.getId(), recruit.getStatus().name(), memberCount);
}
```

### 6-2 프로젝트 상세 조회

```java
public ProjectDetailResponse getDetail(Long memberId, Long projectId) {
    Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
    if (!projectMemberRepository.existsByProjectIdAndMemberIdAndLeftAtIsNull(projectId, memberId)) {
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    Long leaderId = projectMemberRepository.findAllByProjectIdAndLeftAtIsNull(projectId).stream()
            .filter(ProjectMember::isLeader).findFirst()
            .map(pm -> pm.getMember().getId()).orElseThrow();
    return new ProjectDetailResponse(project.getId(), project.getRecruit().getTitle(),
            project.getStatus().name(), project.getStartDate(), project.getEndDate(), leaderId);
}
```
> `ProjectMemberRepository`에 `findAllByProjectIdAndLeftAtIsNull`, `existsByProjectIdAndMemberIdAndLeftAtIsNull`이 이미 있습니다 — 그대로 쓰세요.

---

## 예외 케이스

| 케이스 | 코드 |
|---|---|
| 공고 작성자 아님 | 403 `FORBIDDEN` |
| 존재하지 않는 공고 | 404 `RECRUIT_NOT_FOUND` |
| 이미 프로젝트 있는 공고 | 409 `PROJECT_ALREADY_EXISTS` |
| ACCEPTED 지원자 없음 | 409 `NO_ACCEPTED_APPLICANT` |
| 프로젝트 참여자 아닌데 상세조회 | 403 `FORBIDDEN` |

---

## 테스트 체크리스트

- [ ] ACCEPTED 지원자 1명 이상인 공고에서 프로젝트 생성 → 201, Project/ChatRoom/ProjectMember 전부 DB에 생성 확인
- [ ] ACCEPTED 지원자 없는 공고에서 생성 시도 → 409 `NO_ACCEPTED_APPLICANT`
- [ ] 같은 공고로 재생성 시도 → 409 `PROJECT_ALREADY_EXISTS`
- [ ] 작성자가 아닌 계정으로 생성 시도 → 403
- [ ] 생성 직후 `Recruit.status`가 그대로 OPEN인지 확인 (CLOSED로 바뀌면 안 됨 — Business Logic 10번 원칙)
- [ ] 생성 후 상세조회(6-2) → 리더ID가 공고 작성자와 일치하는지
- [ ] 프로젝트 팀원이 아닌 제3자가 상세조회 시도 → 403

---

## 커밋/PR 가이드

- 브랜치: `feat/project-create`
- 커밋: `feat: 프로젝트 생성(그룹채팅 생성) API 구현 (6-1)` → `feat: 프로젝트 상세 조회 API 구현 (6-2)`
- PR 올리기 전에 **인원1에게 `ProjectRepository`/`ChatRoomRepository` 메서드명이 확정됐다고 한 번 알려주세요** (Day7에서 그대로 갖다 쓸 예정입니다).

---

## 막힐 수 있는 포인트

- **문서상 응답 예시(`recruitStatus:"CLOSED"`)와 Business Logic(상태 유지) 불일치** — 위에서 설명한 대로 Business Logic을 따르세요. 헷갈리면 팀 채팅으로 공유해서 문서를 먼저 고치는 게 좋습니다.
- **팀원이 자기 자신에게 중복 등록되는 케이스**: 작성자가 실수로 본인 공고에 지원해서 ACCEPTED 상태가 됐다면(원래는 불가능하지만) 리더로도, 일반 팀원으로도 두 번 등록될 위험이 있습니다. 오늘은 이 엣지케이스까지 방어하지 않아도 되지만, 시간 되면 `accepted`에서 작성자 본인은 걸러내는 필터를 추가하세요.
- **`memberCount` 계산**: 응답 예시엔 리더 포함 총원입니다. `accepted.size() + 1`로 계산하면 됩니다.
