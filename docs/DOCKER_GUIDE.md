# Docker 개발환경 가이드

Pickii 백엔드 개발에 필요한 인프라(MySQL, Redis, MongoDB)를 Docker로 구성·실행하는 방법을 정리한 문서입니다.

---

## 1. 전체 구조

Spring Boot 앱은 각자 IntelliJ에서 실행하고, **DB 인프라만 Docker로 통일**합니다.
코드를 수정할 때마다 이미지를 다시 빌드할 필요가 없어 개발 속도가 빠르고,
팀원 전원이 동일한 DB 버전/설정을 사용하게 됩니다.

```
┌─────────────────────────────┐
│  IntelliJ에서 실행           │
│  Spring Boot (localhost:8080)│
└──────┬──────┬──────┬────────┘
       │      │      │
       ▼      ▼      ▼
   ┌──────┐┌──────┐┌───────┐
   │MySQL ││Redis ││MongoDB│   ← Docker 컨테이너 (docker compose)
   │:3307 ││:6379 ││:27017 │
   └──────┘└──────┘└───────┘
```

| 컨테이너 | 이미지 | 호스트 포트 | 용도 |
|:---|:---|:---|:---|
| pickii-mysql | mysql:8.0 | **3307** | 메인 DB (회원, 공고, 프로젝트 등) |
| pickii-redis | redis:7-alpine | 6379 | 인증 (Refresh Token, 인증코드, Blacklist) |
| pickii-mongo | mongo:7 | 27017 | 채팅 메시지 |

> **MySQL이 왜 3307?**
> 로컬 PC에 MySQL이 설치되어 있으면 3306이 이미 점유되어 컨테이너가 못 뜹니다.
> 그래서 호스트 포트를 3307로 통일했습니다. (컨테이너 내부는 3306 그대로)
> `application-local.yaml`도 3307을 바라보므로 아무것도 바꿀 필요 없습니다.

---

## 2. 사전 준비 (최초 1회)

1. **Docker Desktop 설치**: https://www.docker.com/products/docker-desktop/
   - Windows는 설치 중 WSL2 관련 항목을 모두 기본값으로 진행
   - 설치 후 재부팅 권장
2. 설치 확인:
   ```bash
   docker --version
   docker compose version
   ```

---

## 3. 최초 세팅

```bash
# 1. 프로젝트 루트에서 환경변수 파일 생성
cp .env.example .env
#    → .env 열어서 값 채우기 (JWT_SECRET 등 팀 공유 값)

# 2. 인프라 실행 (최초에는 이미지 다운로드로 몇 분 소요)
docker compose up -d

# 3. 상태 확인 — 3개 모두 Up(mysql은 healthy)이면 성공
docker compose ps
```

이후 IntelliJ에서 `PickiiApplication` 실행 → http://localhost:8080/api/v1/swagger-ui/index.html 접속 확인.

---

## 4. 일상 명령어

모든 명령은 `docker-compose.yml`이 있는 **프로젝트 루트**에서 실행합니다.

| 명령 | 설명 |
|:---|:---|
| `docker compose up -d` | 전체 시작 (이미 떠있으면 그대로 둠) |
| `docker compose ps` | 상태 확인 |
| `docker compose stop` | 중지 (데이터 유지, 다시 `up -d`로 재개) |
| `docker compose down` | 컨테이너 삭제 (데이터는 볼륨에 유지됨) |
| `docker compose down -v` | ⚠️ 컨테이너 + **데이터 완전 삭제** (DB 초기화) |
| `docker compose logs -f mysql` | 특정 서비스 로그 실시간 보기 (mysql/redis/mongodb) |
| `docker compose restart redis` | 특정 서비스만 재시작 |

> PC를 재부팅하면 컨테이너가 꺼져 있을 수 있습니다. `docker compose up -d` 한 번이면 됩니다.

---

## 5. DB 직접 접속하기

### MySQL
```bash
# CLI로 접속 (비밀번호는 .env의 MYSQL_ROOT_PASSWORD, 기본 pickii1234)
docker exec -it pickii-mysql mysql -uroot -p pickii

# 테이블 목록 확인
docker exec pickii-mysql mysql -uroot -ppickii1234 -e "USE pickii; SHOW TABLES;"
```
- IntelliJ Database 탭 / Workbench 접속 정보: `localhost:3307`, user `root`, db `pickii`

### Redis
```bash
docker exec -it pickii-redis redis-cli
# 예시: KEYS auth:*
```

### MongoDB
```bash
docker exec -it pickii-mongo mongosh pickii
# 예시: db.chat_messages.find()
```

---

## 6. 데이터 관리

- 데이터는 Docker **볼륨**(`pickii_mysql-data`, `pickii_mongo-data`)에 저장되어
  컨테이너를 지워도(`down`) 유지됩니다.
- 스키마가 꼬여서 초기화하고 싶을 때만 `docker compose down -v` 후 다시 `up -d`.
  (테이블은 앱 실행 시 JPA `ddl-auto: update`가 다시 만들어 줍니다)
- 개발 초반에는 `ddl-auto: update`라 엔티티 변경 시 테이블이 자동 반영되지만,
  **컬럼 삭제/이름 변경은 반영되지 않으므로** 이상하면 초기화가 깔끔합니다.

---

## 7. 트러블슈팅

### "ports are not available: ... 3306" (포트 충돌)
로컬에 설치된 MySQL/Redis/MongoDB가 포트를 점유한 경우입니다.
- MySQL은 이미 3307로 회피해 두었으므로 발생하지 않아야 정상
- Redis(6379)나 Mongo(27017)가 충돌하면: 점유 프로세스 확인 후 로컬 서비스를 끄거나,
  `docker-compose.yml`의 호스트 포트를 바꾸고 `application-local.yaml`도 같이 수정
  ```powershell
  # 점유 프로세스 확인 (PowerShell)
  Get-NetTCPConnection -LocalPort 6379 -State Listen | Select OwningProcess
  ```

### Docker Desktop이 "unexpected error"로 켜지지 않을 때
에러 메시지에 `unix://...sock: remove ...: The file cannot be accessed by the system` 이 보이면,
비정상 종료로 남은 소켓 파일이 부팅을 막는 상태입니다.

1. 작업 관리자에서 Docker 관련 프로세스 모두 종료
2. 에러 메시지에 나온 **경로의 폴더**를 이름 변경으로 치워두고 같은 이름의 빈 폴더 생성
   - 예: `C:\Users\{사용자}\AppData\Local\Docker\run` → `run_broken`으로 변경 후 `run` 새로 생성
   - 예: `C:\Users\{사용자}\AppData\Local\docker-secrets-engine` 동일 처리
   (소켓 파일은 일반 삭제가 안 되므로 폴더째 치우는 방식 사용)
3. Docker Desktop 재실행 — 같은 에러가 다른 경로로 뜨면 그 폴더도 동일 처리
4. 정상 부팅 후 `_broken` 폴더는 삭제 (재부팅 후 지워질 수 있음)

### 컨테이너는 떴는데 앱이 DB 연결에 실패할 때
- `docker compose ps`에서 mysql이 `(healthy)`인지 확인 — `(health: starting)`이면 잠시 대기
- `.env`의 `MYSQL_ROOT_PASSWORD`와 볼륨에 저장된 비밀번호가 다르면 인증 실패
  → 비밀번호를 바꿨다면 `docker compose down -v`로 초기화 필요
- 방화벽/VPN이 localhost 포트를 막는 경우도 있음

### 디스크 정리
```bash
docker system df        # 사용량 확인
docker image prune      # 사용하지 않는 (dangling) 이미지 정리
```
⚠️ `docker system prune -a --volumes`는 **모든 데이터를 지우므로** 사용하지 않습니다.

---

## 8. 배포 시 (참고)

로컬 개발은 이 문서의 compose 방식이면 충분합니다.
배포 시에는 프로젝트 루트의 `Dockerfile`(추가 예정)로 앱 이미지를 빌드하고,
DB는 배포 플랫폼의 매니지드 서비스(RDS, Railway Plugin 등)를 사용하는 것을 권장합니다.
