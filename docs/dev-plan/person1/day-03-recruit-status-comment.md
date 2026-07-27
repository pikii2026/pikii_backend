# Day 3 — 공고 상태변경 / 댓글 (3-4 ~ 3-9)

## 오늘의 목표

| 엔드포인트 | Method/URL | DoD |
|---|---|---|
| 3-4 공고 마감 | `PATCH /recruits/{recruitId}/close` | 204, 상태 OPEN/ADDITIONAL → CLOSED |
| 3-5 공고 추가모집 | `PATCH /recruits/{recruitId}/additional` | 204, 상태 CLOSED → ADDITIONAL |
| 3-6 공고 삭제 | `DELETE /recruits/{recruitId}` | 204, Soft Delete |
| 3-7 댓글/답글 목록 조회 | `GET /recruits/{recruitId}/comments` | Tree 구조 반환 |
| 3-8 댓글/답글 작성 | `POST /recruits/{recruitId}/comments` | 201 + commentId |
| 3-9 댓글 삭제 | `DELETE /comments/{commentId}` | 204, Soft Delete |

**DoD**: 댓글 작성 → 답글 작성 → 목록 조회 시 트리 구조로 정확히 나오고, 댓글 삭제 시 "삭제된 댓글입니다"로 표시되지만 답글은 그대로 남는다.

---

## 사전 확인 사항

- 어제(Day2) 만든 `Recruit` 엔티티를 그대로 사용. `close()`, `openAdditional()` 메서드는 **이미 엔티티에 구현되어 있음** (`Recruit.java` 확인해보세요). `softDelete()`도 이미 있습니다.
- 결합 지점 없음. 계속 독립적으로 진행합니다.

---

## 구현 순서

### 3-4, 3-5, 3-6 — 상태 변경 3종 (묶어서 한번에)

전부 작성자 본인 확인 → 엔티티 메서드 호출 → 끝, 패턴이 동일합니다.

```java
// RecruitService.java에 추가
@Transactional
public void close(Long memberId, Long recruitId) {
    Recruit recruit = getOwnedRecruit(memberId, recruitId);
    if (recruit.getStatus() == RecruitStatus.CLOSED) {
        throw new BusinessException(ErrorCode.ALREADY_CLOSED);
    }
    recruit.close();
}

@Transactional
public void openAdditional(Long memberId, Long recruitId) {
    Recruit recruit = getOwnedRecruit(memberId, recruitId);
    if (recruit.getStatus() == RecruitStatus.ADDITIONAL) {
        throw new BusinessException(ErrorCode.ALREADY_ADDITIONAL);
    }
    recruit.openAdditional();
}

@Transactional
public void delete(Long memberId, Long recruitId) {
    Recruit recruit = getOwnedRecruit(memberId, recruitId);
    recruit.softDelete();
}

private Recruit getOwnedRecruit(Long memberId, Long recruitId) {
    Recruit recruit = recruitRepository.findById(recruitId)
            .orElseThrow(() -> new BusinessException(ErrorCode.RECRUIT_NOT_FOUND));
    if (!recruit.getMember().getId().equals(memberId)) {
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    return recruit;
}
```

Controller에 `PATCH /recruits/{recruitId}/close`, `PATCH /recruits/{recruitId}/additional`, `DELETE /recruits/{recruitId}` 3개 추가.

### 3-7, 3-8, 3-9 — 댓글

**3-8 작성 먼저 구현**

1. `domain/recruit/dto/CommentCreateRequest.java` — `content`(2~100자), `parentCommentId`(nullable)
2. `CommentService.create(Long memberId, Long recruitId, CommentCreateRequest request)`
   ```java
   Recruit recruit = recruitRepository.findById(recruitId)
           .orElseThrow(() -> new BusinessException(ErrorCode.RECRUIT_NOT_FOUND));
   Comment parent = null;
   if (request.parentCommentId() != null) {
       parent = commentRepository.findById(request.parentCommentId())
               .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
   }
   Comment comment = Comment.builder().recruit(recruit).member(member).parent(parent).content(request.content()).build();
   return new CommentCreateResponse(commentRepository.save(comment).getId());
   ```

**3-9 삭제**: 본인 확인 후 `comment.softDelete()`.

**3-7 목록 조회 — 오늘의 핵심 난이도**

트리 구조로 응답해야 하고, 삭제된 댓글은 내용만 "삭제된 댓글입니다."로 바꿔서 보여줘야 합니다(작성자 정보는 제거하되 구조는 유지).

1. `CommentRepository`에 `findByRecruitIdAndParentIsNullOrderByCreatedAtAsc(Long recruitId)` 추가 (부모 댓글만)
2. 각 부모 댓글마다 `findByParentIdOrderByCreatedAtAsc(Long parentId)`로 답글 조회 (데이터 적으니 N+1 감수해도 무방, 신경 쓰이면 `findByRecruitId`로 전체를 한 번에 가져와서 메모리에서 `parent`로 그룹핑)
3. DTO 변환 시 삭제 여부 분기:
   ```java
   private CommentResponse toResponse(Comment comment) {
       if (comment.isDeleted()) {
           return CommentResponse.deleted(comment.getId(), replies);  // content="삭제된 댓글입니다.", authorId/Nickname=null
       }
       return CommentResponse.of(comment, replies);
   }
   ```

---

## 예외 케이스

| 케이스 | 코드 |
|---|---|
| 작성자 아닌데 마감/삭제 시도 | 403 `FORBIDDEN` |
| 이미 CLOSED인데 또 마감 | 409 `ALREADY_CLOSED` |
| 이미 ADDITIONAL인데 또 추가모집 | 409 `ALREADY_ADDITIONAL` |
| 댓글 2자 미만/100자 초과 | 400 `VALIDATION_FAILED` |
| 부모 댓글이 존재하지 않음(답글 작성 시) | 404 `COMMENT_NOT_FOUND` |
| 본인 댓글 아닌데 삭제 시도 | 403 `FORBIDDEN` |

---

## 테스트 체크리스트

- [ ] OPEN 공고 → 마감 → 상태 CLOSED로 변경 확인
- [ ] CLOSED 공고 → 추가모집 → 상태 ADDITIONAL로 변경 확인
- [ ] 이미 CLOSED인 공고 재마감 시도 → 409
- [ ] 다른 사람 공고 마감 시도 → 403
- [ ] 삭제 후 상세조회(3-1)하면 안 나오는지 (Soft Delete 반영 여부는 Recruit 조회 쿼리에서 `deletedAt IS NULL` 조건 확인 — 3-1 만들 때 이 조건 빠뜨렸으면 지금 같이 고치세요)
- [ ] 댓글 → 답글 → 답글의 답글까지 3단 작성 후 목록조회 시 트리 구조 정상
- [ ] 답글 있는 부모 댓글 삭제 → 목록에서 내용은 "삭제된 댓글입니다."로, 답글은 그대로 보임

---

## 커밋/PR 가이드

- 브랜치: `feat/recruit-status-comment`
- 커밋: `feat: 공고 마감/추가모집/삭제 API 구현 (3-4~3-6)` → `feat: 댓글/답글 CRUD 구현 (3-7~3-9)`

---

## 막힐 수 있는 포인트

- **Soft Delete된 공고가 목록/상세 조회에 계속 나오는 버그**: `RecruitRepository`, `CommentRepository` 조회 시 `deletedAt IS NULL` 조건을 빼먹기 쉽습니다. Day2에서 만든 3-1 조회 로직도 이 기회에 재점검하세요.
- **트리 재귀 깊이 제한 없음**: 스펙상 답글의 답글도 무한 허용이라, 재귀적으로 처리하거나 `parent` 체인을 따라가는 로직이 필요할 수 있습니다. 다만 실제 데이터에서 깊이가 매우 깊어질 일은 거의 없으니 단순 재귀 함수로 충분합니다.
