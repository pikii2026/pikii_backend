# Pickii Backend

Pickii 백엔드 서버 (Spring Boot 4 / Java 17)

## 기술 스택

| 구분 | 기술 |
|:---|:---|
| Framework | Spring Boot 4.1 (Web MVC, Security, WebSocket) |
| DB | MySQL 8.0 (메인), MongoDB 7 (채팅), Redis 7 (인증/세션) |
| ORM | Spring Data JPA / Spring Data MongoDB |
| 인증 | JWT (jjwt) + Refresh Token Rotation |
| API 문서 | springdoc (Swagger UI) |

## 개발 환경 세팅 (팀원 온보딩)

사전 준비물: **JDK 17**, **Docker Desktop**, IntelliJ

```bash
# 1. 클론
git clone <repo-url>
cd Pickii

# 2. 환경변수 파일 생성 후 값 채우기 (JWT_SECRET 등은 팀 공유 값 사용)
cp .env.example .env

# 3. 인프라(MySQL + Redis + MongoDB) 실행
docker compose up -d

# 4. 실행 확인
docker compose ps
```

이후 IntelliJ에서 `PickiiApplication` 실행 (기본 프로필이 `local`이라 별도 설정 불필요).

- 서버: http://localhost:8080/api/v1
- Swagger: http://localhost:8080/api/v1/swagger-ui.html

메일 발송 기능을 테스트하려면 IntelliJ 실행 구성(Run Configuration)의
환경변수에 `MAIL_USERNAME`, `MAIL_PASSWORD`(Gmail 앱 비밀번호)를 등록한다.

## 인프라 관리 명령어

```bash
docker compose up -d      # 시작
docker compose stop       # 중지 (데이터 유지)
docker compose down       # 컨테이너 삭제 (데이터 유지)
docker compose down -v    # 컨테이너 + 데이터 완전 삭제
```

## 문서

| 문서 | 설명 |
|:---|:---|
| [docs/API_SPEC.md](docs/API_SPEC.md) | REST API 명세 |
| [docs/DB_Schema.md](docs/DB_Schema.md) | DB 스키마 / ERD |
| [docs/Redis_Policy.md](docs/Redis_Policy.md) | Redis 인증 정책 |
| [docs/ERROR_CODE.md](docs/ERROR_CODE.md) | 에러 코드 정의 |
| [docs/ENUM.md](docs/ENUM.md) | Enum 정의 |

## 브랜치 전략

- `main` : 항상 동작하는 상태 유지, 직접 push 금지
- `feat/기능명` : 기능 개발 후 PR로 `main`에 머지
