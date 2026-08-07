# Pickii Backend Documents

Pickii 백엔드 개발 문서 저장소입니다.

본 문서는 API 명세, 데이터베이스 구조, Redis 인증 정책 및 프로젝트에서 사용하는 Enum과 Error Code를 관리하기 위한 문서입니다.

---

# 문서 목록

| 문서              | 설명                    |
|:--------------- |:--------------------- |
| API_SPEC.md     | REST API 명세           |
| DB_SCHEMA.md    | MySQL 데이터베이스 및 ERD    |
| REDIS_POLICY.md | Redis 인증 및 세션 관리 정책   |
| ERROR_CODE.md   | 프로젝트 전체 Error Code 정의 |
| ENUM.md         | 프로젝트에서 사용하는 Enum 정의   |
| PUSH_NOTIFICATION.md | 시스템 푸시 알림(FCM) 도입 계획 및 구현 기록 (구현 완료, API_SPEC 9-8/9-9) |

---

# 문서 구성

```
docs
│
├── README.md
├── API_SPEC.md
├── DB_SCHEMA.md
├── REDIS_POLICY.md
├── ERROR_CODE.md
├── ENUM.md
└── PUSH_NOTIFICATION.md
```

---

# 문서 설명

## API_SPEC.md

REST API 명세 문서이다.

포함 내용

- 공통 정책
- 인증(Auth)
- 메인(Home)
- 공고(Recruit) / 스크랩
- 마이페이지(User)
- 상호평가(Feedback)
- 기준 데이터(Master Data)
- 프로젝트(Project)
- 일정(Schedule)
- 채팅(Chat) / 이미지 업로드
- 알림(Notification)

API 구현 시 가장 먼저 참고하는 문서이다.

---

## DB_SCHEMA.md

MySQL 데이터베이스 구조 문서이다.

포함 내용

- ERD
- Table Schema
- FK 관계
- Index
- Cascade 정책

---

## REDIS_POLICY.md

Redis 사용 정책 문서이다.

포함 내용

- JWT 인증 구조
- Refresh Token 관리
- Refresh Token Rotation
- Access Token Blacklist
- Email Verification
- Verification Token
- Redis Key 구조
- 인증 Flow
- 운영 정책

Redis 관련 구현 시 참고한다.

---

## ERROR_CODE.md

프로젝트에서 사용하는 모든 Error Code를 관리한다.

각 Error는 다음 정보를 가진다.

- HTTP Status
- Error Code
- Description

API에서 발생하는 Error는 해당 문서를 기준으로 관리한다.

---

## ENUM.md

프로젝트에서 사용하는 Enum 정의 문서이다.

예시

- LoginProvider
- RecruitStatus
- ApplyStatus
- ProjectStatus
- AcademicStatus
- ChatMessageType
- ChatRoomType
- NotificationType
- NotificationReferenceType
- VerificationPurpose
- VerificationType

---

# 개발 규칙

## API

- RESTful API 설계
- JSON 사용
- KST(UTC+9) ISO-8601 시간 사용
- JWT 인증 사용

---

## Database

- MySQL 사용 (관계형 데이터)
- MongoDB 사용 (채팅 메시지)
- Soft Delete 사용
- FK 기반 관계 관리

---

## File Storage

채팅 이미지는 DB에 저장하지 않는다.

- Object Storage(예: AWS S3)에 파일 저장
- MongoDB에는 접근 URL만 저장
- 업로드는 REST API, 전송은 WebSocket

---

## Redis

인증 관련 데이터만 저장한다.

- Refresh Token
- Blacklist
- Email Verification
- Verification Token

모든 데이터는 TTL을 이용하여 자동 삭제된다.

---

## Authentication

가입 이메일은 도메인을 제한하지 않는다. (대학 이메일 / 일반 이메일 모두 허용)

- 이메일 형식만 검증하고, 실제 사용 여부는 인증 코드로 확인한다.
- 학교 정보는 이메일 도메인이 아닌 프로필의 학교명(`univ`)으로 관리한다.

인증 방식

- JWT Access Token
- JWT Refresh Token
- Refresh Token Rotation(RTR)

로그아웃 시

- Refresh Token 삭제
- Access Token Blacklist 등록

---

## Project

공고(Recruit)는 자동으로 프로젝트가 되지 않는다.

- 공고 작성자가 **그룹 채팅을 생성하는 시점**에 Project로 전환된다.
- 전환 시 Project + ChatRoom(GROUP) + ProjectMember가 함께 생성된다. (수락된 지원자 1명 이상 필요)
- 전환 시 Recruit는 CLOSED가 되며, 팀원 이탈 시 ADDITIONAL(추가 모집)로 전환한다.
- Project 상태는 IN_PROGRESS / END 두 가지이다.
- 진행기간이 끝나면 프로젝트장에게 종료 확인 알림을 보내고, 3일 내 무응답 시 자동으로 END 처리한다.
- 프로젝트장은 그 안에 연장(EndDate 갱신) 또는 즉시 종료를 선택할 수 있다.
- 추가 모집(ADDITIONAL) 전환은 자동이 아니라 프로젝트장이 직접 수행한다.

---

## Chat

- DIRECT(1:1) / GROUP(프로젝트) 두 종류로 나뉘며 별도 탭으로 관리한다.
- DIRECT 채팅방은 첫 메시지를 보낼 때 생성되며, 모든 회원 간 가능하다.
- 이미지는 REST로 업로드 후 URL을 WebSocket으로 전송한다.
- 채팅방별 알림 on/off를 지원한다. 전역 채팅 알림이 OFF면 전체 차단, ON일 때만 방별 조정.

---

## Social Login

- 현재 **카카오만 지원**한다. (Google / Naver는 Enum에만 정의, 추후 확장)
- **소셜로 신규 가입할 수 없다.** 소셜은 가입 수단이 아니라 로그인 편의 기능이다.
- 이메일+비밀번호로 가입한 뒤, 프로필에서 카카오를 연동해야 소셜 로그인을 쓸 수 있다.
- 식별 키는 이메일이 아니라 `providerUserId`(카카오 고유 id)다.
- 소셜 토큰은 저장하지 않는다. 서버가 카카오 API로 검증한 뒤 즉시 폐기하고 우리 JWT를 발급한다.
- 연동 해제를 지원한다.

---

## Schedule

- 단발 일정과 반복 일정은 별도 API로 분리한다. (`/single`, `/recurring`)
- 반복 일정은 RFC 5545 RRULE을 사용한다.
- 사용자가 카테고리(제목 + 색상)를 만들고 일정 생성 시 선택한다.
- 반복 일정의 특정 회차 제외는 `ExDate`로 처리한다. 회차별 수정은 지원하지 않는다.
- RRULE은 SQL로 조회할 수 없다. 조회 범위 내에서 애플리케이션이 전개한다(ical4j 등).

---

## Meeting Poll (팀 회의 일정 조율)

팀 일정은 조율 절차를 거쳐 확정한다.

- 프로젝트장이 회의를 개설하면 30분 단위 후보 슬롯이 자동 생성된다.
- 팀원은 슬롯별 가능/불가를 **1회 제출**한다. 투표 라운드를 나누지 않는다.
- 개인 캘린더는 **초기값(프리필)** 으로만 쓰인다. 캘린더 미등록자도 정상 참여할 수 있다.
- 미응답자는 집계에서 제외하고 **미응답으로 표시**한다.
- 응답 마감 기본 12시간(프로젝트장 조정 가능), 마감 3시간 전 리마인더 발송.
- **최종 확정은 프로젝트장이 직접** 한다. 확정 후 취소하고 재조율할 수 있다.

---

## MyPage

- 활동 내역: 지원 현황 / 작성 공고 / 작성한 댓글 / 스크랩한 공고 / 상호평가·피드백
- 알림 설정 7종: 채팅 / 새 지원자 / 댓글 / 팀일정·회의 / 매칭 결과 / 프로젝트(종료·평가) / 마케팅
- 설정: 알림 설정 / 로그아웃 / 회원 탈퇴 (비밀번호 변경은 설정 하위)
- 프로필 = 이력서이며, 연락용 이메일은 **이력서 생성 시점**에 가입 이메일이 복사된다.

---

## University / Major

- 대학교는 마스터 테이블(`Univ`)에서 **선택**한다. 사용자가 직접 입력하지 않는다.
- **캠퍼스는 구분하지 않는다.** 인문/자연, 서울/지역 캠퍼스는 모두 같은 학교로 취급한다.
- 전공은 마스터로 관리하지 않고 **사용자가 직접 입력**한다. (학과는 수가 많고 변동이 잦음)
- 교내(onCampus) 공고는 **작성자와 같은 대학교 소속 회원에게만** 노출된다. (`univId` 비교)

---

## Naming

- 지원서는 **Apply**로 통일한다. (`Apply`, `ApplyStatus`, `/applies`)

---

## Feedback

- 상호평가는 프로젝트 종료(END) 후 **3일간** 가능하다.
- 평가/AI 피드백의 기준은 Recruit가 아니라 **Project**이다.
- AI 피드백은 팀 인원별 최소 평가 인원(`ceil(N/2)`, 2인 팀 제외)을 만족해야 생성된다.

---

## AI

외부 AI API를 이용하는 기능

- 공고 초안 생성
- 지원 메시지 개선
- 자기소개 생성
- 상호평가 요약

---

# 문서 수정 규칙

새로운 API 추가 시

1. API_SPEC.md 수정

새로운 Redis Key 추가 시

1. REDIS_POLICY.md 수정

새로운 Error 추가 시

1. ERROR_CODE.md 수정

새로운 Enum 추가 시

1. ENUM.md 수정

DB 변경 시

1. DB_SCHEMA.md 수정

---

# Version

| Version | Description           |
|:------- |:--------------------- |
| v1.0    | Initial Documentation |
