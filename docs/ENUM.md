# ENUM Specification

## 1. 개요

본 문서는 Pickii 프로젝트에서 사용하는 모든 Enum 값을 정의한다.

Enum은 다음 영역에서 공통으로 사용된다.

- MySQL
- Spring Boot
- API Request / Response
- Redis
- AI 처리

모든 Enum은 대문자(SCREAMING_SNAKE_CASE)를 사용한다.

---

# 2. Login Provider

로그인 제공자

| Enum   | 설명                | 지원 여부      |
|:------ |:----------------- |:---------- |
| LOCAL  | 이메일 + 비밀번호 로그인    | O          |
| KAKAO  | 카카오 로그인           | O          |
| GOOGLE | Google 로그인        | X (추후 확장용) |
| NAVER  | 네이버 로그인           | X (추후 확장용) |

현재는 **KAKAO만 지원**한다. GOOGLE / NAVER는 추후 확장을 위해 Enum에만 정의해 둔다.

소셜 로그인 정책

- 소셜로는 **신규 가입할 수 없다.** 소셜은 가입 수단이 아니라 로그인 편의 기능이다.
- 이메일+비밀번호로 가입한 뒤, 프로필에서 카카오를 **연동**해야 소셜 로그인을 사용할 수 있다.
- 식별은 이메일이 아니라 `ProviderUserId`(카카오 고유 id)로 한다.
- 연동 해제를 지원한다.

사용 위치

- Member
- SocialAccount

예시

```json
{
    "provider":"KAKAO"
}
```

---

# 3. Verification Purpose

이메일 인증 목적

| Enum       | 설명      |
|:---------- |:------- |
| SIGNUP     | 회원가입    |
| PW_RESET   | 비밀번호 변경 |
| WITHDRAWAL | 회원탈퇴    |

사용 위치

- Email Verification
- Redis Email Code
- Verification Token

예시

```json
{
    "purpose":"SIGNUP"
}
```

---

# 4. Verification Type

Verification Token 종류

| Enum     | 설명           |
|:-------- |:------------ |
| EMAIL    | 이메일 인증 완료    |
| NICKNAME | 닉네임 중복 확인 완료 |

사용 위치

- Redis Verification Token

예시

```json
{
    "verificationType":"EMAIL"
}
```

---

# 5. Recruit Status

공고 모집 상태

| Enum       | 설명                                |
|:---------- |:--------------------------------- |
| OPEN       | 모집 중 (지원 가능)                      |
| CLOSED     | 모집 마감 (지원 불가)                     |
| ADDITIONAL | 추가 모집 중 (지원 가능)                   |

`ADDITIONAL`은 팀 구성이 끝나 마감(CLOSED)했으나, 팀원 이탈 등으로 인원이 비어 다시 모집하는 상태이다.
프론트에서는 일반 모집(OPEN)과 구분하여 '추가 모집' 배지로 표시한다.

상태 전이

```
OPEN ──(공고 마감 / 프로젝트 생성)──> CLOSED

CLOSED ──(팀원 이탈 등으로 추가 모집)──> ADDITIONAL

ADDITIONAL ──(인원 충원 완료)──> CLOSED
```

지원 가능 여부

| Status     | 지원 가능 |
|:---------- |:----- |
| OPEN       | O     |
| ADDITIONAL | O     |
| CLOSED     | X     |

사용 위치

- Recruit

> DB_SCHEMA.md `Recruit.Status` 와 동일한 값을 사용한다.

---

# 6. Apply Status

지원 상태

| Enum     | 설명  |
|:-------- |:--- |
| WAITING  | 대기  |
| ACCEPTED | 수락  |
| REJECTED | 거절  |

사용 위치

- Apply

예시

```json
{
    "status":"WAITING"
}
```

> 지원서의 공식 명칭은 **Apply**로 통일한다. (테이블/엔티티/API 리소스 모두 `apply`)

---

# 7. Notification Type

알림 종류

| Enum        | 설명    |
|:----------- |:----- |
| CHAT        | 채팅    |
| RECRUIT     | 공고    |
| APPLY       | 지원    |
| ACCEPT      | 지원 수락 |
| REJECT      | 지원 거절 |
| COMMENT     | 댓글    |
| FEEDBACK    | 상호평가  |
| PROJECT     | 프로젝트  |
| SCHEDULE    | 팀 일정  |
| MEETING     | 회의 일정 조율 (일정 등록 요청 / 투표 요청 / 확정) |
| SYSTEM      | 시스템   |

사용 위치

- Notification

---

# 8. Notification Setting Type

알림 설정 종류

| Enum           | DB Column       | 설명            |
|:-------------- |:--------------- |:------------- |
| CHAT_NOTI      | `ChatNoti`      | 채팅 알림         |
| APPLICANT_NOTI | `ApplicantNoti` | 새 지원자 알림      |
| COMMENT_NOTI   | `CommentNoti`   | 내 글 댓글 알림     |
| SCHEDULE_NOTI  | `ScheduleNoti`  | 팀 일정 · 회의 조율 알림 |
| MATCH_NOTI     | `MatchNoti`     | 팀 합격/매칭 결과 알림 |
| PROJECT_NOTI   | `ProjectNoti`   | 프로젝트 종료 확인 · 상호평가 요청 알림 |
| MARKETING_NOTI | `MarketingNoti` | 마케팅/광고성 정보 수신 동의 |

사용 위치

- NotificationSetting

> DB_SCHEMA.md `NotificationSetting` 테이블 컬럼과 1:1로 대응한다.
> 마케팅 수신 동의는 회원가입 시 받은 값을 `MarketingNoti`의 초기값으로 저장하며, 이후 알림 설정 API에서 변경한다.

---

# 9. Evaluation Score

상호평가 점수

| 값   | 설명    |
|:--- |:----- |
| 1   | 매우 낮음 |
| 2   | 낮음    |
| 3   | 보통    |
| 4   | 좋음    |
| 5   | 매우 좋음 |

사용 위치

- Feedback

---

# 10. Tech Level

기술 숙련도

| 값   | 설명  |
|:--- |:--- |
| 1   | 하   |
| 2   | 중   |
| 3   | 상   |

사용 위치

- ResumeSkill

---

# 11. AI Generation Type

AI 생성 기능 종류

| Enum                | 설명     |
|:------------------- |:------ |
| RECRUIT_DRAFT       | 공고 초안  |
| APPLY_MESSAGE       | 지원 메시지 |
| ABOUT_ME            | 자기소개   |
| FEEDBACK_SUMMARY    | 피드백 요약 |

사용 위치

- AI Service

---

# 12. Academic Status

학적 상태

| Enum                 | 설명    |
|:-------------------- |:----- |
| ENROLLED             | 재학    |
| LEAVE_OF_ABSENCE     | 휴학    |
| GRADUATION_DEFERRED  | 졸업유예  |
| GRADUATED            | 졸업    |

사용 위치

- MemberUniv
- 프로필 생성 / 수정 / 조회 API

예시

```json
{
    "academicStatus":"ENROLLED"
}
```

---

# 13. Project Status

프로젝트 진행 상태

| Enum        | 설명           |
|:----------- |:------------ |
| IN_PROGRESS | 진행 중         |
| END         | 종료 (상호평가 가능) |

상태 전이

```
프로젝트 생성 (그룹 채팅 생성)

↓

IN_PROGRESS

↓

진행기간(EndDate) 도달

↓

프로젝트장에게 종료 확인 알림 (연장 / 종료 선택)

↓

3일 내 무응답 → 자동 종료

↓

END → 상호평가 가능
```

프로젝트장은 진행기간이 끝나기 전에도 언제든 직접 종료할 수 있다.

사용 위치

- Project

---

# 14. Meeting Poll Status

회의 일정 조율 상태

| Enum       | 설명                            |
|:---------- |:----------------------------- |
| COLLECTING | 응답 수집 중 (기본 12시간, 프로젝트장 조정 가능) |
| CONFIRMED  | 최종 일정 확정 (팀 일정 등록 완료)         |
| CANCELLED  | 조율 취소 (재조율 시 기존 조율은 CANCELLED) |

상태 전이

```
프로젝트장이 회의 개설

↓

COLLECTING ──(전원 응답 또는 마감 시각 도달)──> 집계 완료

↓

프로젝트장이 최종 확정 → CONFIRMED → PartySchedule 생성

CONFIRMED ──(프로젝트장이 취소하고 다시 조율)──> CANCELLED
```

- 투표 라운드를 나누지 않는다. 팀원은 슬롯별 가능/불가를 **1회 제출**한다.
- 미응답자는 가능/불가 어느 쪽으로도 집계하지 않고 **미응답으로 표시**한다.
- 최종 확정은 **프로젝트장이 직접** 한다. 자동 확정하지 않는다.

사용 위치

- MeetingPoll

---

# 15. Chat Message Type

채팅 메시지 종류

| Enum   | 설명                     |
|:------ |:---------------------- |
| TEXT   | 텍스트 메시지                |
| IMAGE  | 이미지 메시지                |
| SYSTEM | 시스템 안내 메시지 (senderId 없음, 예: 회의 조율 개설/확정, 팀 일정 참석여부 변경) |

사용 위치

- MongoDB ChatMessage
- WebSocket 메시지 전송

예시

```json
{
    "type":"IMAGE",
    "imageUrl":"https://cdn.pickii.com/chat/2026/07/uuid.png"
}
```

---

# 16. Chat Room Type

채팅방 종류

| Enum   | 설명                | 생성 시점                  |
|:------ |:----------------- |:---------------------- |
| DIRECT | 1:1 개인 채팅         | 상대에게 첫 메시지를 보낼 때 생성    |
| GROUP  | 프로젝트 팀 그룹 채팅      | 공고 작성자가 그룹 채팅을 생성할 때   |

사용 위치

- ChatRoom
- 채팅방 목록 조회 (탭 구분)

> GROUP 채팅방 생성은 곧 프로젝트 전환을 의미한다. (Project 생성 트리거)

---

# 17. Notification Reference Type

알림 클릭 시 이동할 대상(딥링크) 종류

| Enum        | 설명            | 이동 경로 예시                  |
|:----------- |:------------- |:------------------------- |
| RECRUIT     | 공고            | `/recruits/{referenceId}` |
| APPLY       | 지원서           | `/applies/{referenceId}`      |
| PROJECT     | 프로젝트          | `/projects/{referenceId}` |
| CHATROOM    | 채팅방           | `/chatrooms/{referenceId}` |
| COMMENT     | 댓글            | `/recruits/{referenceId}` |
| FEEDBACK    | 상호평가          | `/feedbacks/{referenceId}` |

사용 위치

- NotificationHistory (`ReferenceType`, `ReferenceId`)
- 알림 목록 조회 API

---

# 18. Redis Key Prefix

Redis Key Prefix 정의

| Prefix         | 설명                     |
|:-------------- |:---------------------- |
| auth:refresh   | Refresh Token          |
| auth:blacklist | Access Token Blacklist |
| auth:code      | 이메일 인증 코드              |
| auth:code:ip   | 이메일 인증 요청 IP당 횟수 제한    |
| auth:verify    | Verification Token     |

---

# 19. Common Boolean

프로젝트에서 사용하는 Boolean 의미

| 값     | 설명               |
|:----- |:---------------- |
| true  | 활성화 / 동의 / 가능    |
| false | 비활성화 / 미동의 / 불가능 |

예시

```json
{
    "autoLogin":true
}
```

```json
{
    "isAvailable":false
}
```

```json
{
    "isVerified":true
}
```

---

# 20. Naming Convention

## Enum Class

PascalCase

```
RecruitStatus
```

```
ApplyStatus
```

```
LoginProvider
```

---

## Enum Value

SCREAMING_SNAKE_CASE

```
OPEN
```

```
CLOSED
```

```
WAITING
```

```
ACCEPTED
```

---

## JSON

API에서는 Enum 이름 그대로 문자열을 사용한다.

예시

```json
{
    "status":"OPEN"
}
```

```json
{
    "provider":"KAKAO"
}
```

---

# 21. 관리 규칙

새로운 Enum을 추가할 경우

1. ENUM.md 수정
2. DB Enum 확인
3. API_SPEC 반영
4. Swagger 반영
5. Frontend 타입 반영

Enum 값은 변경하지 않는다.

기존 Enum을 삭제해야 하는 경우에는 Deprecated 처리 후 제거한다.
