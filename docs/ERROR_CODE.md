# Error Code

## 1. 개요

Pickii 프로젝트에서 사용하는 모든 Error Code를 정의한다.

모든 에러 응답은 아래 형식을 따른다.

```json
{
    "error": {
        "code": "ERROR_CODE_NAME",
        "message": "에러에 대한 상세 메시지"
    },
    "timestamp": "2026-07-06T19:30:00+09:00"
}
```

---

# 2. Error Response Rule

| 항목          | 설명                 |
|:----------- |:------------------ |
| HTTP Status | HTTP 상태 코드         |
| Error Code  | 프로젝트 내부 Error Code |
| Message     | 사용자에게 반환할 메시지      |
| Timestamp   | 오류 발생 시각(KST)      |

---

# 3. Auth

## 이메일 인증

| HTTP | Error Code           | 설명          |
|:---- |:-------------------- |:----------- |
| 409  | EMAIL_ALREADY_EXISTS | 이미 가입된 이메일  |
| 404  | USER_NOT_FOUND       | 가입되지 않은 사용자 |
| 429  | TOO_MANY_REQUESTS    | 인증 요청 횟수 초과 |

---

## 이메일 인증 확인

| HTTP | Error Code                | 설명            |
|:---- |:------------------------- |:------------- |
| 400  | INVALID_VERIFICATION_CODE | 인증번호 불일치      |
| 404  | VERIFICATION_NOT_FOUND    | 인증번호 만료 또는 없음 |

---

## 닉네임

| HTTP | Error Code              | 설명           |
|:---- |:----------------------- |:------------ |
| 400  | VALIDATION_FAILED       | 닉네임 길이 조건 위반 |
| 409  | NICKNAME_ALREADY_EXISTS | 이미 사용 중인 닉네임 |

---

## 회원가입

| HTTP | Error Code                 | 설명             |
|:---- |:-------------------------- |:-------------- |
| 400  | VALIDATION_FAILED          | 입력값 검증 실패      |
| 400  | PASSWORD_MISMATCH          | 비밀번호 확인 불일치    |
| 400  | REQUIRED_TERMS_NOT_AGREED  | 필수 약관 미동의      |
| 401  | INVALID_VERIFICATION_TOKEN | 인증 토큰 만료 또는 조작 |

---

## 로그인

| HTTP | Error Code          | 설명              |
|:---- |:------------------- |:--------------- |
| 401  | INVALID_CREDENTIALS | 이메일 또는 비밀번호 불일치 |

---

## Token Refresh

| HTTP | Error Code            | 설명                      |
|:---- |:--------------------- |:----------------------- |
| 401  | INVALID_REFRESH_TOKEN | Refresh Token 만료 또는 불일치 |

---

## 로그아웃

| HTTP | Error Code    | 설명                    |
|:---- |:------------- |:--------------------- |
| 401  | INVALID_TOKEN | Access Token이 유효하지 않음 |

---

## 비밀번호 재설정

| HTTP | Error Code                 | 설명                    |
|:---- |:-------------------------- |:--------------------- |
| 400  | PASSWORD_MISMATCH          | 비밀번호 확인 불일치           |
| 401  | INVALID_VERIFICATION_TOKEN | Verification Token 오류 |

---

## 비밀번호 변경 (로그인 상태)

| HTTP | Error Code                 | 설명                    |
|:---- |:-------------------------- |:--------------------- |
| 400  | PASSWORD_MISMATCH          | 비밀번호 확인 불일치           |
| 401  | INVALID_TOKEN              | Access Token 오류       |
| 401  | INVALID_VERIFICATION_TOKEN | Verification Token 오류 |

---

## 회원 탈퇴

| HTTP | Error Code                 | 설명       |
|:---- |:-------------------------- |:-------- |
| 400  | REQUIRED_TERMS_NOT_AGREED  | 필수 동의 누락 |
| 401  | INVALID_CREDENTIALS        | 비밀번호 불일치 |
| 401  | INVALID_VERIFICATION_TOKEN | 인증 토큰 오류 |

---

## 소셜 로그인

| HTTP | Error Code           | 설명                    |
|:---- |:-------------------- |:--------------------- |
| 401  | INVALID_SOCIAL_TOKEN | 소셜 Access Token 검증 실패 |
| 404  | NOT_LINKED_ACCOUNT   | 연동된 계정 없음             |

---

## 소셜 연동 / 연동 해제

| HTTP | Error Code     | 설명            |
|:---- |:-------------- |:------------- |
| 401  | UNAUTHORIZED   | 로그인되지 않은 사용자  |
| 409  | ALREADY_LINKED | 이미 다른 계정과 연동됨 |
| 404  | NOT_LINKED_ACCOUNT | 연동된 계정 없음 (해제 시)  |

---

# 4. Recruit

## 공고 조회

| HTTP | Error Code        | 설명         |
|:---- |:----------------- |:---------- |
| 400  | VALIDATION_FAILED | 검색어 조건 위반  |
| 404  | RECRUIT_NOT_FOUND | 존재하지 않는 공고 |

---

## 공고 작성

| HTTP | Error Code        | 설명        |
|:---- |:----------------- |:--------- |
| 400  | VALIDATION_FAILED | 입력값 검증 실패 |

---

## AI 초안 생성

| HTTP | Error Code           | 설명       |
|:---- |:-------------------- |:-------- |
| 400  | VALIDATION_FAILED    | 필수 정보 누락 |
| 500  | AI_GENERATION_FAILED | AI 서버 오류 |

---

## 공고 상태 변경

| HTTP | Error Code        | 설명      |
|:---- |:----------------- |:------- |
| 403  | FORBIDDEN          | 작성자가 아님      |
| 404  | RECRUIT_NOT_FOUND  | 공고 없음         |
| 409  | ALREADY_CLOSED     | 이미 마감됨        |
| 409  | ALREADY_ADDITIONAL | 이미 추가 모집 중임   |

---

## 댓글

| HTTP | Error Code        | 설명          |
|:---- |:----------------- |:----------- |
| 400  | VALIDATION_FAILED | 댓글 길이 조건 위반 |
| 403  | FORBIDDEN         | 본인 댓글이 아님   |
| 404  | RECRUIT_NOT_FOUND | 공고 없음       |
| 404  | COMMENT_NOT_FOUND | 댓글 없음       |

---

## 지원

| HTTP | Error Code        | 설명        |
|:---- |:----------------- |:--------- |
| 400  | VALIDATION_FAILED | 메시지 길이 초과 |
| 400  | RECRUIT_CLOSED    | 모집 종료 (OPEN / ADDITIONAL 상태가 아님) |
| 409  | ALREADY_APPLIED   | 이미 지원함    |

---

## 지원 메시지 AI

| HTTP | Error Code           | 설명       |
|:---- |:-------------------- |:-------- |
| 500  | AI_GENERATION_FAILED | AI 생성 실패 |

---

# 5. Recruit (수정 / 취소 / 스크랩)

## 공고 수정

| HTTP | Error Code        | 설명        |
|:---- |:----------------- |:--------- |
| 400  | VALIDATION_FAILED | 입력값 검증 실패 |
| 403  | FORBIDDEN         | 작성자가 아님   |
| 404  | RECRUIT_NOT_FOUND | 공고 없음     |

---

## 지원 취소

| HTTP | Error Code              | 설명                        |
|:---- |:----------------------- |:------------------------- |
| 403  | FORBIDDEN               | 본인 지원서가 아님                |
| 404  | APPLY_NOT_FOUND   | 존재하지 않는 지원서               |
| 409  | APPLY_NOT_WAITING | WAITING 상태가 아니어서 취소할 수 없음 |

---

## 지원 키워드

| HTTP | Error Code        | 설명                    |
|:---- |:----------------- |:--------------------- |
| 400  | VALIDATION_FAILED | 키워드 6개 이상 선택 (최대 5개)  |

---

## 공고 스크랩

| HTTP | Error Code        | 설명         |
|:---- |:----------------- |:---------- |
| 404  | RECRUIT_NOT_FOUND | 존재하지 않는 공고 |
| 409  | ALREADY_SCRAPPED  | 이미 스크랩한 공고 |
| 404  | SCRAP_NOT_FOUND   | 스크랩하지 않은 공고 |

---

# 6. Project

## 프로젝트 생성 (그룹 채팅 생성)

| HTTP | Error Code             | 설명              |
|:---- |:---------------------- |:--------------- |
| 403  | FORBIDDEN              | 공고 작성자가 아님      |
| 404  | RECRUIT_NOT_FOUND      | 공고 없음           |
| 409  | PROJECT_ALREADY_EXISTS | 이미 프로젝트가 생성된 공고 |
| 409  | NO_ACCEPTED_APPLICANT  | 수락된 지원자가 없음     |

---

## 프로젝트 조회

| HTTP | Error Code        | 설명           |
|:---- |:----------------- |:------------ |
| 403  | FORBIDDEN         | 프로젝트 참여자가 아님 |
| 404  | PROJECT_NOT_FOUND | 존재하지 않는 프로젝트 |

---

## 프로젝트 종료

| HTTP | Error Code        | 설명         |
|:---- |:----------------- |:---------- |
| 403  | FORBIDDEN         | 프로젝트장이 아님   |
| 404  | PROJECT_NOT_FOUND | 프로젝트 없음     |
| 409  | ALREADY_ENDED     | 이미 종료된 프로젝트 |

---

## 프로젝트 연장

| HTTP | Error Code        | 설명                    |
|:---- |:----------------- |:--------------------- |
| 400  | VALIDATION_FAILED | 기존 종료일보다 이전 날짜        |
| 403  | FORBIDDEN         | 프로젝트장이 아님             |
| 404  | PROJECT_NOT_FOUND | 프로젝트 없음               |
| 409  | ALREADY_ENDED     | 이미 종료된 프로젝트는 연장 불가    |

---

## 프로젝트 나가기

| HTTP | Error Code          | 설명                       |
|:---- |:------------------- |:------------------------ |
| 403  | FORBIDDEN           | 프로젝트 참여자가 아님             |
| 404  | PROJECT_NOT_FOUND   | 프로젝트 없음                  |
| 409  | LEADER_CANNOT_LEAVE | 프로젝트장은 위임 후에만 나갈 수 있음    |

---

## 팀원 퇴출

| HTTP | Error Code               | 설명              |
|:---- |:------------------------ |:--------------- |
| 400  | CANNOT_KICK_SELF         | 프로젝트장 자신은 퇴출 불가 |
| 403  | FORBIDDEN                | 프로젝트장이 아님       |
| 404  | PROJECT_NOT_FOUND        | 프로젝트 없음         |
| 404  | PROJECT_MEMBER_NOT_FOUND | 대상이 팀원이 아님      |

---

## 프로젝트장 위임

| HTTP | Error Code               | 설명              |
|:---- |:------------------------ |:--------------- |
| 403  | FORBIDDEN                | 프로젝트장이 아님       |
| 404  | PROJECT_NOT_FOUND        | 프로젝트 없음         |
| 404  | PROJECT_MEMBER_NOT_FOUND | 위임 대상이 팀원이 아님   |

---

# 7. Schedule

## 개인 일정

| HTTP | Error Code         | 설명        |
|:---- |:------------------ |:--------- |
| 400  | VALIDATION_FAILED  | 입력값 검증 실패        |
| 400  | INVALID_RRULE      | RRULE(반복 규칙) 형식 오류 |
| 403  | FORBIDDEN          | 본인 일정이 아님        |
| 404  | SCHEDULE_NOT_FOUND | 일정 없음            |
| 404  | SCHEDULE_CATEGORY_NOT_FOUND | 지정한 일정 카테고리 없음 |

---

## 팀 일정

| HTTP | Error Code               | 설명           |
|:---- |:------------------------ |:------------ |
| 400  | VALIDATION_FAILED        | 입력값 검증 실패       |
| 400  | INVALID_RRULE            | RRULE(반복 규칙) 형식 오류 |
| 403  | FORBIDDEN                | 프로젝트 참여자가 아님    |
| 404  | PROJECT_NOT_FOUND        | 프로젝트 없음         |
| 404  | PARTY_SCHEDULE_NOT_FOUND | 팀 일정 없음         |

---

## 회의 일정 조율 (Meeting Poll)

| HTTP | Error Code          | 설명                       |
|:---- |:------------------- |:------------------------ |
| 400  | VALIDATION_FAILED   | 입력값 검증 실패                |
| 403  | FORBIDDEN           | 프로젝트장 또는 팀원이 아님          |
| 404  | PROJECT_NOT_FOUND   | 프로젝트 없음                  |
| 404  | POLL_NOT_FOUND      | 조율 없음                    |
| 404  | SLOT_NOT_FOUND      | 존재하지 않는 후보 슬롯            |
| 409  | POLL_ALREADY_ACTIVE | 진행 중인 조율이 이미 있음          |
| 409  | POLL_NOT_COLLECTING | 응답 수집 단계가 아님 (확정/취소된 조율) |
| 409  | UNANSWERED_EXISTS   | 미응답자가 있는 슬롯 확정 시도 (force 필요) |

---

## 일정 카테고리

| HTTP | Error Code                  | 설명            |
|:---- |:--------------------------- |:------------- |
| 400  | VALIDATION_FAILED           | 입력값 검증 실패     |
| 403  | FORBIDDEN                   | 본인 카테고리가 아님   |
| 404  | SCHEDULE_CATEGORY_NOT_FOUND | 일정 카테고리 없음    |

---

# 8. Chat

## 채팅방 / 채팅 내역 조회

| HTTP | Error Code         | 설명           |
|:---- |:------------------ |:------------ |
| 403  | FORBIDDEN          | 채팅방 참여자가 아님  |
| 404  | CHATROOM_NOT_FOUND | 채팅방 없음       |

---

## 1:1 채팅방 생성

| HTTP | Error Code       | 설명           |
|:---- |:---------------- |:------------ |
| 400  | CANNOT_CHAT_SELF | 자기 자신과 채팅 불가 |
| 404  | USER_NOT_FOUND   | 대상 회원 없음     |

---

## 채팅방 읽음 처리 / 나가기

| HTTP | Error Code          | 설명                             |
|:---- |:------------------- |:------------------------------ |
| 403  | FORBIDDEN           | 채팅방 참여자가 아님                    |
| 404  | CHATROOM_NOT_FOUND  | 채팅방 없음                         |
| 409  | LEADER_CANNOT_LEAVE | 프로젝트장은 그룹 채팅방을 나갈 수 없음 (위임 필요) |


---

## 이미지 업로드

| HTTP | Error Code           | 설명                       |
|:---- |:-------------------- |:------------------------ |
| 400  | INVALID_FILE_TYPE    | 허용되지 않는 파일 형식            |
| 403  | FORBIDDEN            | 채팅방 참여자가 아님              |
| 404  | CHATROOM_NOT_FOUND   | 채팅방 없음                   |
| 413  | FILE_TOO_LARGE       | 파일 크기 초과(최대 10MB)        |
| 500  | FILE_UPLOAD_FAILED   | 스토리지 업로드 실패              |

---

# 9. Notification

| HTTP | Error Code             | 설명        |
|:---- |:---------------------- |:--------- |
| 403  | FORBIDDEN              | 본인 알림이 아님 |
| 404  | NOTIFICATION_NOT_FOUND | 알림 없음     |

---

# 10. User

## 프로필 생성 / 수정

| HTTP | Error Code        | 설명                  |
|:---- |:----------------- |:------------------- |
| 400  | VALIDATION_FAILED | 입력값 검증 실패 (전공 길이 등) |
| 404  | UNIV_NOT_FOUND    | 존재하지 않는 대학교 ID      |

---


## 프로필 조회

| HTTP | Error Code       | 설명         |
|:---- |:---------------- |:---------- |
| 404  | RESUME_NOT_FOUND | 작성된 프로필 없음 |

---

## 프로필 생성

| HTTP | Error Code           | 설명       |
|:---- |:-------------------- |:-------- |
| 500  | AI_GENERATION_FAILED | AI 생성 실패 |

---

# 11. Feedback

## 평가 대상 조회

| HTTP | Error Code                | 설명              |
|:---- |:------------------------- |:--------------- |
| 403  | FORBIDDEN                 | 프로젝트 참여자가 아님    |
| 404  | PROJECT_NOT_FOUND         | 프로젝트 없음         |
| 409  | PROJECT_NOT_ENDED         | 아직 종료되지 않은 프로젝트 |
| 409  | EVALUATION_PERIOD_EXPIRED | 평가 기간(3일) 종료    |

---

## 평가 작성

| HTTP | Error Code                | 설명              |
|:---- |:------------------------- |:--------------- |
| 400  | ALREADY_EVALUATED         | 이미 평가 완료        |
| 403  | FORBIDDEN                 | 평가 권한 없음        |
| 404  | PROJECT_NOT_FOUND         | 프로젝트 없음         |
| 409  | PROJECT_NOT_ENDED         | 아직 종료되지 않은 프로젝트 |
| 409  | EVALUATION_PERIOD_EXPIRED | 평가 기간(3일) 종료    |

---

## AI 평가 결과 조회

| HTTP | Error Code              | 설명                   |
|:---- |:----------------------- |:-------------------- |
| 403  | FORBIDDEN               | 프로젝트 참여자가 아님         |
| 404  | PROJECT_NOT_FOUND       | 프로젝트 없음              |
| 404  | EVALUATION_NOT_FOUND    | AI 피드백 데이터 없음        |
| 409  | EVALUATION_NOT_COMPLETE | 평가 기간이 아직 끝나지 않음     |
| 409  | INSUFFICIENT_EVALUATION | 최소 평가 인원 미달 (2인 팀 포함) |

---

# 12. Common

공통적으로 여러 API에서 사용하는 Error Code이다.

| HTTP | Error Code            | 설명               |
|:---- |:--------------------- |:---------------- |
| 400  | VALIDATION_FAILED     | 입력값 검증 실패        |
| 401  | UNAUTHORIZED          | 인증 필요            |
| 401  | INVALID_TOKEN         | Access Token 오류  |
| 401  | INVALID_REFRESH_TOKEN | Refresh Token 오류 |
| 403  | FORBIDDEN             | 권한 없음            |
| 404  | RESOURCE_NOT_FOUND    | 리소스를 찾을 수 없음     |
| 409  | DUPLICATE_RESOURCE    | 중복된 리소스          |
| 429  | TOO_MANY_REQUESTS     | 요청 제한 초과         |
| 413  | FILE_TOO_LARGE        | 파일 크기 초과          |
| 500  | INTERNAL_SERVER_ERROR | 서버 내부 오류         |
| 500  | AI_GENERATION_FAILED  | AI 서버 오류         |
| 500  | FILE_UPLOAD_FAILED    | 파일 업로드 실패        |

---

# 13. Error Code Naming Rule

Error Code는 다음 규칙을 따른다.

- 모두 대문자 사용
- 단어는 `_`(Snake Case)로 구분
- 의미가 명확하도록 작성

예시

```text
INVALID_TOKEN
PASSWORD_MISMATCH
EMAIL_ALREADY_EXISTS
RECRUIT_NOT_FOUND
ALREADY_APPLIED
AI_GENERATION_FAILED
```

---

# 14. Error Handling Guideline

- 하나의 요청에서 가장 먼저 발생한 오류를 반환한다.
- Error Code는 변경하지 않는다.
- Message는 사용자 친화적으로 작성한다.
- HTTP Status와 Error Code는 항상 일치해야 한다.
- 모든 예외는 Global Exception Handler를 통해 동일한 응답 형식으로 반환한다.
