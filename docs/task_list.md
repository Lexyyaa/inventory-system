# Task List

> 무엇을 어떤 순서로 하고, 어디까지 했는지만 적는다.
> 내용은 ID로 참조만 한다.
> - 결정: [01](design/01-analysis.md)
> - 요구사항: [02](design/02-requirements.md)
> - API: [04](design/04-api-spec.md)
> - TC: [05](design/05-test-cases.md)
>

**현재:** F3 / T3-2

## 규칙

- 기능(F) = 브랜치, 작업(T) = 커밋 1개
- 작업 줄의 백틱 안 문장이 **그대로 커밋 메시지**다
- 작업 순서: `feat`(구현) → `test`(성공·실패) → `test`(엣지·동시성)
- 줄 끝에는 이 작업이 구현하는 `TC`를 적는다
  - `TC-a-b ~ TC-a-c`는 양 끝을 포함한 연속 범위다
- 끝나면 `[x]`로 바꾸고 맨 위 **현재**를 다음 작업으로 옮긴다
- 05의 모든 `TC`는 아래 어딘가의 범위에 한 번 이상 들어가야 한다 (`verifier`가 검사)
- 기능 블록의 `.http 실행 케이스` 줄과 `작업 로그` 줄 (F1에는 작업 로그 줄만 있다)
  - `implementer`가 아니라 `/run-feature`가 처리한다

## 시간 계획

**시작:** 15:30
**제한:** 10:00

| 구간 | 목표 종료 | 실제 종료 |
|---|---|---|
| F0 설계 문서 · 점검 | +1:30 (17:00) | 16:49 |
| F1 기초 설정 | +2:30 (18:00) | 17:13 |
| F2 입고 | +4:30 (20:00) | 18:16 |
| F2 코드 리뷰 · 반영 | | 20:10 |
| F3 조회 | +5:15 (20:45) | |
| F4 출고 | +7:00 (22:30) | |
| F5 마무리 | +9:00 (00:30) | |
| 버퍼 | +10:00 (01:30) | |

---

## F0. 설계 `feature/design`

- [x] T0-1 `docs: 과제 원문 추가`
- [x] T0-2 `docs: 요구사항 분석 및 결정 기록`
- [x] T0-3 `docs: 요구사항 정의`
- [x] T0-4 `docs: 도메인 모델 및 ERD 작성`
- [x] T0-5 `docs: API 명세 및 에러 코드 작성`
- [x] T0-6 `docs: 테스트 케이스 작성`
- [x] T0-7 `docs: 작업 목록 작성`
- [x] T0-8 `chore: 작업 규칙 문서 추가`

## F1. 기초 설정 `feature/setup` — TC-1-01 ~ TC-1-04

> 리뷰: reviewer만 (verifier 생략)

- [x] T1-1 `chore: 사용하지 않는 공통 코드 제거` — BaseTimeEntity · DateRules · PageLimits와 테스트 · JpaConfig(@EnableJpaAuditing)
- [x] T1-2 `feat: 스키마 DDL 및 업체 seed 추가` — schema.sql(IF NOT EXISTS) · data.sql(ON CONFLICT) · ddl-auto validate · defer false · 프로파일의 ddl-auto 덮어쓰기 제거
- [x] T1-3 `feat: 에러 응답을 API 명세에 맞게 정비` — code 필드 · INVALID_REQUEST · 재고 에러 코드 · 검증/역직렬화 매핑 · Jackson 강제 변환 끔 · TransactionRunner 주석(이 과제의 입출고 흐름에는 쓰지 않음) · GlobalExceptionHandlerTest를 code · INVALID_REQUEST · 04 메시지로 갱신 · ErrorCode · ErrorResponse · GlobalExceptionHandler Javadoc 갱신
- [x] T1-4 `feat: 업체 엔티티 및 업체 헤더 해석 구현` — Tenant 엔티티 · TenantRepository(인터페이스 · Impl · JpaRepository) · Tenant ApplicationService · HandlerInterceptor
- [x] T1-5 `test: 업체 헤더 실패 케이스` — TC-1-01 ~ TC-1-02 (테스트 전용 엔드포인트)
- [x] T1-6 `test: DB 제약 조건 검증` — TC-1-03 ~ TC-1-04
- [x] T1-7 `docs: F1 작업 로그`

## F2. 입고 `feature/inbound` — TC-2-01 ~ TC-2-13

- [x] T2-1 `feat: 상품·재고 엔티티 구현`
- [x] T2-2 `feat: 상품 생성 및 재고 증가 원자 쿼리 구현`
- [x] T2-3 `feat: 입고 API 구현` — POST /api/v1/inventory/inbound · 수량 상한 설정 · ApiDocs · OpenApiConfig 제목 · 설명
- [x] T2-4 `test: 입고 성공·실패 케이스` — TC-2-01 ~ TC-2-08
- [x] T2-5 `test: 입고 엣지 케이스` — TC-2-09 ~ TC-2-10
- [x] T2-6 `test: 입고 동시성` — TC-2-11 ~ TC-2-13
- [x] T2-7 `test: F2 .http 실행 케이스`
- [x] T2-8 `docs: F2 작업 로그`

> 코드 리뷰 반영 (사용자 리뷰 합의 14건)

- [x] T2-9 `refactor: 웹 계층 패키지와 API 경로 접두사 정리` — interceptor · support/config/WebConfig · @RequestAttribute · API_PREFIX · ArchUnit support 제외
- [x] T2-10 `refactor: 입고를 도메인 서비스로 분리` — ProductService · InventoryService · 컨트롤러 세 줄 · toCommand · requireNonNull 삭제
- [x] T2-11 `refactor: 재고 변경 결과를 InventoryState로 받기` — InventorySnapshot · ChangedRow 통합, Instant
- [x] T2-12 `feat: 모든 테이블에 생성·변경 시각 추가` — schema.sql 컬럼 3개 · 매핑 전용 BaseTimeEntity
- [x] T2-13 `chore: 쓰지 않는 TransactionRunner 삭제`
- [x] T2-14 `style: 주석 정리` — OpenApiConfig 제목 · 버전만
- [x] T2-15 `docs: 코드 리뷰 합의 규칙 반영` — CLAUDE.md 3개 · 체크리스트 · 03 시각 컬럼
- [x] T2-16 `docs: F2 코드 리뷰 작업 로그`
- [x] T2-17 `refactor: 업체 확인을 도메인 서비스로 분리` — TenantService
- [x] T2-18 `refactor: 수량 검사를 재고 엔티티로 옮김` — repository가 필요 없는 규칙은 엔티티 · 주석 자리 규칙

## F3. 조회 `feature/query` — TC-3-01 ~ TC-3-04

> 리뷰: reviewer만 (verifier 생략)

- [x] T3-1 `feat: 현재 재고 조회 API 구현` — GET /api/v1/inventory/{productCode} · ApiDocs
- [ ] T3-2 `test: 재고 조회 성공·실패 케이스` — TC-3-01 ~ TC-3-03
- [ ] T3-3 `test: 재고 조회 엣지 케이스` — TC-3-04
- [ ] T3-4 `test: F3 .http 실행 케이스`
- [ ] T3-5 `docs: F3 작업 로그`

## F4. 출고 `feature/outbound` — TC-4-01 ~ TC-4-09

- [ ] T4-1 `feat: 재고 조건부 차감 쿼리 구현`
- [ ] T4-2 `feat: 출고 API 구현` — POST /api/v1/inventory/outbound · ApiDocs
- [ ] T4-3 `test: 출고 성공·실패 케이스` — TC-4-01 ~ TC-4-04
- [ ] T4-4 `test: 출고 엣지 케이스` — TC-4-05 ~ TC-4-06
- [ ] T4-5 `test: 출고 동시성` — TC-4-07 ~ TC-4-09
- [ ] T4-6 `test: F4 .http 실행 케이스`
- [ ] T4-7 `docs: F4 작업 로그`

## F5. 마무리 `feature/docs`

- [ ] T5-1 `docs: .http 실행 케이스 정리`
- [ ] T5-2 `chore: 사용하지 않는 파일 정리`
- [ ] T5-3 `docs: README 작성 (실행 방법 · 기술 스택과 선택 이유 · 설계 결정 · API · DDL 위치 · 한계와 확장 방향)`
- [ ] T5-4 `docs: AI 활용 내역 정리`

---

## 리뷰 백로그

<!--
/run-feature 리뷰·검증에서 나온 중간 · 낮음 지적을 한 줄씩 쌓는다.
결정 없이 적기만 하고, /wrap-up에서 한 번에 처리한다.
쪽: 코드 / 테스트 / 문서. 처리: 고침 (SHA) / README 한계
-->

| F | 출처 | 심각도 | 쪽 | 내용 | 처리 |
|---|---|---|---|---|---|
| F1 | reviewer | 확인 | 코드 | `X-Tenant-Id`에 제어 문자(NUL)가 오면 응답 형식이 `{code, message}`인지, 500이 나는지 미확인 — `.http` 실측 때 curl로 확인 | F2 실측: Tomcat이 Spring 전에 HTML 400으로 막음 (500 아님) → README 한계 후보 |
| F2 | reviewer | 낮음 | 문서 | 03 §9는 "RETURNING이 비면 재조회"인데 구현은 항상 재조회 (결과 같음, 신규 상품일 때 SELECT 1회 추가) | F2 리뷰 반영에서 03 §9 문구를 구현에 맞춤 |
| F2 | reviewer | 낮음 | 문서 | native `timestamptz`가 Hibernate 6.6에서 `Instant`로 와서 RETURNING을 인프라 projection으로 받음 — src/main/CLAUDE.md RETURNING 규칙 문구와 다름 | 고침 (9427b3a): `InventoryState(Instant)`로 바로 받음 |
| F2 | reviewer | 낮음 | 테스트 | 05에 없는 테스트 3건(수량 양 끝값, NUL · 짝 없는 서로게이트, 이모지 255/256자)이 코드에만 있음 — 05 추가 제안 | |
| F2 | verifier | 중간 | 테스트 | 상품코드 허용 문자(`A 001` 등) 위반 TC 없음 — `@Pattern`을 지워도 F2 테스트가 통과 | |
| F2 | verifier | 중간 | 테스트 | 04 §2 순서 조합 "필수값 → 수량", "수량 → 상품 상태" TC 없음 | |
| F2 | verifier | 중간 | 문서 | 02 §3 · 04 §3 "1~255자"의 세는 단위 미정 | F2 리뷰 반영에서 "글자(문자) 단위" 명시 |
| F2 | verifier | 중간 | 테스트 | "정의되지 않은 필드 무시" TC 없음, Boot 기본값에 기대고 yml 명시 없음 | |
| F2 | verifier | 낮음 | 문서 | 04 §3에 길이 · 허용 문자 위반 문구가 없어 101자 상품코드에도 "필수 요청 정보가 누락되었습니다." | |
| F2 | reviewer · verifier | 확인 | 문서 | `@NotBlank`는 U+0020 이하만 공백으로 봄 — 전각 공백(U+3000) · NBSP만인 상품명은 통과. 02 §3 "공백"의 범위 미정 | |
| F2 | verifier | 낮음 | 테스트 | 상품코드 대소문자 구분(A001 · a001 별개) TC 없음 | |
| F2 | verifier | 낮음 | 테스트 | updatedAt을 오프셋만 단언하고 DB `updated_at` 값과 대조하지 않음 | |
| F2 | verifier | 낮음 | 테스트 | 수량 상한이 설정값에서 오는지 검증하지 않음 (하드코딩해도 통과) | |
| F2 | verifier | 낮음 | 테스트 | 동시성 TC가 실제 경합을 보장하지 않음 (스레드 3개) | |
| F2 | reviewer | 확인 | 문서 | Long 범위를 넘는 quantity는 400 `INVALID_REQUEST`(형식) — 02 §8 "상한 초과 → INVALID_QUANTITY"와 04 §2 "타입 → INVALID_REQUEST" 중 어느 쪽인지 문서 미정 (현재 동작은 04 §2) | |
| F2 | verifier | 낮음 | 코드 | F4 선행: `InventoryException`에 detail 생성자 없음 — 출고 문구를 넘기려면 추가 필요 | |
| F2 | verifier | 낮음 | 코드 | 상품명 `@CodePointLength`는 springdoc이 읽지 않아 Swagger 스키마에 `maxLength: 255`가 빠졌을 수 있음 (미확인) — 필요하면 `@Schema(maxLength = 255)` | |

---

## 결정 필요 / 보류

<!--
진행 중에 생긴 것만 한 줄로 적는다.
결정되면 01에 ADR로 옮기고 여기서는 지운다.
-->

- README에 쓸 것 (T5-3)
  - 구현 범위: 02 §1을 요약해 소개
  - 확장 방향: 창고/로케이션별 재고, 예약 재고, 재고 이동 이력, 멱등성 (03에서 뺀 내용)
  - 한계
    - DB 이식성: 원자 SQL이 PostgreSQL 문법(`ON CONFLICT`, `RETURNING`)이라 DB를 바꾸면 세 쿼리를 다시 써야 함
    - `X-Tenant-Id`에 제어 문자(NUL)가 오면 Tomcat이 Spring 전에 HTML 400으로 막음 (`{code, message}` 형식 아님)
    - 재고 합이 BIGINT 범위를 넘으면 500 (한 건 상한 10억이라 약 92억 번 입고해야 생김)
