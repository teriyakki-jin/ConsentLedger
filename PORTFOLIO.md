# ConsentLedger — 포트폴리오

> 개인정보 전송요구권(마이데이터) 관리 플랫폼
> Java 17 · Spring Boot 3.3 · Spring AI 1.0 · React 19 · AWS EC2/RDS/S3/CloudFront

---

## 1. 프로젝트 개요

### 한 줄 요약

「개인정보 보호법」 제35조의2 기반의 **마이데이터 동의·전송 관리 시스템**을 풀스택으로 구현하고, SHA-256 해시 체인 감사 로그 + AI 이상 탐지 + MCP 서버를 통합한 개인 프로젝트입니다.

### 기획 배경

단순한 CRUD 앱 대신, **실무에서 직면하는 3가지 문제**를 해결하는 것을 목표로 설계했습니다.

| 문제 | 내 접근 |
|------|---------|
| 감사 로그는 삭제·위변조가 가능하다 | SHA-256 해시 체인 + DB 트리거로 불변성 보장 |
| 보안 위협을 사람이 일일이 분석해야 한다 | Claude Sonnet AI가 로그 패턴 자동 탐지 |
| AI 툴과 백엔드 시스템이 분리되어 있다 | Spring AI MCP 서버로 Claude Desktop과 직접 연결 |

### 주요 수치

| 항목 | 내용 |
|------|------|
| 개발 기간 | 2025 ~ 2026 (개인 프로젝트) |
| 백엔드 | Java 17 / Spring Boot 3.3.5 |
| 프론트엔드 | React 19 / TypeScript / Vite |
| 테스트 | 단위 119개 + 통합 26개 = **145개** |
| API 엔드포인트 | 25개+ |
| DB 마이그레이션 | Flyway V1~V11 |
| 배포 | AWS EC2 + RDS + S3 + CloudFront |
| Live Demo | https://dgrf2fg1y3qje.cloudfront.net |
| GitHub | https://github.com/teriyakki-jin/ConsentLedger |

---

## 2. 기술 스택 및 선택 이유

| 영역 | 기술 | 선택 이유 |
|------|------|----------|
| Language | Java 17 | LTS, Record/Pattern Matching |
| Framework | Spring Boot 3.3.5 | Hibernate 6, Spring Security 6 |
| AI | Spring AI 1.0.0 + Claude Sonnet | 공식 MCP 지원, 안정적 AI 추상화 |
| Database | PostgreSQL 16 | JSONB, 강력한 트랜잭션, 트리거 |
| Frontend | React 19 + TypeScript + Vite | 최신 React 기능, 빠른 빌드 |
| Container | Docker Compose + nginx | 환경 일관성, 리버스 프록시 |
| CI/CD | GitHub Actions + GHCR | 코드-이미지-배포 단일 파이프라인 |
| Cloud | AWS EC2 + RDS + S3 + CloudFront | 실제 운영 환경 경험 |
| Testing | JUnit 5 + Mockito + Testcontainers | 단위/통합 분리, 실제 DB 검증 |

---

## 3. 시스템 아키텍처

```
┌──────────────────────────────────────────────────────┐
│  브라우저 (React SPA)                                  │
│  → CloudFront (HTTPS) → S3 정적 파일                  │
│  → EC2 :80 (nginx → Spring Boot) → RDS               │
├──────────────────────────────────────────────────────┤
│  Claude Desktop                                        │
│  → SSE /sse (MCP 프로토콜, ADMIN JWT)                 │
│  → Spring Boot MCP 서버 (6개 도구)                    │
├──────────────────────────────────────────────────────┤
│  GitHub Actions CI/CD                                  │
│  push → 테스트 → Docker 빌드 → GHCR push              │
│       → S3 + CloudFront 프론트 배포                   │
│       → EC2 SSH docker compose 배포                   │
└──────────────────────────────────────────────────────┘
```

**인증 분기:**
- 사용자 → `Authorization: Bearer <JWT>` → `UsernamePasswordAuthenticationToken`
- Agent → `X-API-Key: <raw-key>` → SHA-256 해시 조회 → `ApiKeyAuthenticationToken`

---

## 4. 핵심 기능 및 기술 구현

### 4-1. SHA-256 해시 체인 감사 로그

**핵심 아이디어**: 각 감사 로그가 이전 로그의 해시를 포함해 체인을 형성합니다.

```java
String raw = prevHash + timestamp.truncatedTo(MICROS) + action + payloadJson;
String hash = DigestUtils.sha256Hex(raw);
```

**3중 불변성 보장:**

| 보장 수단 | 내용 |
|----------|------|
| DB 트리거 | `audit_logs_immutable`: UPDATE/DELETE 시 예외 발생 |
| 비관적 잠금 | `audit_chain_anchor` 행에 `SELECT FOR UPDATE` → 동시 삽입 직렬화 |
| 검증 API | `GET /admin/audit-logs/verify`: 전체 체인 배치(1,000건) 순회 검증 |

**겪은 문제와 해결:**

| 문제 | 원인 | 해결 |
|------|------|------|
| 해시 재계산 불일치 | Java 나노초 vs PostgreSQL 마이크로초 정밀도 차이 | `truncatedTo(MICROS)` 적용 |
| 대량 검증 시 OOM | JPA 1차 캐시 무한 누적 | 배치마다 `entityManager.clear()` |
| 동시 삽입 해시 순서 깨짐 | 낙관적 락 재시도 시 체인 충돌 | 비관적 락으로 전환 |

---

### 4-2. 전송 요청 상태 머신

```
PENDING ──approve──▶ APPROVED ──execute──▶ EXECUTING ──▶ COMPLETED
         ──reject──▶ REJECTED                          ──▶ FAILED
```

**구현 포인트:**
- `approve`, `execute`는 각각 `findByIdForUpdate()` (비관적 잠금) → 동시 이중 승인 방지
- 멱등성: 동일 요청 중복 생성 시 `REQUIRES_NEW` 트랜잭션 격리 → 항상 201 반환
- 상태 전이 위반 시 `IllegalStateException` → 글로벌 핸들러에서 400 응답

---

### 4-3. AI 이상 탐지 (Claude Sonnet)

최근 N일 감사 로그를 Claude에게 전달해 4가지 보안 패턴 자동 분석:

| 패턴 | 탐지 기준 |
|------|----------|
| `ACCOUNT_TAKEOVER` | 짧은 시간 내 다수 로그인 실패 |
| `DATA_EXFILTRATION` | 단일 사용자의 대량 데이터 조회 |
| `PRIVILEGE_ABUSE` | 권한 범위 외 자원 접근 시도 |
| `ABNORMAL_HOURS` | 새벽 2~5시 접근 집중 |

**설계 결정:**
- payloadJson은 AI에 미전달 → **개인정보 노출 방지**
- 최대 500건 제한 (설정 가능) → **비용·지연 제어**
- JSON 파싱 실패 시 빈 findings 반환 → **Graceful Degradation**
- 데모 시나리오 API: 43개 이상 패턴 로그 자동 생성 → **즉시 시연 가능**

---

### 4-4. Spring AI MCP 서버

SSE(Server-Sent Events) 기반으로 Claude Desktop 등 MCP 클라이언트와 실시간 연결:

```java
@Tool(description = "감사 로그 이상 분석")
public AnomalyReport analyzeAnomalies(@ToolParam int days) {
    return anomalyDetectionService.analyze(days);
}
```

등록된 도구 6종: `getAuditLogs` · `verifyAuditChain` · `getConsentsByUser` · `getTransferRequests` · `listUsers` · `analyzeAnomalies`

> **활용 예**: "지난 7일간 로그에서 이상 징후 분석해줘" → Claude가 도구 호출 → AI 리포트 반환

---

### 4-5. AWS 인프라 직접 설계 및 운영

| 단계 | 작업 내용 |
|------|----------|
| EC2 | Amazon Linux 2023, Docker 설치, systemd 서비스 등록, nginx 리버스 프록시 |
| RDS | PostgreSQL 16, VPC 내부 전용 보안 그룹 (EC2-RDS 전용 SG 자동 생성) |
| S3 | 정적 웹 호스팅, index.html 캐시 없음 / assets 1년 캐시 |
| CloudFront | S3 오리진, HTTPS 강제, SPA 404→200 처리 |
| CI/CD | GitHub Actions → GHCR → SSH 배포 + S3 sync + CF invalidation |

**실제 트러블슈팅 경험:**

| 문제 | 원인 | 해결 |
|------|------|------|
| EC2→RDS TCP timeout | RDS SG 인바운드 소스가 자기 자신(default SG)으로 잘못 설정됨 | AWS 콘솔 EC2 연결 설정으로 전용 SG 자동 생성 |
| Spring Boot 시작 실패 | 컨테이너 내 `logs/` 디렉토리 없음 (Permission denied) | docker-compose volume mount + chmod |
| DNS NXDOMAIN | RDS 엔드포인트 오타 (`cxc18` vs `cxci8`, 숫자 1과 소문자 i 혼동) | 콘솔에서 직접 복사 확인 후 수정 |
| SSH 배포 실패 | `DEPLOY_KEY` base64 인코딩 vs raw PEM 불일치 | CI에서 raw PEM을 파일로 직접 저장하도록 변경 |
| OpenAI 빈 key 오류 | Spring AI auto-config이 빈 문자열 허용 안 함 | 더미 값으로 우회 (실제 OpenAI 미사용) |
| docker compose 없음 | Amazon Linux 2023에 compose plugin 미포함 | GitHub에서 standalone 바이너리 직접 설치 |

---

## 5. 테스트 전략

### 구조

```
test/
├── fixture/           # 테스트 데이터 팩토리 (UserFixture, AuditLogFixture 등)
├── domain/**/service/ # 단위 테스트 (@ExtendWith(MockitoExtension))
└── integration/       # 통합 테스트 (@SpringBootTest + Testcontainers)
```

### 설계 원칙

| 원칙 | 구현 |
|------|------|
| 테스트 격리 | SecurityContext는 `SecurityContextHolder` 직접 설정 |
| 실제 DB 검증 | Testcontainers PostgreSQL → Flyway 마이그레이션 자동 적용 |
| 픽스처 중앙화 | 모든 테스트 객체를 팩토리 메서드로 생성 → 중복 제거 |
| CI 분리 실행 | `-Dintegration.tests.enabled=true` 플래그로 단위/통합 분리 |

| 구분 | 수량 |
|------|------|
| 단위 테스트 | 119개 |
| 통합 테스트 | 26개 |
| **합계** | **145개** |

---

## 6. CI/CD 파이프라인

```
git push (main)
    │
    ▼
[test] 단위 + 통합 테스트 (Testcontainers)
    │
    ├──▶ [publish] Docker 빌드 → ghcr.io:latest + sha 태그
    │         └──▶ [deploy-backend] EC2 SSH → docker compose pull & up
    │
    └──▶ [deploy-frontend] Vite 빌드 → S3 sync → CloudFront invalidation
```

**설계 포인트**: Secrets 미설정 시 해당 Job 자동 skip → 로컬/VPS 환경에서도 CI 정상 동작

---

## 7. 보안 설계

| 항목 | 구현 |
|------|------|
| 비밀번호 | BCrypt (단방향 해시) |
| API Key | SHA-256 해시만 DB 저장, 평문 미보관 |
| JWT | HS256, 15분 만료 |
| SQL Injection | JPA Parameterized Query + JPA Specification |
| CORS | `allowedOriginPatterns` (ENV 기반 동적 설정) |
| 감사 로그 | 불변 해시 체인 + DB 트리거 (UPDATE/DELETE 차단) |
| AI 분석 | payloadJson 미전송 (개인정보 보호) |

---

## 8. 배운 점 / 개선 방향

### 배운 점

- **비관적 잠금 선택 기준**: 이전 값에 의존하는 연산(해시 체인)은 낙관적 락 재시도가 데이터 정합성을 깰 수 있음
- **Testcontainers 가치**: 실제 PostgreSQL 트리거·JSONB를 테스트에서 검증 → 통합 신뢰도 향상
- **Spring AI MCP**: 표준 프로토콜로 LLM↔백엔드 연결 시 AI 클라이언트 독립성 확보
- **AWS 실전 경험**: 보안 그룹 오설정·DNS 오타·CRLF 이슈 등 콘솔 디버깅 경험

### 개선 방향

- JWT Refresh Token 추가 (현재 15분 후 재로그인 필요)
- CloudFront + ALB + 도메인으로 EC2 IP 직접 노출 해소
- AI 이상탐지 결과 DB 저장 및 이력 관리
- 프론트엔드 테스트 (Vitest + Testing Library) 추가

---

## 9. 실행 방법

```bash
# 로컬 개발
docker compose up -d        # PostgreSQL + pgAdmin
./gradlew bootRun           # Spring Boot (localhost:8080)
cd frontend && npm run dev  # React (localhost:5173)

# 테스트 계정
# ADMIN: admin@consentledger.com / admin1234
# USER:  user@consentledger.com  / user1234
# AGENT: X-API-Key: test-api-key-agent-a

# 테스트
./gradlew test
./gradlew test -Dintegration.tests.enabled=true
```

> Live Demo: https://dgrf2fg1y3qje.cloudfront.net (동일 계정 사용 가능)
