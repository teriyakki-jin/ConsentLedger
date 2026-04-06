# ConsentLedger — 포트폴리오 프로젝트 소개

> 개인정보 전송요구권(마이데이터) 관리 플랫폼
> Spring Boot 3.3 · PostgreSQL 16 · Spring AI 1.0 · MCP

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [기술 스택](#2-기술-스택)
3. [시스템 아키텍처](#3-시스템-아키텍처)
4. [핵심 기능 상세](#4-핵심-기능-상세)
5. [데이터 모델](#5-데이터-모델)
6. [API 명세](#6-api-명세)
7. [보안 설계](#7-보안-설계)
8. [테스트 전략](#8-테스트-전략)
9. [트러블슈팅 & 학습 포인트](#9-트러블슈팅--학습-포인트)
10. [실행 방법](#10-실행-방법)

---

## 1. 프로젝트 개요

### 배경

마이데이터(MyData) 제도는 개인이 자신의 데이터를 제3자에게 안전하게 이전할 수 있도록 보장하는 법적 권리입니다. 금융·의료·통신 등 데이터 보유 기관(DataHolder)이 사용자 요청에 따라 데이터를 이전해야 하는 이 흐름을, **API 기반의 참조 구현**으로 설계했습니다.

### 목표

- 동의(Consent) → 전송 요청(TransferRequest) → 승인 → 실행의 **전체 생명주기** 관리
- SHA-256 해시 체인으로 감사 로그의 **위·변조 불가능성** 보장
- AI Agent가 MCP(Model Context Protocol)를 통해 시스템과 **직접 상호작용**
- Claude AI 기반 **자동 이상 탐지** (보안 모니터링)

### 주요 수치

| 항목 | 내용 |
|------|------|
| 개발 기간 | 2025년 (개인 프로젝트) |
| 언어 / 프레임워크 | Java 17 / Spring Boot 3.3.5 |
| 테스트 커버리지 | 단위 22개, MCP 5개, 통합 5개 (총 99 테스트 케이스 통과) |
| API 엔드포인트 | 20개+ |
| DB 마이그레이션 | Flyway V1~V11 |
| GitHub | https://github.com/teriyakki-jin/ConsentLedger |

---

## 2. 기술 스택

### Backend

| 분류 | 기술 | 선택 이유 |
|------|------|-----------|
| Framework | Spring Boot 3.3.5 | Jakarta EE 10, 최신 Spring Security 6 |
| 언어 | Java 17 | Record, sealed classes, 최신 LTS |
| ORM | Spring Data JPA (Hibernate 6) | JPQL, Specification, 비관적 락 |
| DB | PostgreSQL 16 | JSONB, TIMESTAMPTZ(μs), 트리거 |
| 마이그레이션 | Flyway | 버전 관리된 스키마 진화 |
| 인증 | JJWT 0.12.6 | JWT, Spring Security 통합 |
| AI/MCP | Spring AI 1.0.0 | MCP Server, Claude 연동 |
| 문서화 | SpringDoc OpenAPI 2.6 | Swagger UI 자동 생성 |
| PDF | iText 8.0.4 | 감사 리포트 PDF 생성 |
| 코드 생성 | Lombok, MapStruct 1.6.3 | 보일러플레이트 제거 |

### Testing

| 도구 | 용도 |
|------|------|
| JUnit 5 | 단위/통합 테스트 |
| Mockito | Mock 기반 단위 테스트 |
| Testcontainers | 실제 PostgreSQL 컨테이너 통합 테스트 |
| Spring Security Test | 보안 컨텍스트 테스트 |

### Infrastructure

| 도구 | 용도 |
|------|------|
| Docker | PostgreSQL 로컬 개발 환경 |
| Gradle | 빌드 자동화 |

---

## 3. 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────┐
│                      Client Layer                        │
│   Web App (JWT)    AI Agent (API Key)   Claude Desktop  │
│       │                   │             (MCP/SSE)        │
└───────┼───────────────────┼─────────────┼───────────────┘
        │                   │             │
        ▼                   ▼             ▼
┌─────────────────────────────────────────────────────────┐
│               Spring Boot Application                    │
│                                                          │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────┐ │
│  │  JWT Filter  │  │ API Key      │  │  MCP Server   │ │
│  │  (사용자 인증)│  │ Filter       │  │  (SSE /sse)   │ │
│  │              │  │ (에이전트 인증)│  │               │ │
│  └──────┬───────┘  └──────┬───────┘  └───────┬───────┘ │
│         └─────────────────┴──────────────────┘         │
│                            │                            │
│  ┌─────────────────────────▼──────────────────────────┐ │
│  │                  Controller Layer                   │ │
│  │  Auth  Consent  Transfer  Admin  Audit  Anomaly    │ │
│  └─────────────────────────┬──────────────────────────┘ │
│                            │                            │
│  ┌─────────────────────────▼──────────────────────────┐ │
│  │                   Service Layer                     │ │
│  │                                                     │ │
│  │  ConsentService    TransferRequestService           │ │
│  │  HashChainService  AnomalyDetectionService          │ │
│  │  AuditLogService   AuditReportService               │ │
│  └─────────────────────────┬──────────────────────────┘ │
│                            │                            │
│  ┌─────────────────────────▼──────────────────────────┐ │
│  │               Repository Layer (JPA)                │ │
│  └─────────────────────────┬──────────────────────────┘ │
└────────────────────────────┼────────────────────────────┘
                             │
                ┌────────────▼────────────┐
                │     PostgreSQL 16        │
                │                          │
                │  users  agents           │
                │  consents  transfer_     │
                │  requests  audit_logs    │
                │  audit_chain_anchor      │
                └──────────────────────────┘
                             │
                ┌────────────▼────────────┐
                │      Claude AI           │
                │  (anomaly detection)     │
                └──────────────────────────┘
```

### 패키지 구조

```
src/main/java/com/consentledger/
├── global/
│   ├── config/          # CORS, JPA, Security 설정
│   ├── security/        # JWT·API Key 필터, CustomUserDetails
│   ├── exception/       # GlobalExceptionHandler, 도메인 예외
│   ├── dto/             # 공통 응답 DTO
│   └── util/            # HashUtils (SHA-256), 기타 유틸
├── domain/
│   ├── auth/            # AuthController, AuthService, LoginRequest/Response
│   ├── user/            # User 엔티티, UserRepository, UserService
│   ├── agent/           # Agent, AgentAgreement 엔티티
│   ├── dataholder/      # DataHolder 엔티티, DataHolderService
│   ├── consent/         # Consent 엔티티, ConsentController, ConsentService
│   ├── transfer/        # TransferRequest 엔티티, TransferRequestService
│   └── audit/
│       ├── AuditLog, AuditChainAnchor 엔티티
│       ├── HashChainService, AuditLogService, AuditReportService
│       └── anomaly/     # AnomalyDetectionService, AnomalyController, DTO
└── mcp/
    ├── McpServerConfig  # MethodToolCallbackProvider 등록
    └── tools/           # AuditMcpTools, ConsentMcpTools, TransferMcpTools ...
```

---

## 4. 핵심 기능 상세

### 4-1. 동의 관리 (Consent Lifecycle)

사용자가 특정 에이전트에게 특정 데이터 보유 기관의 데이터 접근을 허용하는 흐름입니다.

```
사용자
  │
  ├── POST /consents ──────────────────────────────────┐
  │   body: { agentId, dataHolderId, scopes, expiresAt }│
  │                                                     │
  │   ① Agent 존재·활성화 검증                          │
  │   ② DataHolder 존재·메서드 지원 검증                 │
  │   ③ Consent 저장 (status=ACTIVE)                   │
  │   ④ 감사 로그 기록 (CONSENT_CREATED)               │
  │                                                     ▼
  │                                             DB: consents
  │
  └── POST /consents/{id}/revoke ──────────────────────┐
      ① 소유자 확인 (본인 동의만 철회 가능)               │
      ② 멱등성: 이미 REVOKED면 오류 없이 반환            │
      ③ status = REVOKED                               │
      ④ 감사 로그 기록 (CONSENT_REVOKED)               │
                                                       ▼
                                                 DB: consents
```

**핵심 비즈니스 규칙:**
- 만료된 동의(`expiresAt < now`)는 조회 시 `EXPIRED`로 자동 계산 (`getEffectiveStatus()`)
- 전송 요청 생성 시 동의 유효성 실시간 검증

---

### 4-2. 전송 요청 상태 머신 (Transfer State Machine)

전송 요청은 명확한 상태 전이 규칙을 따릅니다.

```
                  생성
                   │
                   ▼
             REQUESTED ─────────────────────────► DENIED
                   │ (사용자 승인)                (사용자 거부)
                   │
                   ▼
             APPROVED ──────────────────────────► FAILED
                   │ (시스템/에이전트 실행)          (실행 실패)
                   │
                   ▼
             COMPLETED
```

**멱등성 보장:**
```java
// idempotency_key (unique constraint) 기반 중복 요청 처리
Optional<TransferRequest> existing = repository.findByIdempotencyKey(key);
if (existing.isPresent()) {
    return existing.get(); // 201로 그대로 반환 (중복 생성 없음)
}
```

**동시성 제어:**
- `approve()` / `execute()` 시 `SELECT ... FOR UPDATE` (비관적 락)
- `@Transactional(propagation = REQUIRES_NEW)` 로 독성 트랜잭션 방지

---

### 4-3. SHA-256 해시 체인 감사 로그

**목적:** 감사 로그의 위·변조를 수학적으로 방지

**체인 구조:**

```
Genesis (hash: "0000...0000")
  │
  ├── Log #1
  │     payloadHash = SHA256(payloadJson)
  │     recordCore = {action, actor_*, ts, outcome, ...}
  │     hash = SHA256(prevHash + SHA256(recordCore))
  │     prevHash = "0000...0000"
  │
  ├── Log #2
  │     hash = SHA256(log#1.hash + SHA256(recordCore#2))
  │     prevHash = log#1.hash
  │
  └── Log #N
        ...
```

**구현 핵심:**

```java
// 1. payload → payloadHash
String payloadHash = HashUtils.sha256(canonicalJson(payload));

// 2. record core (키 알파벳 정렬 → 결정적 직렬화)
Map<String, Object> recordCore = new TreeMap<>();
recordCore.put("action", log.getAction());
recordCore.put("ts", log.getTs().truncatedTo(MICROS).toString());
// ... 8개 필드 추가

// 3. 최종 해시 = SHA256(prevHash + SHA256(recordCore))
String hash = HashUtils.sha256(prevHash + HashUtils.sha256(toCanonicalJson(recordCore)));
```

**DB 불변성 보장 (PostgreSQL 트리거):**

```sql
CREATE FUNCTION prevent_audit_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'audit_logs table is immutable';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_logs_immutable
BEFORE UPDATE OR DELETE ON audit_logs
FOR EACH ROW EXECUTE FUNCTION prevent_audit_modification();
```

**검증 (배치 처리):**

```java
// OOM 방지: 1000개씩 배치 처리 + EntityManager.clear()
String prevHash = GENESIS_HASH;
for (int offset = 0; ; offset += BATCH_SIZE) {
    List<AuditLog> batch = repo.findAllOrderedById(offset, BATCH_SIZE);
    if (batch.isEmpty()) break;
    for (AuditLog log : batch) {
        String expected = computeHash(prevHash, log);
        if (!expected.equals(log.getHash())) {
            return VerifyResult.broken(log.getId(), "hash mismatch");
        }
        prevHash = log.getHash();
    }
    entityManager.clear(); // JPA 1차 캐시 제거
}
```

---

### 4-4. Spring AI MCP 서버

**MCP(Model Context Protocol)** 를 통해 Claude Desktop 등 AI 클라이언트가 시스템에 직접 접근합니다.

**등록 방식:**

```java
@Configuration
public class McpServerConfig {
    @Bean
    public ToolCallbackProvider tools(
        AuditMcpTools audit, ConsentMcpTools consent,
        TransferMcpTools transfer, AdminMcpTools admin,
        AnomalyMcpTools anomaly
    ) {
        return MethodToolCallbackProvider.builder()
            .toolObjects(audit, consent, transfer, admin, anomaly)
            .build();
    }
}
```

**제공 도구 (5개 클래스, ADMIN 전용):**

| 도구 클래스 | 메서드 | 기능 |
|-------------|--------|------|
| AuditMcpTools | `getAuditLogs(from, to, action, outcome, page)` | 감사 로그 필터링 조회 |
| AuditMcpTools | `verifyAuditChain()` | 해시 체인 무결성 검증 |
| ConsentMcpTools | `getConsentsByUser(userId)` | 특정 사용자 동의 목록 |
| TransferMcpTools | `getTransferRequests()` | 전체 전송 요청 목록 |
| AdminMcpTools | `listUsers()` | 사용자 목록 |
| AnomalyMcpTools | `analyzeAnomalies(days)` | AI 이상 탐지 실행 |

**Claude Desktop 연결 설정:**
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

### 4-5. AI 기반 이상 탐지

Claude Sonnet 모델이 감사 로그를 분석하여 보안 위협 패턴을 자동으로 탐지합니다.

**탐지 패턴:**

| 패턴 | 설명 | 시그널 |
|------|------|--------|
| ABNORMAL_HOURS | 비정상 시간대 접근 | 22:00~06:00 UTC 또는 주말 |
| PRIVILEGE_ABUSE | 권한 남용 | 과도한 ADMIN 작업, 대량 일괄 처리 |
| DATA_EXFILTRATION | 데이터 유출 시도 | 단시간 대량 전송 요청 |
| ACCOUNT_TAKEOVER | 계정 탈취 시도 | 새 IP 로그인, 반복 실패 후 성공 |

**Prompt Injection 방지:**

```java
// 로그 데이터 정제: 제어 문자 제거
String sanitized = raw.replaceAll("[\\r\\n\\t\\x00-\\x1F\\x7F]", " ");
// 필드 길이 제한 (토큰 남용 방지)
if (sanitized.length() > 200) sanitized = sanitized.substring(0, 200) + "...";
```

**응답 구조:**
```json
{
  "analyzedAt": "2025-03-14T10:30:00Z",
  "periodDays": 7,
  "totalLogsAnalyzed": 342,
  "findings": [
    {
      "patternType": "ABNORMAL_HOURS",
      "severity": "HIGH",
      "description": "사용자 user@example.com이 03:45 UTC에 10건의 전송 요청",
      "affectedActor": "user@example.com",
      "evidenceLogIds": [101, 102, 103],
      "recommendation": "해당 시간대 접근 차단 또는 추가 인증 요구"
    }
  ],
  "riskSummary": "1개의 HIGH 위협 탐지",
  "hasCriticalFindings": false,
  "analysisStatus": "SUCCESS"
}
```

**안전 장치:**
- 분석 범위 강제 제한: 1~30일
- 최대 로그 수 제한: 500건 (OOM 방지)
- `payloadJson` 제외: 민감 데이터 AI 전달 차단
- 파싱 실패 시 graceful degradation (빈 findings 반환)

---

## 5. 데이터 모델

### ERD 요약

```
users ──────────────────────────────── consents
  │ id(UUID)                             id(UUID)
  │ email (unique)                       user_id → users.id
  │ password_hash (BCrypt)               agent_id → agents.id
  │ role (USER/ADMIN)                    data_holder_id → data_holders.id
  │                                      scopes (JSONB)
agents ─────────────────────────────── status (ACTIVE/REVOKED/EXPIRED)
  │ id(UUID)                             expires_at
  │ name
  │ status (ACTIVE/SUSPENDED/REVOKED)  transfer_requests
  │ client_id (unique)                   id(UUID)
  │ api_key_hash (SHA-256)               consent_id → consents.id
  │ last_rotated_at                      method
  │                                      status (REQUESTED/APPROVED/..)
agent_agreements                         idempotency_key (unique)
  id(UUID)                               requester_user_id
  agent_id → agents.id                   requester_agent_id
  allowed_methods (JSONB)                approved_by_user_id
  valid_from, valid_until
                                       audit_logs (불변, INSERT 전용)
data_holders                             id(BIGSERIAL)
  id(UUID)                               ts (TIMESTAMPTZ, μs)
  institution_code (unique)              action, object_type, object_id
  name                                   actor_user_id, actor_agent_id
  supported_methods (JSONB)              outcome (SUCCESS/FAILURE)
  endpoint_url                           payload_json (text)
                                         payload_hash (SHA-256)
audit_chain_anchor                       prev_hash
  id = 1 (single row)                    hash (unique, SHA-256)
  last_log_id                            schema_version = 1
  last_hash
  updated_at
```

### JSONB 활용

JSONB 타입을 활용하여 동적 구조 데이터를 유연하게 저장합니다.

```sql
-- 에이전트가 허용된 작업 메서드 목록
agent_agreements.allowed_methods: ["GET_ACCOUNTS", "GET_TRANSACTIONS"]

-- 데이터 보유 기관이 지원하는 메서드
data_holders.supported_methods: ["GET_ACCOUNTS", "GET_TRANSACTIONS", "GET_INSURANCE"]

-- 동의 범위
consents.scopes: ["account", "transaction", "personal_info"]
```

---

## 6. API 명세

### 인증 방식

| 클라이언트 | 헤더 | 형식 |
|------------|------|------|
| 사용자 (JWT) | `Authorization` | `Bearer <JWT_TOKEN>` |
| 에이전트 | `X-API-Key` | `<RAW_API_KEY>` (SHA-256 해시와 비교) |

### 엔드포인트 목록

#### 인증
```
POST   /auth/login                     # JWT 발급
```

#### 동의 관리 (USER 이상)
```
POST   /consents                       # 동의 생성
GET    /consents                       # 내 동의 목록
GET    /consents/{id}                  # 동의 단건 조회
POST   /consents/{id}/revoke           # 동의 철회
```

#### 전송 요청 (USER, AGENT)
```
POST   /transfer-requests              # 전송 요청 생성 (멱등성)
GET    /transfer-requests              # 내 전송 요청 목록
POST   /transfer-requests/{id}/approve # 승인 (USER만)
POST   /transfer-requests/{id}/execute # 실행 (USER, AGENT)
```

#### 관리자 (ADMIN 전용)
```
GET    /admin/users                    # 사용자 목록
GET    /admin/agents                   # 에이전트 목록
PATCH  /admin/agents/{id}/status       # 에이전트 상태 변경

GET    /admin/audit-logs               # 감사 로그 조회 (필터링, 페이징)
GET    /admin/audit-logs/verify        # 해시 체인 무결성 검증
GET    /admin/audit-logs/analyze       # AI 이상 탐지 (?days=7)
GET    /admin/reports/audit            # 감사 리포트 PDF 다운로드

GET    /sse                            # MCP SSE 엔드포인트
```

#### 공개
```
GET    /swagger-ui/index.html          # API 문서
GET    /actuator/health                # 헬스 체크
```

---

## 7. 보안 설계

### 이중 인증 아키텍처

```
요청 수신
   │
   ├── X-API-Key 헤더 존재?
   │     YES → ApiKeyAuthenticationFilter
   │              ① SHA-256(rawKey) 계산
   │              ② DB에서 api_key_hash 비교
   │              ③ Agent.status == ACTIVE 확인
   │              ④ ApiKeyAuthenticationToken 발행
   │
   └── Authorization: Bearer ... 헤더 존재?
         YES → JwtAuthenticationFilter
                  ① JWT 서명 검증 (HMAC-SHA256)
                  ② 만료 시간 검증
                  ③ userId 추출 → UserDetails 로드
                  ④ UsernamePasswordAuthenticationToken 발행
```

### 권한 매트릭스

| 엔드포인트 | 익명 | USER | AGENT | ADMIN |
|------------|------|------|-------|-------|
| `/auth/**` | ✅ | ✅ | ✅ | ✅ |
| `/consents/**` | ❌ | ✅ | ✅ | ✅ |
| `/transfer-requests/**` | ❌ | ✅ | ✅ | ✅ |
| `/admin/**` | ❌ | ❌ | ❌ | ✅ |
| `/sse` | ❌ | ❌ | ❌ | ✅ |

### 인증 실패 응답 일관성

- 미인증: **401 Unauthorized** (`AuthenticationEntryPoint` 등록)
- 인증은 됐으나 권한 없음: **403 Forbidden**

### CORS 설정

```yaml
# application.yml
cors:
  allowed-origins:
    - http://localhost:3000
    - http://localhost:5173
  allowed-methods: GET, POST, PUT, DELETE, PATCH, OPTIONS
  allowed-headers: Authorization, Content-Type, X-API-Key
  allow-credentials: true
```

---

## 8. 테스트 전략

### 테스트 구조

```
src/test/java/com/consentledger/
├── fixture/                           # 테스트 데이터 팩토리
│   ├── UserFixture
│   ├── AgentFixture
│   ├── ConsentFixture
│   ├── TransferRequestFixture
│   └── AuditLogFixture
├── domain/                            # 단위 테스트 (Mockito)
│   ├── auth/service/AuthServiceTest
│   ├── consent/service/ConsentServiceTest
│   ├── transfer/service/TransferRequestServiceTest
│   ├── audit/service/HashChainServiceTest
│   ├── audit/anomaly/AnomalyDetectionServiceTest
│   └── ... (총 15개)
├── mcp/tools/                         # MCP 도구 단위 테스트 (5개)
│   ├── AuditMcpToolsTest
│   ├── ConsentMcpToolsTest
│   ├── AnomalyMcpToolsTest
│   └── ...
└── integration/                       # 통합 테스트 (Testcontainers)
    ├── BaseControllerIntegrationTest
    ├── AuthIntegrationTest
    ├── ConsentControllerIntegrationTest
    ├── TransferRequestControllerIntegrationTest
    └── AuditControllerIntegrationTest
```

### 단위 테스트 패턴

**보안 컨텍스트 설정:**
```java
// JWT 사용자 테스트
var userDetails = new CustomUserDetails(user);
var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
SecurityContextHolder.getContext().setAuthentication(auth);

// API Key 에이전트 테스트
var auth = new ApiKeyAuthenticationToken(agent);
SecurityContextHolder.getContext().setAuthentication(auth);
```

**ChatModel Deep Stub (AI 이상 탐지 테스트):**
```java
ChatModel chatModel = mock(ChatModel.class, RETURNS_DEEP_STUBS);
ChatResponse chatResponse = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
given(chatModel.call(any(Prompt.class))).willReturn(chatResponse);
given(chatResponse.getResult().getOutput().getText()).willReturn(mockJson);
```

### 통합 테스트 패턴

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@EnabledIfSystemProperty(named = "integration.tests.enabled", matches = "true")
class ConsentControllerIntegrationTest extends BaseControllerIntegrationTest {
    // Testcontainers PostgreSQL 자동 시작
    // Flyway 마이그레이션 자동 적용
    // @Transactional + 롤백으로 테스트 격리
}
```

```bash
# 통합 테스트 실행
./gradlew test -Dintegration.tests.enabled=true
```

---

## 9. 트러블슈팅 & 학습 포인트

### 9-1. JSONB 매핑 문제 (Hibernate 6)

**문제:** Hibernate 6에서 기존 `@Convert(converter = JsonbConverter.class)` 방식 동작 안 함

**해결:**
```java
// Before (Hibernate 5 스타일)
@Convert(converter = JsonbConverter.class)
private List<String> scopes;

// After (Hibernate 6)
@JdbcTypeCode(SqlTypes.JSON)
private List<String> scopes;
```

**학습:** Hibernate 6은 JDBC 타입 처리 방식이 크게 변경됨. `@JdbcTypeCode`가 표준 방법.

---

### 9-2. 해시 체인 정밀도 불일치

**문제:** Java `Instant`와 PostgreSQL `TIMESTAMPTZ`의 정밀도 차이로 해시 검증 실패

```
Java:       2025-03-14T10:30:00.123456789Z  (나노초, 9자리)
PostgreSQL: 2025-03-14T10:30:00.123456Z     (마이크로초, 6자리)
```

**해결:**
```java
// DB 저장 전 마이크로초로 자름
Instant ts = Instant.now().truncatedTo(ChronoUnit.MICROS);
auditLog.setTs(ts);
// 해시 계산에도 동일하게 적용
recordCore.put("ts", log.getTs().truncatedTo(MICROS).toString());
```

**학습:** 외부 시스템과 데이터를 교환할 때 항상 정밀도 차이를 확인해야 함.

---

### 9-3. payload_json 타입 변경 (JSONB → text)

**문제:** PostgreSQL이 JSONB 저장 시 키 순서를 변경하거나 공백을 제거하여 해시가 달라짐

```sql
-- JSONB: 입력값 재직렬화 (키 순서, 공백 변경 가능)
-- text: 정확한 원본 바이트 보존
```

**해결:** V11 마이그레이션으로 `payload_json` 컬럼 타입을 `text`로 변경

**학습:** 해시 입력 데이터는 재직렬화 없이 정확한 바이트를 보존해야 함.

---

### 9-4. 비관적 락과 독성 트랜잭션

**문제:** 전송 요청 승인/실행 시 동시 요청으로 인한 상태 불일치

**해결:**
```java
// 비관적 락으로 한 번에 하나의 트랜잭션만 처리
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<TransferRequest> findByIdForUpdate(UUID id);

// REQUIRES_NEW로 독성 트랜잭션 방지
@Transactional(propagation = Propagation.REQUIRES_NEW)
public TransferRequestResponse approve(UUID id) {
    TransferRequest tr = repo.findByIdForUpdate(id).orElseThrow(...);
    tr.approve(currentUser);
    // ...
}
```

**학습:** 비관적 락은 성능 비용이 있지만 금융 도메인처럼 정확성이 중요한 경우 적합.

---

### 9-5. AuditChainAnchor 직렬화 보장

**문제:** 해시 체인 앵커(`AuditChainAnchor`) 업데이트가 동시에 여러 스레드에서 실행될 때 체인 깨짐

**해결:**
```java
// 비관적 락으로 직렬화
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<AuditChainAnchor> findById(Long id);

// 모든 감사 로그 추가는 이 락을 통해서만
public void appendLog(AuditLog log) {
    AuditChainAnchor anchor = anchorRepo.findById(1L).orElseThrow();
    // prevHash = anchor.getLastHash()
    // 해시 계산 → 저장 → 앵커 업데이트
}
```

---

### 9-6. Spring AI MCP 순환 의존성

**문제:** `AnomalyMcpTools` → `AnomalyDetectionService` → `ChatModel` → Spring AI MCP가 다시 서비스를 참조

**해결:**
```java
@Service
public class AnomalyMcpTools {
    private final AnomalyDetectionService anomalyService;

    public AnomalyMcpTools(@Lazy AnomalyDetectionService anomalyService) {
        this.anomalyService = anomalyService;
    }
}
```

---

### 9-7. 통합 테스트와 불변 트리거 충돌

**문제:** 통합 테스트 setup에서 `audit_logs`를 초기화하려 할 때 PostgreSQL 트리거가 DELETE를 차단

**해결:**
```sql
-- 테스트 setup
ALTER TABLE audit_logs DISABLE TRIGGER audit_logs_immutable;
DELETE FROM audit_logs;
ALTER TABLE audit_logs ENABLE TRIGGER audit_logs_immutable;
```

**학습:** DB 수준 제약과 테스트 격리 전략 사이의 충돌을 항상 고려해야 함.

---

### 9-8. JPA Specification vs JPQL (null 파라미터)

**문제:** JPQL의 `WHERE col = :param` 은 `:param`이 null일 때 `col = null` 이 되어 아무 결과도 반환 안 됨

**해결:** JPA Specification으로 null 조건을 동적으로 구성
```java
Specification<AuditLog> spec = (root, query, cb) -> {
    List<Predicate> predicates = new ArrayList<>();
    if (action != null) predicates.add(cb.equal(root.get("action"), action));
    if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("ts"), from));
    // ...
    return cb.and(predicates.toArray(new Predicate[0]));
};
```

---

## 10. 실행 방법

### 사전 요구사항

- Java 17+
- Docker Desktop
- Gradle (Wrapper 포함)

### 로컬 개발 환경 시작

```bash
# 1. PostgreSQL 컨테이너 시작
docker run -d \
  --name consentledger-db \
  -e POSTGRES_DB=consentledger \
  -e POSTGRES_USER=consentledger \
  -e POSTGRES_PASSWORD=consentledger_pw \
  -p 5432:5432 \
  postgres:16

# 2. 환경 변수 설정
export JWT_SECRET=your-256-bit-secret-key
export OPENAI_API_KEY=sk-ant-...  # Claude API Key

# 3. 애플리케이션 실행 (Flyway 자동 마이그레이션)
./gradlew bootRun

# 4. Swagger UI 접근
open http://localhost:8080/swagger-ui/index.html
```

### 테스트 실행

```bash
# 단위 테스트 (Docker 불필요)
./gradlew test

# 통합 테스트 (Docker 필요)
./gradlew test -Dintegration.tests.enabled=true
```

### 시드 데이터 (자동 적용)

| 역할 | 이메일 | 비밀번호 |
|------|--------|----------|
| ADMIN | admin@consentledger.com | admin1234 |
| USER | user@consentledger.com | user1234 |

| 에이전트 | API Key |
|----------|---------|
| Agent-A | `test-api-key-agent-a` |

```bash
# JWT 토큰 발급 예시
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@consentledger.com","password":"admin1234"}'

# AI 이상 탐지 실행 (ADMIN 전용)
curl http://localhost:8080/admin/audit-logs/analyze?days=7 \
  -H "Authorization: Bearer <JWT_TOKEN>"

# 해시 체인 무결성 검증
curl http://localhost:8080/admin/audit-logs/verify \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

---

## 마무리

이 프로젝트를 통해 다음을 깊이 있게 경험했습니다.

**백엔드 아키텍처:**
- Spring Boot 3.3 / Spring Security 6의 최신 패턴 적용
- JPA Specification, 비관적 락, REQUIRES_NEW 트랜잭션 전략
- PostgreSQL JSONB, 트리거, TIMESTAMPTZ 마이크로초 정밀도

**보안:**
- JWT + API Key 이중 인증 파이프라인 구현
- SHA-256 해시 체인으로 로그 무결성 수학적 보장
- AI 입력 데이터 정제 (Prompt Injection 방지)

**AI 통합:**
- Spring AI 1.0.0 MCP 서버 구현 (SSE 기반)
- Claude AI를 활용한 보안 이상 탐지 자동화
- Graceful degradation 패턴

**테스트:**
- Testcontainers로 실제 DB를 활용한 통합 테스트
- Mockito RETURNS_DEEP_STUBS로 복잡한 AI 응답 체인 테스트
- DB 수준 불변 트리거와 테스트 격리 전략 조화
