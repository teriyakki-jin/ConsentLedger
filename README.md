# ConsentLedger

> 마이데이터/개인정보 전송 동의 관리를 위한 참조 구현 프로젝트

![Java](https://img.shields.io/badge/Java-17-007396?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?style=flat-square&logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql)
![React](https://img.shields.io/badge/React-19-61DAFB?style=flat-square&logo=react)
![TypeScript](https://img.shields.io/badge/TypeScript-5-3178C6?style=flat-square&logo=typescript)

---

## 개요

ConsentLedger는 **개인정보보호법상 개인정보 전송요구권(마이데이터)** 흐름을 검증하기 위한 참조 구현입니다.

- 사용자 로그인(JWT) 및 Agent API Key 이중 인증
- 데이터 전송 동의 생성 / 조회 / 철회
- 전송 요청 생성 / 사용자 승인 / 실행
- **SHA-256 해시 체인** 기반 감사 로그 무결성 검증
- 관리자 감사 로그 조회 및 PDF 리포트 다운로드

> 운영 환경이 아닌 흐름 검증 목적의 샘플 구현입니다.

---

## Tech Stack

| 영역 | 기술 |
|------|------|
| Backend | Java 17, Spring Boot 3.3, Spring Security, Spring Data JPA, Flyway |
| Database | PostgreSQL 16 |
| API Docs | springdoc-openapi (Swagger UI) |
| Frontend | React 19, TypeScript, Vite, Zustand, Axios |
| Infra | Docker Compose (PostgreSQL + pgAdmin) |

---

## Architecture

```
┌─────────────┐     JWT / API Key     ┌──────────────────────┐
│  Frontend   │ ─────────────────────▶│  Spring Boot Backend │
│ React/Vite  │                       │                      │
│ :5173       │ ◀─────────────────────│  - AuthFilter        │
└─────────────┘    JSON Response      │  - ConsentService    │
                                      │  - TransferService   │
                                      │  - AuditService      │
                                      └──────────┬───────────┘
                                                 │ JPA
                                      ┌──────────▼───────────┐
                                      │   PostgreSQL 16       │
                                      │   - SHA-256 Hash Chain│
                                      │   - Flyway V1~V11    │
                                      └──────────────────────┘
```

**인증 방식**
- 사용자: `Authorization: Bearer <JWT>`
- Agent: `X-API-Key: <api-key>` (SHA-256 해시로 DB 저장)

**감사 로그 무결성**
- 각 로그 행의 SHA-256 해시가 이전 행 해시를 포함 → 해시 체인 형성
- `GET /admin/audit-logs/verify` 로 전체 체인 검증 가능

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
│   │   │   │   ├── transfer/      # 전송 요청
│   │   │   │   ├── audit/         # 감사 로그 + 해시 체인
│   │   │   │   ├── admin/         # 관리자 기능
│   │   │   │   ├── agent/         # API Key 인증 Agent
│   │   │   │   ├── user/
│   │   │   │   └── dataholder/
│   │   │   └── global/            # config, security, exception, dto, util
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/      # Flyway SQL (V1~V11)
│   └── test/
├── frontend/                      # React + Vite
├── docker-compose.yml
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

### Admin

| Method | Path | 설명 |
|--------|------|------|
| GET | `/admin/users` | 사용자 목록 |
| GET | `/admin/agents` | Agent 목록 |
| PATCH | `/admin/agents/{id}/status` | Agent 상태 변경 |
| GET | `/admin/audit-logs` | 감사 로그 조회 |
| GET | `/admin/audit-logs/verify` | 해시 체인 무결성 검증 |
| GET | `/admin/reports/audit?from=...&to=...` | PDF 리포트 다운로드 |

---

## Environment Variables

`src/main/resources/application.yml` 기준 주요 환경 변수입니다.

```yaml
# DB
DB_HOST=localhost
DB_PORT=5432
DB_NAME=consentledger
DB_USERNAME=consentledger
DB_PASSWORD=consentledger_pw

# JWT
JWT_SECRET=<your-secret>

# CORS
CORS_ORIGIN_1=http://localhost:3000
CORS_ORIGIN_2=http://localhost:5173
```

> 운영 환경에서는 `JWT_SECRET`, DB 계정, CORS, API Key 관리 전략을 반드시 별도로 강화하세요.

---

## Test

```bash
./gradlew test
```

단위 테스트 18개 포함 (HashChainService, TransferStateMachine, JwtTokenProvider).
