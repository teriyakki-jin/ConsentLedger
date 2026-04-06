# ConsentLedger

> 마이데이터/개인정보 전송 동의 관리를 위한 참조 구현 프로젝트

![Java](https://img.shields.io/badge/Java-17-007396?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?style=flat-square&logo=springboot)
![Spring AI](https://img.shields.io/badge/Spring_AI-1.0.0-6DB33F?style=flat-square&logo=spring)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql)
![CI/CD](https://github.com/teriyakki-jin/ConsentLedger/actions/workflows/ci.yml/badge.svg)
![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)

---

## 목차

- [개요](#개요)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Quick Start (로컬)](#quick-start-로컬)
- [Default Test Data](#default-test-data)
- [API Reference](#api-reference)
- [Environment Variables](#environment-variables)
- [Production 배포](#production-배포)
- [CI/CD](#cicd)
- [MCP 연동 (Claude Desktop)](#mcp-연동-claude-desktop)
- [Test](#test)

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
| Infra | Docker Compose, Dockerfile (multi-stage), nginx, GitHub Actions CI/CD |

---

## Architecture

<img width="2816" height="1536" alt="Gemini_Generated_Image_kfvuhjkfvuhjkfvu" src="https://github.com/user-attachments/assets/5cc71476-ed95-433e-a0ea-4d81bd5b563d" />


### 프로덕션

```
  Client (HTTP/HTTPS)
        │
  ┌─────▼──────┐
  │  nginx :80  │  reverse proxy, security headers, SSE passthrough
  └─────┬──────┘
        │ proxy_pass
  ┌─────▼──────────┐
  │  app :8080      │  Spring Boot (prod profile), non-root, healthcheck
  └─────┬──────────┘
        │ JPA / JDBC
  ┌─────▼──────────┐
  │  postgres :5432 │  PostgreSQL 16-alpine, named volume 영속화
  └────────────────┘

  ※ 모든 서비스는 Docker 내부 네트워크. 외부 노출은 nginx :80만.
  ※ SSL/TLS는 nginx 앞단의 로드밸런서 또는 certbot으로 처리 권장.
```

**인증 방식**
- 사용자: `Authorization: Bearer <JWT>`
- Agent: `X-API-Key: <api-key>` (SHA-256 해시로 DB 저장)

**감사 로그 무결성**
- 각 로그 행의 SHA-256 해시가 이전 행 해시를 포함 → 해시 체인 형성
- `GET /admin/audit-logs/verify`로 전체 체인 검증 가능

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
│   │       └── db/migration/      # Flyway SQL (V1~V11)
│   └── test/                      # 단위 + 통합 테스트
├── infra/
│   └── nginx/
│       ├── nginx.conf             # nginx 메인 설정
│       └── default.conf           # 리버스 프록시 서버 블록
├── docker-compose.yml             # 로컬 개발용 (DB only)
├── docker-compose.prod.yml        # 프로덕션용 (app + DB + nginx)
├── .env.example                   # 환경변수 템플릿
├── Dockerfile                     # Multi-stage 빌드 (JDK→JRE, 비루트 실행)
├── .github/workflows/ci.yml       # GitHub Actions CI/CD
└── build.gradle
```

---

## Quick Start (로컬)

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

| 변수 | 기본값 | 필수 | 설명 |
|------|--------|:----:|------|
| `JWT_SECRET` | 없음 | ✅ | JWT 서명키 (최소 32바이트 랜덤 문자열) |
| `DB_HOST` | `localhost` | | PostgreSQL 호스트 |
| `DB_PORT` | `5432` | | PostgreSQL 포트 |
| `DB_NAME` | `consentledger` | | 데이터베이스명 |
| `DB_USERNAME` | `consentledger` | | DB 사용자 |
| `DB_PASSWORD` | `consentledger_pw` | | DB 비밀번호 |
| `SPRING_PROFILES_ACTIVE` | `local` | | 프로파일 (`prod` for production) |
| `CORS_ALLOWED_ORIGINS` | `localhost:3000,5173,4173` | | CORS 허용 origin (콤마 구분) |
| `OPENAI_API_KEY` | 없음 | | AI 이상탐지용 (미설정 시 graceful degradation) |
| `IMAGE_TAG` | `latest` | | Docker 이미지 태그 (prod compose용) |
| `SERVER_NAME` | `_` | | nginx server_name |

> `JWT_SECRET` 생성: `openssl rand -hex 32`

---

## Production 배포

### 사전 요구사항

- Docker Engine 24+, Docker Compose v2
- GitHub Actions에서 `ghcr.io` 이미지를 pull할 수 있는 서버

### 1. 서버 초기 셋업

```bash
# 서버에서 실행
git clone https://github.com/teriyakki-jin/ConsentLedger.git /opt/consentledger
cd /opt/consentledger

# 환경변수 파일 생성
cp .env.example .env
vi .env  # 실제 값 채우기

# ghcr.io 이미지 pull 권한 설정 (Personal Access Token 또는 GitHub Actions token)
echo "<GITHUB_PAT>" | docker login ghcr.io -u <GITHUB_USER> --password-stdin

# 첫 실행
docker compose -f docker-compose.prod.yml up -d
```

### 2. 서비스 상태 확인

```bash
docker compose -f docker-compose.prod.yml ps
curl http://localhost/actuator/health
```

### 3. 수동 업데이트

```bash
cd /opt/consentledger
docker compose -f docker-compose.prod.yml pull app
docker compose -f docker-compose.prod.yml up -d --no-deps --wait app
docker image prune -f
```

### 4. GitHub Actions 자동 배포 설정

`main` push 시 자동으로 서버에 배포하려면 GitHub Secrets를 설정합니다.

**Settings → Secrets and variables → Actions → New repository secret**

| Secret | 설명 |
|--------|------|
| `DEPLOY_HOST` | EC2 IP 또는 호스트명 |
| `DEPLOY_USER` | SSH 사용자명 |
| `DEPLOY_KEY` | SSH 개인키 (PEM 형식) |
| `DEPLOY_PORT` | SSH 포트 (기본 22, 생략 가능) |

> `DEPLOY_HOST`가 설정되지 않으면 deploy job이 자동으로 skip됩니다.

**SSH 키 생성:**
```bash
ssh-keygen -t ed25519 -C "github-actions-deploy" -f deploy_key
cat deploy_key.pub >> ~/.ssh/authorized_keys  # EC2에서 실행
cat deploy_key                                 # 출력값을 DEPLOY_KEY secret에 저장
```

### 5. SSL/TLS 설정

nginx 앞단에서 처리하는 것을 권장합니다.

**Certbot (Let's Encrypt):**
```bash
apt install certbot python3-certbot-nginx
certbot --nginx -d your-domain.com
```

**또는** AWS CloudFront / ALB에서 TLS 종료 후 EC2로 HTTP 포워딩.

---

## AWS 배포 (EC2 + RDS + S3 + CloudFront)

### 아키텍처

```
사용자 (HTTPS)
    │
    ▼
CloudFront
    ├── /*       → S3 버킷 (React 정적 빌드)
    └── /api/*   → EC2 (nginx → Spring Boot)
                          │
                        RDS (PostgreSQL)
```

### AWS 리소스 설정

#### 1. RDS (PostgreSQL 16)
```
엔진: PostgreSQL 16
인스턴스: db.t3.micro (프리티어)
DB명: consentledger
포트: 5432
퍼블릭 액세스: 비활성 (EC2 보안 그룹에서만 허용)
```

#### 2. EC2
```
AMI: Amazon Linux 2023 또는 Ubuntu 22.04
인스턴스: t3.small 이상 권장
보안 그룹 인바운드:
  - 22 (SSH) — 내 IP만
  - 80 (HTTP) — 0.0.0.0/0 (CloudFront에서 포워딩)
```

**초기 셋업 스크립트:**
```bash
curl -fsSL https://raw.githubusercontent.com/teriyakki-jin/ConsentLedger/main/infra/aws/setup.sh | sudo bash
```

#### 3. S3 버킷
```
버킷 정책: CloudFront OAC(Origin Access Control)만 허용
정적 웹사이트 호스팅: 비활성 (CloudFront 통해서만 접근)
```

#### 4. CloudFront
```
오리진 1: S3 버킷 (OAC 설정)
오리진 2: EC2 DNS (HTTP, 포트 80)

Behavior 설정:
  - /api/*  → EC2 오리진, 캐시 없음 (Cache-Control: no-store)
  - /sse    → EC2 오리진, 캐시 없음, 커스텀 Origin Timeout: 3600s
  - /*      → S3 오리진, 캐시 활성화
```

### GitHub Secrets 전체 목록 (AWS 포함)

| Secret | 설명 | 필수 |
|--------|------|:----:|
| `DEPLOY_HOST` | EC2 퍼블릭 IP | |
| `DEPLOY_USER` | SSH 사용자 (`ec2-user` / `ubuntu`) | |
| `DEPLOY_KEY` | SSH 개인키 | |
| `AWS_ACCESS_KEY_ID` | IAM 액세스 키 | |
| `AWS_SECRET_ACCESS_KEY` | IAM 시크릿 키 | |
| `AWS_REGION` | 리전 (예: `ap-northeast-2`) | |
| `S3_BUCKET_NAME` | 프론트엔드 S3 버킷명 | |
| `CLOUDFRONT_DISTRIBUTION_ID` | CloudFront 배포 ID | |
| `VITE_API_BASE_URL` | 백엔드 API URL (예: `https://xxx.cloudfront.net`) | |

> `S3_BUCKET_NAME`이 설정되지 않으면 `deploy-frontend` job이 자동으로 skip됩니다.

**IAM 권한 (최소):**
```json
{
  "Effect": "Allow",
  "Action": [
    "s3:PutObject", "s3:DeleteObject", "s3:ListBucket",
    "cloudfront:CreateInvalidation"
  ],
  "Resource": [
    "arn:aws:s3:::YOUR_BUCKET/*",
    "arn:aws:cloudfront::*:distribution/YOUR_DIST_ID"
  ]
}
```

### EC2에서 RDS 사용 (.env 설정)
```bash
# /opt/consentledger/.env
DB_HOST=xxx.ap-northeast-2.rds.amazonaws.com
DB_PORT=5432
DB_NAME=consentledger
DB_USERNAME=consentledger
DB_PASSWORD=<rds-password>
JWT_SECRET=<openssl rand -hex 32>
CORS_ALLOWED_ORIGINS=https://xxx.cloudfront.net
OPENAI_API_KEY=<optional>

# RDS compose 사용
docker compose -f docker-compose.rds.yml up -d
```

---

## CI/CD

`main` 브랜치에 push 시 GitHub Actions가 자동 실행됩니다.

```
push to main
    │
    ▼
[test] ─── 단위 + 통합 테스트
    │
    ├──▶ [publish] ─── Docker 빌드 → ghcr.io push
    │         │ DEPLOY_HOST 설정됨
    │         └──▶ [deploy-backend] ─── EC2 SSH → docker compose rds up
    │
    └──▶ [deploy-frontend] ─── Vite 빌드 → S3 sync → CloudFront invalidation
                                (S3_BUCKET_NAME 설정됨)
```

| Job | 조건 | 내용 |
|-----|------|------|
| test | 모든 push/PR | 단위 테스트 + 통합 테스트 (Testcontainers) |
| publish | main push + test 성공 | Docker 빌드 + `ghcr.io` push |
| deploy-frontend | main push + test 성공 + `S3_BUCKET_NAME` 설정됨 | Vite 빌드 → S3 sync → CloudFront invalidation |
| deploy-backend | main push + publish 성공 + `DEPLOY_HOST` 설정됨 | SSH → EC2 `docker compose -f docker-compose.rds.yml up` |

> **GitHub Actions 권한 설정**: Settings → Actions → General → Workflow permissions → "Read and write permissions" 활성화 필요 (GHCR push를 위해)

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

**등록된 MCP 도구:**

| 도구 | 설명 |
|------|------|
| `getAuditLogs` | 감사 로그 조회 |
| `verifyAuditChain` | 해시 체인 무결성 검증 |
| `getConsentsByUser` | 사용자별 동의 조회 |
| `getTransferRequests` | 전송 요청 조회 |
| `listUsers` | 사용자 목록 조회 |
| `analyzeAnomalies` | AI 이상 탐지 실행 |

---

## Test

```bash
# 단위 테스트
./gradlew test

# 통합 테스트 (Docker 필요)
./gradlew test -Dintegration.tests.enabled=true
```

| 테스트 종류 | 수량 | 비고 |
|-----------|------|------|
| 단위 테스트 | 119개 | Mockito, @WebMvcTest |
| 통합 테스트 | 26개 | Testcontainers PostgreSQL |
