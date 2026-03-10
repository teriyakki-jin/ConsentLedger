# ConsentLedger

> 마이데이터/개인정보 전송 동의 관리를 위한 참조 구현 프로젝트

![Java](https://img.shields.io/badge/Java-17-007396?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?style=flat-square&logo=springboot)
![Spring AI](https://img.shields.io/badge/Spring_AI-1.0.0-6DB33F?style=flat-square&logo=spring)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql)
![React](https://img.shields.io/badge/React-19-61DAFB?style=flat-square&logo=react)
![TypeScript](https://img.shields.io/badge/TypeScript-5-3178C6?style=flat-square&logo=typescript)
![CI](https://github.com/teriyakki-jin/ConsentLedger/actions/workflows/ci.yml/badge.svg)

---

## 개요

ConsentLedger는 **개인정보보호법상 개인정보 전송요구권(마이데이터)** 흐름을 검증하기 위한 참조 구현입니다.

- 사용자 로그인(JWT) 및 Agent API Key 이중 인증
- 데이터 전송 동의 생성 / 조회 / 철회
- 전송 요청 생성 / 사용자 승인 / 실행
- **SHA-256 해시 체인** 기반 감사 로그 무결성 검증
- 관리자 감사 로그 조회 및 PDF 리포트 다운로드
- **Spring AI MCP 서버** 통합 — Claude Desktop 등 AI 클라이언트와 직접 연결
- **AI 이상 탐지** — Claude Sonnet으로 감사 로그에서 보안 위협 자동 분석

> 운영 환경이 아닌 흐름 검증 목적의 샘플 구현입니다.

---

## Tech Stack

| 영역 | 기술 |
|------|------|
| Backend | Java 17, Spring Boot 3.3, Spring Security, Spring Data JPA, Flyway |
| AI / MCP | Spring AI 1.0.0, MCP Server (SSE), Claude Sonnet |
| Database | PostgreSQL 16 |
| API Docs | springdoc-openapi (Swagger UI) |
| Frontend | React 19, TypeScript, Vite, Zustand, Axios |
| Infra | Docker Compose, Dockerfile (multi-stage), GitHub Actions CI |

---

## Architecture


<img width="2816" height="1536" alt="Gemini_Generated_Image_kfvuhjkfvuhjkfvu" src="https://github.com/user-attachments/assets/5cc71476-ed95-433e-a0ea-4d81bd5b563d" />



**인증 방식**
- 사용자: `Authorization: Bearer <JWT>`
- Agent: `X-API-Key: <api-key>` (SHA-256 해시로 DB 저장)

**감사 로그 무결성**
- 각 로그 행의 SHA-256 해시가 이전 행 해시를 포함 → 해시 체인 형성
- `GET /admin/audit-logs/verify` 로 전체 체인 검증 가능

**MCP 서버**
- SSE 엔드포인트: `GET /sse` (ADMIN JWT 필요)
- 등록 도구: `getAuditLogs`, `verifyAuditChain`, `getConsentsByUser`, `getTransferRequests`, `listUsers`, `analyzeAnomalies`

---

## Project Structure

```
ConsentLedger/
├── src/
│   ├── main/
│   │   ├── java/com/consentledger/
│   │   │   ├── domain/
│   │   │   │   ├── auth/          # JWT 인증
│   │   │   │   ├── consent/       # 동의 관리
│   │   │   │   ├── transfer/      # 전송 요청 상태 머신
│   │   │   │   ├── audit/         # 감사 로그 + 해시 체인 + AI 이상 탐지
│   │   │   │   ├── admin/         # 관리자 기능 + MCP 관리 API
│   │   │   │   ├── agent/         # API Key 인증 Agent
│   │   │   │   ├── user/
│   │   │   │   └── dataholder/    # 데이터 보유자 관리
│   │   │   ├── mcp/               # Spring AI MCP 서버 설정 + 도구 클래스
│   │   │   └── global/            # config, security, exception, dto, util
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-prod.yml.example  # 운영 환경 설정 템플릿
│   │       ├── logback-spring.xml            # 프로필별 로깅 전략
│   │       └── db/migration/      # Flyway SQL (V1~V11)
│   └── test/                      # 단위 + 통합 테스트
├── frontend/                      # React + Vite
├── docker-compose.yml
├── Dockerfile                     # Multi-stage 빌드 (JDK→JRE, 비루트 실행)
├── .dockerignore
├── .github/workflows/ci.yml       # GitHub Actions CI (단위+통합 테스트, Docker 빌드)
└── build.gradle
```

---

## Quick Start

### 1. DB 실행 (Docker)

```bash
docker compose up -d
```

| 서비스 | URL | 계정 |
|--------|-----|------|
| PostgreSQL | `localhost:5432` | - |
| pgAdmin | `http://localhost:5050` | `admin@consentledger.com` / `admin` |

### 2. Backend 실행

```bash
./gradlew bootRun
```

| 서비스 | URL |
|--------|-----|
| API Server | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui` |
| OpenAPI JSON | `http://localhost:8080/api-docs` |
| MCP SSE | `http://localhost:8080/sse` (ADMIN JWT 필요) |

### 3. Frontend 실행

```bash
cd frontend
npm install
npm run dev
```

Frontend: `http://localhost:5173`

---

## Default Test Data

Flyway seed(`V9__seed_data.sql`)로 아래 계정 및 데이터가 자동 생성됩니다.

### 계정

| 역할 | Email | Password |
|------|-------|----------|
| ADMIN | `admin@consentledger.com` | `admin1234` |
| USER | `user@consentledger.com` | `user1234` |

### Agent API Key

- 헤더: `X-API-Key`
- 테스트 키: `test-api-key-agent-a`

---

## API Reference

### Auth

| Method | Path | 설명 |
|--------|------|------|
| POST | `/auth/login` | JWT 발급 |

### Consents

| Method | Path | 권한 | 설명 |
|--------|------|------|------|
| POST | `/consents` | USER | 동의 생성 |
| GET | `/consents` | USER | 내 동의 목록 |
| GET | `/consents/{id}` | USER | 동의 상세 |
| POST | `/consents/{id}/revoke` | USER | 동의 철회 |

### Transfer Requests

| Method | Path | 권한 | 설명 |
|--------|------|------|------|
| POST | `/transfer-requests` | JWT or API Key | 전송 요청 생성 |
| GET | `/transfer-requests` | JWT or API Key | 요청 목록 |
| POST | `/transfer-requests/{id}/approve` | USER | 사용자 승인 |
| POST | `/transfer-requests/{id}/execute` | AGENT | 실행 |

### Data Holders

| Method | Path | 권한 | 설명 |
|--------|------|------|------|
| GET | `/data-holders` | USER | 데이터 보유자 목록 |

### Admin

| Method | Path | 설명 |
|--------|------|------|
| GET | `/admin/users` | 사용자 목록 |
| GET | `/admin/agents` | Agent 목록 |
| PATCH | `/admin/agents/{id}/status` | Agent 상태 변경 |
| GET | `/admin/audit-logs` | 감사 로그 조회 |
| GET | `/admin/audit-logs/verify` | 해시 체인 무결성 검증 |
| GET | `/admin/audit-logs/analyze?days=7` | AI 이상 탐지 분석 |
| GET | `/admin/reports/audit?from=...&to=...` | PDF 리포트 다운로드 |
| GET | `/admin/mcp` | MCP 서버 상태 및 등록 도구 목록 |
| POST | `/admin/mcp/invoke` | MCP 도구 수동 실행 |

### MCP Server

| 엔드포인트 | 설명 |
|-----------|------|
| `GET /sse` | MCP SSE 스트림 (ADMIN JWT 필요) |

---

## Environment Variables

```yaml
# DB
DB_HOST=localhost
DB_PORT=5432
DB_NAME=consentledger
DB_USERNAME=consentledger
DB_PASSWORD=consentledger_pw

# JWT (필수 - 미설정 시 앱 시작 불가, 최소 32바이트)
JWT_SECRET=<your-secret-at-least-32-bytes>

# CORS (콤마 구분)
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173

# AI 이상 탐지 + MCP (선택 - 미설정 시 AI 기능 비활성)
OPENAI_API_KEY=<your-openai-api-key>
```

> `JWT_SECRET`은 환경변수로 **반드시** 주입해야 합니다 (기본값 없음).
> `OPENAI_API_KEY` 미설정 시 이상 탐지 및 MCP 도구가 graceful degradation으로 동작합니다.

---

## MCP 연동 (Claude Desktop)

`claude_desktop_config.json`에 아래를 추가하면 Claude Desktop에서 ConsentLedger 도구를 직접 사용할 수 있습니다.

```json
{
  "mcpServers": {
    "consentledger": {
      "transport": "sse",
      "url": "http://localhost:8080/sse",
      "headers": {
        "Authorization": "Bearer <ADMIN_JWT_TOKEN>"
      }
    }
  }
}
```

---

## Docker

```bash
# 이미지 빌드
docker build -t consentledger:latest .

# 컨테이너 실행
docker run -p 8080:8080 \
  -e JWT_SECRET=<32바이트_이상_시크릿> \
  -e DB_HOST=host.docker.internal \
  -e DB_USERNAME=consentledger \
  -e DB_PASSWORD=consentledger_pw \
  consentledger:latest
```

> 운영 환경 설정은 `src/main/resources/application-prod.yml.example`을 참고해 `application-prod.yml`을 작성하세요.

---

## CI/CD

`main` 브랜치에 push 또는 PR 시 GitHub Actions가 자동 실행됩니다.

| Job | 조건 | 내용 |
|-----|------|------|
| test | 모든 push/PR | 단위 테스트 + 통합 테스트 (Testcontainers) |
| build-docker | main push + test 성공 | Docker 이미지 빌드 검증 |

---

## Test

```bash
# 단위 테스트 (119개)
./gradlew test

# 통합 테스트 (Docker 필요, 26개)
./gradlew test -Dintegration.tests.enabled=true
```

| 테스트 종류 | 수량 | 비고 |
|-----------|------|------|
| 단위 테스트 | 119개 | Mockito, @WebMvcTest |
| 통합 테스트 | 26개 | Testcontainers PostgreSQL |
| E2E 테스트 | 4 시나리오 | Playwright |
