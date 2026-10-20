# ConsentLedger

마이데이터 동의와 전송 요청을 관리하고, 처리 이력을 해시 체인으로 검증하는 웹 서비스입니다.

[![CI/CD](https://github.com/teriyakki-jin/ConsentLedger/actions/workflows/ci.yml/badge.svg)](https://github.com/teriyakki-jin/ConsentLedger/actions/workflows/ci.yml)
![Java 17](https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white)
![Spring Boot 3.3.5](https://img.shields.io/badge/Spring_Boot-3.3.5-6DB33F?logo=springboot&logoColor=white)
![Spring AI 1.0.0](https://img.shields.io/badge/Spring_AI-1.0.0-6DB33F)
![React 19](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)

## 프로젝트 소개

개인정보 제공에 동의한 뒤 누가 전송을 요청하고 승인했는지, 그 이력이 바뀌지는 않았는지 확인할 수 있도록 만들었습니다. 사용자 동의, 전송 요청, 감사 로그를 연결하고 관리자 화면에서 이력을 조회·검증할 수 있습니다.

주요 구현은 세 가지입니다.

- **동의와 전송 요청 관리:** 요청자·소유자 확인, 중복 요청 처리, 승인·실행 상태 전이
- **감사 로그 검증:** SHA-256 해시 체인, DB 변경 방지 트리거, 체인 불일치 위치 확인
- **AI 도구 연동:** Spring AI MCP로 감사 조회·검증 도구 제공, OpenAI 모델로 이상 징후 분석

백엔드는 Spring Boot, 프론트엔드는 React로 구현했습니다. AWS 배포 구성과 GitHub Actions 워크플로도 저장소에 포함되어 있습니다. 외부 기관에 실제 개인정보를 전송하는 서비스가 아니라 동의·승인·감사 흐름을 구현한 개인 프로젝트입니다.

## 핵심 구현

### 동의와 전송 요청

사용자는 동의를 생성·철회하고, 해당 동의에 대한 전송 요청을 승인합니다. 요청 생성 시 동의의 유효성, 데이터 보유 기관이 지원하는 전송 방식, Agent의 협약과 상태를 확인합니다.

```text
REQUESTED -> APPROVED -> COMPLETED
    |           |
    v           v
  DENIED      FAILED
```

위 상태는 도메인에서 정의한 전이 규칙입니다. 현재 REST API는 요청 생성·조회·승인·실행을 제공합니다. 실행은 상태를 `COMPLETED`로 바꾸고 감사 로그를 기록하며, 외부 기관 API를 호출하지 않습니다.

- `idempotencyKey`로 중복 생성 요청을 조회하고, 요청자·동의·전송 방식이 같은지 확인합니다.
- 승인과 실행 시 비관적 잠금으로 해당 요청을 조회합니다.
- 승인은 동의 소유자, 실행은 동의 소유자 또는 요청한 Agent인지 확인합니다.

구현: [TransferRequestService](src/main/java/com/consentledger/domain/transfer/service/TransferRequestService.java) · [TransferStateMachine](src/main/java/com/consentledger/domain/transfer/service/TransferStateMachine.java)

### 감사 로그 해시 체인

로그를 추가할 때 이전 로그의 해시와 현재 레코드 정보를 연결합니다. 검증 시 payload와 레코드 해시를 다시 계산하고, 처음 불일치한 로그 ID와 원인을 반환합니다.

```text
payloadHash = SHA-256(canonicalJson(payload))
recordCore  = {action, actor IDs, IP, object, outcome, payloadHash, schemaVersion, ts}
hash        = SHA-256(prevHash + canonicalJson(recordCore))
```

- `audit_chain_anchor` 행을 잠가 로그 추가 순서를 조정합니다.
- 시간을 마이크로초 단위로 맞춰 PostgreSQL 저장 전후의 해시 계산을 일치시킵니다.
- `audit_logs_immutable` 트리거가 일반적인 `UPDATE`·`DELETE`를 차단합니다.
- `GET /admin/audit-logs/verify`가 로그를 1,000건씩 읽어 체인을 검증합니다.

해시 체인은 저장된 이력의 불일치를 찾는 장치입니다. DB 관리자가 트리거를 해제하고 전체 체인을 다시 작성하는 경우까지 보장하지는 않습니다. 데모용 변조 API는 트리거를 일시 해제해 검증 실패를 재현합니다.

구현: [HashChainService](src/main/java/com/consentledger/domain/audit/service/HashChainService.java) · [DB 마이그레이션](src/main/resources/db/migration)

### 사용자·Agent 인증

| 대상 | 인증 방식 | 확인 내용 |
| --- | --- | --- |
| 사용자·관리자 | JWT Bearer 토큰 | BCrypt 비밀번호 검증 후 발급, 기본 유효기간 1시간 |
| Agent | `X-API-Key` | 입력 키의 SHA-256 해시를 DB의 해시와 비교 |
| 관리자 API·MCP | `ADMIN` 권한 | `/admin/**`, `/sse`, `/mcp/**` 및 MCP 도구 접근 제어 |

Agent 전송 요청 생성 시에는 활성 상태, 협약 유효기간과 허용된 전송 방식도 확인합니다. JWT와 API Key는 호출 주체에 따른 인증 수단입니다.

구현: [SecurityConfig](src/main/java/com/consentledger/global/config/SecurityConfig.java) · [AgentPolicyValidator](src/main/java/com/consentledger/domain/agent/service/AgentPolicyValidator.java)

### AI 이상 징후 분석

Spring AI `ChatModel`에 감사 로그를 전달해 다음 패턴을 분석하도록 요청합니다. 저장소의 기본 모델 설정은 OpenAI `gpt-4o`입니다.

| 패턴 | 분석 대상 |
| --- | --- |
| `ACCOUNT_TAKEOVER` | 계정 탈취 의심 행동 |
| `DATA_EXFILTRATION` | 짧은 시간에 집중된 전송·접근 |
| `PRIVILEGE_ABUSE` | 과도한 관리자 작업이나 권한 남용 |
| `ABNORMAL_HOURS` | 업무 시간 밖의 활동 |

분석 범위는 1~30일, 최대 500건입니다. API 호출 실패는 `ANALYSIS_FAILED`, 로그가 없는 경우는 `NO_LOGS`로 구분합니다. 모델 응답 파싱에 실패하면 빈 결과를 반환할 수 있으므로, 빈 `findings`만으로 이상이 없다고 판단해서는 안 됩니다. 결과는 관리자 검토를 돕는 참고 정보입니다.

구현: [AnomalyDetectionService](src/main/java/com/consentledger/domain/audit/anomaly/AnomalyDetectionService.java)

## 구성

```text
React / TypeScript
       | /api/*
       v
Vite proxy (local) / nginx (deployment)
       |
       v
Spring Boot ---- PostgreSQL / Flyway
       |
       +-------- Spring AI ChatModel ---- OpenAI API
       |
       +-------- Spring AI MCP server <-- MCP client (ADMIN JWT)
```

| 영역 | 기술 |
| --- | --- |
| 백엔드 | Java 17, Spring Boot 3.3.5, Spring Data JPA, Spring Security |
| 인증 | JJWT, BCrypt, SHA-256 API Key 해시 |
| DB | PostgreSQL 16, Flyway |
| AI·도구 | Spring AI 1.0.0, OpenAI ChatModel, MCP WebMVC 서버 |
| 프론트엔드 | React 19, TypeScript, Vite, Zustand, Axios |
| 배포 | Docker Compose, nginx, EC2, RDS, S3, CloudFront |
| 검증 | JUnit 5, Mockito, Testcontainers, Playwright |

## 로컬 실행

필요 환경: JDK 17, Node.js(Vite 7 지원 버전), Docker Compose.

### 1. 저장소와 DB 준비

```bash
git clone https://github.com/teriyakki-jin/ConsentLedger.git
cd ConsentLedger
docker compose up -d
```

기본 Compose 파일은 PostgreSQL과 pgAdmin을 실행합니다. 백엔드가 시작되면 Flyway가 테이블과 데모 데이터를 준비합니다.

### 2. 백엔드 실행

PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
$env:APP_JWT_SECRET = "local-demo-only-signing-key-at-least-32-characters"
$env:SPRING_AI_OPENAI_API_KEY = "disabled-for-local-demo"
.\gradlew.bat bootRun
```

Bash:

```bash
export SPRING_PROFILES_ACTIVE=local
export APP_JWT_SECRET=local-demo-only-signing-key-at-least-32-characters
export SPRING_AI_OPENAI_API_KEY=disabled-for-local-demo
./gradlew bootRun
```

위 AI 키는 모델 초기화를 위한 더미 값입니다. AI 분석을 사용하려면 `SPRING_AI_OPENAI_API_KEY`를 본인의 유효한 키로 설정합니다. 이 변수는 로컬 프로필에 지정된 키보다 우선합니다. `.env` 파일을 만드는 것만으로 `bootRun`에 환경변수가 자동 주입되지는 않습니다.

### 3. 프론트엔드 실행

별도 터미널에서 실행합니다.

```bash
cd frontend
npm ci
npm run dev
```

프론트엔드의 `/api/*` 요청은 Vite가 `localhost:8080`으로 전달하면서 `/api` 접두사를 제거합니다.

| 서비스 | 주소 |
| --- | --- |
| 웹 화면 | `http://localhost:5173` |
| 백엔드 | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui` |
| 헬스 체크 | `http://localhost:8080/actuator/health` |
| MCP SSE | `http://localhost:8080/sse` |
| pgAdmin | `http://localhost:5050` |

### 데모 계정

| 역할 | 이메일 | 비밀번호 |
| --- | --- | --- |
| 관리자 | `admin@consentledger.com` | `admin1234` |
| 사용자 | `user@consentledger.com` | `user1234` |

로컬 데모 데이터 기준입니다. pgAdmin 로그인은 `admin@consentledger.com` / `admin`이며 앱 로그인과 별개입니다.

사용자 계정으로 동의 생성 → 전송 요청 → 승인 → 실행을 확인하고, 관리자 계정으로 감사 로그 조회와 체인 검증을 확인할 수 있습니다.

## 주요 API

아래 경로는 백엔드 직접 호출 기준입니다. 프론트엔드 프록시를 거칠 때는 `/api`를 앞에 붙입니다.

| 메서드 | 경로 | 기능 |
| --- | --- | --- |
| POST | `/auth/login` | 이메일·비밀번호로 JWT 발급 |
| POST / GET | `/consents` | 동의 생성·조회 |
| POST | `/consents/{id}/revoke` | 동의 철회 |
| POST / GET | `/transfer-requests` | 전송 요청 생성·본인 관련 목록 조회 |
| POST | `/transfer-requests/{id}/approve` | 동의 소유자가 승인 |
| POST | `/transfer-requests/{id}/execute` | 동의 소유자 또는 요청 Agent가 실행 |
| GET | `/admin/audit-logs` | 감사 로그 필터 조회 |
| GET | `/admin/audit-logs/verify` | 해시 체인 검증 |
| GET | `/admin/audit-logs/analyze?days=7` | AI 이상 징후 분석 |
| GET | `/admin/reports/audit?from=...&to=...` | 기간별 PDF 감사 보고서 |
| GET | `/admin/users` | 사용자 목록 |

관리자 API에는 `ADMIN` JWT가 필요합니다. 전체 요청·응답 형식은 실행 후 Swagger UI에서 확인할 수 있습니다.

## MCP 연동

Spring AI MCP 서버에 아래 6개 도구를 등록했습니다. MCP 클라이언트의 SSE 연결 설정에 서버 주소와 관리자 토큰을 지정합니다.

| 설정 | 값 |
| --- | --- |
| 전송 방식 | SSE |
| 서버 주소 | `http://localhost:8080/sse` |
| 인증 헤더 | `Authorization: Bearer <ADMIN_JWT>` |

클라이언트별 원격 SSE·헤더 지원 여부와 설정 형식은 다를 수 있습니다. 토큰이 만료되면 다시 로그인해 갱신해야 합니다.

| 도구 | 기능 |
| --- | --- |
| `getAuditLogs` | 감사 로그 필터 조회 |
| `verifyAuditChain` | 감사 로그 체인 검증 |
| `getConsentsByUser` | 지정한 사용자의 동의 조회 |
| `getTransferRequests` | 전송 요청 목록 조회 |
| `listUsers` | 사용자 목록 조회 |
| `analyzeAnomalies` | 지정 기간의 이상 징후 분석 |

이 도구들은 조회·검증·분석용이며 모두 `ADMIN` 권한을 요구합니다. Agent의 `X-API-Key` 인증과는 별도입니다.

구현: [McpServerConfig](src/main/java/com/consentledger/mcp/McpServerConfig.java) · [MCP 도구](src/main/java/com/consentledger/mcp/tools)

## 테스트

```bash
# 단위 테스트
./gradlew test

# 통합 테스트 활성화: Docker 필요
./gradlew test -Dintegration.tests.enabled=true --rerun-tasks
```

Windows에서는 `./gradlew` 대신 `.\gradlew.bat`를 사용합니다. 앞의 로컬 실행 환경변수를 설정한 터미널에서 실행하면 됩니다. 통합 테스트는 Testcontainers PostgreSQL을 사용하고, 기본 실행에서는 비활성화됩니다.

| 범위 | 확인 내용 |
| --- | --- |
| 서비스·컨트롤러 | 인증, 동의, 전송 요청, 권한과 오류 응답 |
| 감사·AI | 해시 검증, 이상 징후 분석 서비스와 예외 처리 |
| MCP | 도구 호출과 관리자 접근 제어 |
| 통합 | 실제 PostgreSQL을 사용한 API 흐름 |
| 프론트엔드 E2E | 로그인, 동의, 전송 요청, 관리자 화면 |

프론트엔드 검사:

```bash
cd frontend
npm run lint
npm run build
npm run test:e2e
```

E2E 실행 전 로컬 백엔드와 프론트엔드를 켜고 Playwright Chromium을 준비해야 합니다(`npx playwright install chromium`). Java 테스트 결과는 `build/reports/tests/test/index.html`에서 확인할 수 있습니다. 최신 CI 결과는 README 상단 배지에 연결되어 있습니다.

## 배포와 설정

배포 구성은 CloudFront가 `/api/*`를 EC2의 nginx로, 나머지 요청을 S3의 정적 프론트엔드로 전달하는 방식입니다. nginx는 API 접두사를 제거하고 Spring Boot로 전달하며, 백엔드는 RDS PostgreSQL에 연결합니다.

- [docker-compose.yml](docker-compose.yml): 로컬 PostgreSQL·pgAdmin
- [docker-compose.prod.yml](docker-compose.prod.yml): 컨테이너 기반 배포 구성
- [docker-compose.rds.yml](docker-compose.rds.yml): RDS 연결용 앱·nginx 구성
- [nginx 설정](infra/nginx): API 프록시 설정
- [환경변수 예시](.env.example): 배포용 설정 템플릿

배포용 `.env`를 작성한 뒤 RDS 구성으로 실행합니다.

```bash
docker compose -f docker-compose.rds.yml up -d
```

| 설정 | 용도 |
| --- | --- |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | DB 접속 대상 |
| `DB_USERNAME`, `DB_PASSWORD` | DB 인증 |
| `JWT_SECRET` | 배포 구성의 JWT 서명키 |
| `OPENAI_API_KEY` | 배포 구성의 AI 모델 키 |
| `CORS_ALLOWED_ORIGINS` | 허용할 프론트엔드 origin |
| `SPRING_PROFILES_ACTIVE` | 실행 프로필 |
| `IMAGE_TAG` | 배포할 컨테이너 이미지 태그 |

GitHub Actions는 `main` 대상 PR과 push에서 Java 테스트를 실행합니다. `main` push에서는 테스트 후 GHCR 이미지 게시, S3 프론트엔드 배포, EC2 백엔드 배포를 수행하도록 구성되어 있습니다. 프론트엔드는 `S3_BUCKET_NAME`, 백엔드는 `DEPLOY_HOST`가 없으면 배포 단계를 건너뜁니다. 실제 배포에는 나머지 AWS·SSH Secrets도 필요합니다.

워크플로: [ci.yml](.github/workflows/ci.yml)

## 범위와 제한

- 외부 마이데이터 기관 연동과 실제 데이터 전송은 포함하지 않습니다.
- 관리자 데모 API에는 의도적으로 감사 로그를 변조하는 기능이 있습니다. 공유·운영 환경에 배포하기 전 비활성화하거나 제거해야 합니다.
- 기본 계정과 DB 비밀번호는 로컬 실습용입니다. 배포 전 교체해야 합니다.
- AI 분석에는 외부 API 호출이 발생합니다. 실제 개인정보가 포함된 로그를 넣기 전 전송 항목과 처리 방침을 검토해야 합니다.
- 법적 적합성이나 실제 보안 탐지 정확도를 검증한 제품은 아닙니다.
