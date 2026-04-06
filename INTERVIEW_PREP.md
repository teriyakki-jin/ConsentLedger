# ConsentLedger 프로젝트 면접 경험 정리

## 1. 프로젝트 한 줄 소개

> 개인정보보호법상 **개인정보 전송요구권(마이데이터)**의 동의 관리·감사 흐름을 검증하는 Spring Boot 백엔드 참조 구현입니다.

---

## 2. 기술 스택

| 영역 | 사용 기술 |
|------|----------|
| Language | Java 17 |
| Framework | Spring Boot 3.3, Spring Security, Spring Data JPA, Flyway |
| AI / MCP | Spring AI 1.0.0, Claude Sonnet (Anthropic) |
| DB | PostgreSQL 16 |
| Frontend | React 19, TypeScript, Vite, Zustand |
| Infra | Docker Compose, GitHub Actions CI |
| Docs | springdoc-openapi (Swagger UI) |

---

## 3. 핵심 구현 포인트

### 3-1. SHA-256 해시 체인 감사 로그

**문제 인식:** 감사 로그는 삭제·위변조 시 추적이 불가능해야 한다.

**해결 방법:**
- 각 로그 행마다 `hash = SHA256(prev_hash + record_core_json)` 으로 체인 형성
- `audit_chain_anchor` 테이블에 마지막 해시를 저장하고 **비관적 락(`SELECT FOR UPDATE`)** 으로 직렬화 → 동시성 환경에서도 해시 순서 보장
- DB 트리거(`audit_logs_immutable`)로 `DELETE/UPDATE` 자체를 원천 차단
- `GET /admin/audit-logs/verify` 로 전체 체인을 배치(1,000건)로 순회·검증 (JPA 1차 캐시 `entityManager.clear()` 로 OOM 방지)

**면접 답변 포인트:**
- "왜 비관적 락?" → 해시가 이전 값에 의존하므로 순서가 틀리면 전체 체인이 깨집니다. 낙관적 락은 재시도 시 hash 충돌 가능성이 있어 비관적 락을 선택했습니다.
- "PostgreSQL 마이크로초 정밀도 이슈" → `Instant.now().truncatedTo(MICROS)` 로 절삭하지 않으면 Java 나노초와 DB 저장값이 달라 재계산 해시가 불일치하는 버그가 있었습니다.

---

### 3-2. 이중 인증 (JWT + API Key)

- **사용자 인증:** JWT Bearer 토큰 (`Authorization: Bearer <token>`)
- **Agent 인증:** `X-API-Key` 헤더 (DB에는 SHA-256 해시만 저장, raw key 미보관)
- Spring Security `OncePerRequestFilter` 를 두 개 체이닝 → 토큰 종류에 따라 `UsernamePasswordAuthenticationToken` / `ApiKeyAuthenticationToken` 으로 분기
- RBAC: `USER`, `ADMIN`, `AGENT` 3가지 역할

---

### 3-3. 전송 요청 상태 머신 (State Machine)

```
REQUESTED → APPROVED → COMPLETED
         ↓          ↓
        DENIED     FAILED
```

- `TransferStateMachine` 컴포넌트에 허용 전이 Map을 선언적으로 정의
- 불가능한 전이 시도 시 즉시 `BusinessException` 발생
- **멱등성(Idempotency):** `REQUIRES_NEW` 트랜잭션으로 이미 승인된 요청에 대한 중복 호출을 안전하게 처리

---

### 3-4. Spring AI MCP 서버 통합

- `GET /sse` 엔드포인트로 Claude Desktop 등 AI 클라이언트와 SSE 연결
- `@Tool` 어노테이션 기반으로 6개 도구 등록: `getAuditLogs`, `verifyAuditChain`, `getConsentsByUser`, `getTransferRequests`, `listUsers`, `analyzeAnomalies`
- Admin JWT 인증 후에만 SSE 접근 가능

---

### 3-5. AI 이상 탐지 (Claude Sonnet 연동)

**기능:** 최근 N일간 감사 로그를 Claude에게 전달 → 4가지 이상 패턴 분석

| 패턴 | 설명 |
|------|------|
| ABNORMAL_HOURS | 비업무시간(22:00-06:00 UTC) / 주말 행위 |
| PRIVILEGE_ABUSE | 과도한 관리자 작업, 벌크 오퍼레이션 |
| DATA_EXFILTRATION | 단시간 대량 전송, 비정상 접근 패턴 |
| ACCOUNT_TAKEOVER | 새 IP 로그인, 실패 후 성공, 동시 세션 |

**보안 고려사항:**
- LLM에 전달하는 로그에서 `payloadJson` 제거 (개인정보 보호)
- 모든 문자열 필드 **Prompt Injection 방지 sanitize** (제어문자·개행 제거, 200자 제한)
- 분석 건수 상한(`max-logs-per-analysis: 500`) 설정 → 토큰 남용 방지
- AI 호출 실패 시 graceful degradation (빈 findings + `ANALYSIS_FAILED` 상태 반환)

---

### 3-6. 테스트 전략

- **단위 테스트 99개** (`@ExtendWith(MockitoExtension.class)`)
- **통합 테스트** (`@SpringBootTest + Testcontainers` PostgreSQL, `-Dintegration.tests.enabled=true`)
- **CI:** GitHub Actions (PR마다 자동 실행)

---

## 4. 트러블슈팅 경험 (STAR 형식)

### T1. 해시 불일치 버그

- **Situation:** 해시 체인 검증이 로컬에서는 통과하는데 DB 재시작 후 실패
- **Task:** 해시가 저장·재계산 간에 달라지는 원인 파악
- **Action:** `Instant` 의 나노초 정밀도 vs PostgreSQL 마이크로초 정밀도 차이 확인 → `.truncatedTo(ChronoUnit.MICROS)` 추가
- **Result:** 체인 검증 100% 통과

### T2. 동시 요청 시 해시 체인 순서 보장

- **Situation:** 부하 테스트에서 두 로그의 `prev_hash`가 동일한 현상 발생
- **Task:** 감사 로그 기록의 직렬화 필요
- **Action:** `audit_chain_anchor` 단일 행에 `SELECT FOR UPDATE` 비관적 락 적용
- **Result:** 동시성 환경에서도 해시 체인 연속성 보장

### T3. 독성 트랜잭션(Poisoned Transaction) 방지

- **Situation:** 승인 중 예외 발생 시 감사 로그까지 롤백되어 기록이 사라지는 문제
- **Task:** 감사 로그는 성공·실패 무관하게 항상 기록되어야 함
- **Action:** `REQUIRES_NEW` 전파 속성으로 감사 로그 트랜잭션 분리
- **Result:** 비즈니스 로직 실패 시에도 감사 기록 보존

---

## 5. "어떤 점을 배웠나요?" 답변 예시

> "데이터 무결성을 코드로 보장하는 것과 DB 제약으로 보장하는 것의 차이를 직접 경험했습니다. 해시 체인은 위변조 감지를 위한 것이고, DB 트리거는 실수 또는 직접 쿼리로 인한 삭제를 막기 위한 것으로 서로 다른 위협 모델을 대응합니다. 또한 LLM을 프로덕션에 연동할 때 Prompt Injection 방지, 토큰 비용 제어, 응답 파싱 실패에 대한 graceful degradation이 모두 필수적이라는 것을 배웠습니다."

---

## 6. 자주 나올 질문 & 예상 답변 요점

| 질문 | 핵심 답변 |
|------|----------|
| 왜 JPA + Flyway 조합? | 엔터티 변경과 스키마 변경을 독립적으로 버전 관리하기 위해. 팀 협업 시 마이그레이션 충돌 방지 |
| 비관적 락 vs 낙관적 락 선택 기준 | 해시 체인처럼 순서가 결과에 영향을 미치는 경우엔 비관적 락. 충돌 빈도 낮고 재시도 허용되면 낙관적 락 |
| Spring AI MCP를 선택한 이유 | Claude Desktop 등 표준 MCP 클라이언트와 zero-config 연동 가능. 직접 HTTP 클라이언트 구현 대비 도구 등록/스키마 자동화 |
| RBAC 설계 | USER(자기 데이터 조회·동의), ADMIN(전체 감사·관리), AGENT(API Key로 전송 요청 실행) — 최소 권한 원칙 |
| 테스트에서 가장 어려웠던 점 | Spring Security 컨텍스트 수동 설정. `@WithMockUser`는 커스텀 `UserDetails` 와 호환 안 돼서 `SecurityContextHolder`를 직접 셋팅 |
| API Key는 왜 해시로만 저장? | 유출 시 원문 복원 불가. HMAC 대신 SHA-256 단방향 해시로 저장하고 요청마다 해싱 후 비교 |
| 이상 탐지에서 payloadJson을 왜 제외? | 개인정보(동의 내용, 전송 데이터 등)가 포함될 수 있어 외부 LLM API 전송 시 개인정보 최소화 원칙 적용 |
