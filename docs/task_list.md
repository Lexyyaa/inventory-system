# Task List

> 무엇을 어떤 순서로 하고, 어디까지 했는지만 적는다.
> 내용은 ID로 참조만 한다.
> - 결정: [01](design/01-analysis.md)
> - 요구사항: [02](design/02-requirements.md)
> - API: [04](design/04-api-spec.md)
> - TC: [05](design/05-test-cases.md)
>

**현재:** F2 / T2-3

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
| F2 입고 | +4:30 (20:00) | |
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
- [ ] T2-3 `feat: 입고 API 구현` — POST /api/v1/inventory/inbound · 수량 상한 설정 · ApiDocs · OpenApiConfig 제목 · 설명
- [ ] T2-4 `test: 입고 성공·실패 케이스` — TC-2-01 ~ TC-2-08
- [ ] T2-5 `test: 입고 엣지 케이스` — TC-2-09 ~ TC-2-10
- [ ] T2-6 `test: 입고 동시성` — TC-2-11 ~ TC-2-13
- [ ] T2-7 `test: F2 .http 실행 케이스`
- [ ] T2-8 `docs: F2 작업 로그`

## F3. 조회 `feature/query` — TC-3-01 ~ TC-3-04

> 리뷰: reviewer만 (verifier 생략)

- [ ] T3-1 `feat: 현재 재고 조회 API 구현` — GET /api/v1/inventory/{productCode} · ApiDocs
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
| F1 | reviewer | 낮음 | 코드 | `TransactionRunner` 주석의 "용도" 단락이 REPEATABLE READ 락 조회 용도를 설명한다 (이 과제는 쓰지 않음, "용도:트랜잭션" 띄어쓰기) | |
| F1 | reviewer | 확인 | 코드 | `X-Tenant-Id`에 제어 문자(NUL)가 오면 응답 형식이 `{code, message}`인지, 500이 나는지 미확인 — `.http` 실측 때 curl로 확인 | |

---

## 결정 필요 / 보류

<!--
진행 중에 생긴 것만 한 줄로 적는다.
결정되면 01에 ADR로 옮기고 여기서는 지운다.
-->

- README에 쓸 것 (T5-3)
  - 구현 범위: 02 §1을 요약해 소개
  - 확장 방향: 창고/로케이션별 재고, 예약 재고, 재고 이동 이력, 멱등성 (03에서 뺀 내용)
