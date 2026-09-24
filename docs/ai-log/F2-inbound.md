# F2. 입고 `feature/inbound`

## 받은 지시

- `/run-feature F2`
- 범위: `task_list` F2의 T2-1 ~ T2-6 구현
- T2-7(`.http`) · T2-8(작업 로그)은 메인 세션이 처리
- 브랜치: `feature/inbound`
- 사용자 위임
  - 설계를 바꾸지 않는 작은 결정은 추천안으로 적용
  - 적용한 결정은 보고와 ai-log에 남김

## 작업 흐름

1. implementer 구현
   - 소요: 약 20분
   - 중간 멈춤 없음
   - T2-1 `65775e7` `feat: 상품·재고 엔티티 구현`
   - T2-2 `5245d9a` `feat: 상품 생성 및 재고 증가 원자 쿼리 구현`
   - T2-3 `bed543f` `feat: 입고 API 구현`
   - T2-4 `e92bd9a` `test: 입고 성공·실패 케이스`
   - T2-5 `8abf5cc` `test: 입고 엣지 케이스`
   - T2-6 `1113bf2` `test: 입고 동시성`
2. reviewer · verifier 동시 실행
   - verifier 모델: Sonnet
3. reviewer 높음 1건 → implementer 수정
   - `94a5d20` `fix: 상품명 길이를 문자 단위로 검사`
4. 수정분 재리뷰 · 재점검
   - reviewer 높음 0건
   - verifier 높음 0건
5. 서버 실측 (`.http` 10건)
   - 수정 전 1회
   - 수정 후 1회
   - T2-7 `b06ab05` `test: F2 .http 실행 케이스` (메인 세션)
6. 작업 로그 작성 (T2-8, 메인 세션)

## 바뀐 파일

- 상품 (main: `src/main/java/com/deepfine/inventorysystem/`)
  - `domain/product/Product.java` (생성) — 상품 엔티티
    - 정적 팩토리 `create`
    - 상품명 정확 일치 검사 `validateName`
    - `@UniqueConstraint` (매핑 문서용)
  - `domain/product/ProductRepository.java` (생성) — 도메인 저장소 인터페이스
  - `domain/product/exception/ProductException.java` (생성) — 상품 도메인 예외
  - `infrastructure/persistence/product/ProductJpaRepository.java` (생성) — Spring Data 저장소
  - `infrastructure/persistence/product/ProductRepositoryImpl.java` (생성) — 03 §9 SQL
    - `ON CONFLICT DO NOTHING RETURNING id`
- 재고 (main: `src/main/java/com/deepfine/inventorysystem/`)
  - `domain/inventory/Inventory.java` (생성) — 재고 엔티티
    - 입고 수량 검사 `validateInboundQuantity(quantity, maxQuantity)`
  - `domain/inventory/InventoryRepository.java` (생성) — 도메인 저장소 인터페이스
  - `domain/inventory/InventorySnapshot.java` (생성) — RETURNING 결과 record
  - `domain/inventory/exception/InventoryException.java` (생성) — 재고 도메인 예외
  - `infrastructure/persistence/inventory/InventoryJpaRepository.java` (생성) — Spring Data 저장소
    - RETURNING 결과 projection `ChangedRow`
  - `infrastructure/persistence/inventory/InventoryRepositoryImpl.java` (생성) — 03 §10 UPSERT
    - `GREATEST(updated_at, clock_timestamp())`
    - `RETURNING quantity, updated_at AS updatedAt`
- 입고 API (main: `src/main/java/com/deepfine/inventorysystem/`)
  - `application/inventory/InventoryApplicationService.java` (생성) — 입고 조율
  - `application/inventory/InventoryCommand.java` (생성) — `InventoryCommand.Inbound.of`
  - `application/inventory/InventoryInfo.java` (생성) — `InventoryInfo.Inbound`
  - `presentation/inventory/InventoryController.java` (생성) — `POST /api/v1/inventory/inbound`
  - `presentation/inventory/InventoryApiDocs.java` (생성) — Swagger 문서 인터페이스
  - `presentation/inventory/InventoryRequest.java` (생성) — 입고 요청 DTO
  - `presentation/inventory/InventoryResponse.java` (생성) — `InventoryResponse.Inbound`
- 설정
  - `support/properties/InventoryProperties.java` (생성) — 수량 상한 설정
  - `support/config/OpenApiConfig.java` (수정) — 제목 · 설명
  - `InventorySystemApplication.java` (수정) — `@ConfigurationPropertiesScan`
  - `src/main/resources/application.yml` (수정) — `inventory.max-quantity: 1000000000`
- 테스트 (test: `src/test/java/com/deepfine/inventorysystem/`)
  - `support/InventoryTestDb.java` (생성) — JDBC 헬퍼
  - `domain/product/ProductTest.java` (생성) — TC-2-05
  - `presentation/inventory/InboundApiTest.java` (생성) — TC-2-01 ~ TC-2-04 · TC-2-06 ~ TC-2-09
    - TC 번호 없는 테스트 3건 포함
  - `presentation/inventory/InboundRollbackTest.java` (생성) — TC-2-10
    - `@MockitoSpyBean` 사용
  - `presentation/inventory/InboundConcurrencyTest.java` (생성) — TC-2-11 ~ TC-2-13
- 실측
  - `http/inbound.http` (생성) — 입고 실측 10건
- 문서
  - `docs/task_list.md` (수정) — T2 체크 · **현재** 갱신 · 리뷰 백로그

## 설계대로 한 것

- 입고 흐름: 트랜잭션 하나 (03 §8 · §9 · §10 · §14)
  1. 상품 생성 시도
  2. 같은 트랜잭션에서 상품 재조회
  3. 상품명 검증
  4. 재고 UPSERT
- 쓰지 않은 것
  - `FOR UPDATE`
  - `REQUIRES_NEW`
  - 격리 수준 상향
- 수량 범위 검사
  - 서비스가 설정 상한을 읽어 도메인에 넘김
  - 검사는 도메인에서
  - 상품 INSERT보다 먼저
- Request `quantity` 검증은 `@NotNull`만
- 응답 값의 출처
  - 수량 · 변경 시각: RETURNING 값
  - 상품코드 · 상품명: 재조회한 `Product` 값
- Swagger
  - 애너테이션은 `InventoryApiDocs`에만
  - 매핑에 `consumes` · `produces` 없음
- 동시성 TC
  - 스레드 수: 05 기준
  - 실행: `ConcurrencyRunner`
  - 성공 · 실패 집계: MockMvc 상태 코드
  - 판정 식 확인: DB 재조회

## 설계에 없어서 정한 것

- 상품 재조회 조건
  - 정한 것: RETURNING 결과와 관계없이 항상 재조회
  - 다른 선택지: RETURNING이 비었을 때만 재조회
  - 이유: 신규 · 기존 상품이 같은 검증 경로를 탐
  - 이유: 응답이 항상 저장된 값
  - 대가: 신규 상품일 때 SELECT 1회 추가
- 상품 생성 의도를 넘기는 방식
  - 정한 것: 운영 코드도 `Product.create`로 만든 객체를 저장소에 넘김
  - JPA로 저장하지는 않음 (저장은 03 §9 SQL)
  - 다른 선택지: 저장소가 인자 3개를 받고 팩토리는 테스트에서만 사용
- 수량 검사 위치
  - 정한 것: `Inventory`의 정적 메서드
  - 값 객체(VO)는 두지 않음
- 수량 상한 설정 이름
  - 정한 것: `inventory.max-quantity`
  - 근거: 상시 결정 "정책 수치"
- 상품명 허용 문자
  - 정한 것: `@Pattern("[^\u0000\uD800-\uDFFF]*")` → 위반 시 400 `INVALID_REQUEST`
  - 근거: 상시 결정 "DB 컬럼 범위"
  - 계기 (임시 확인): NUL이 들어오면 500
  - 계기 (임시 확인): 짝 없는 서로게이트는 다른 문자로 저장되어 신규 상품인데 409
- Swagger 문서
  - 스키마 이름 충돌 → `@Schema(name)`으로 `InboundRequest` · `InboundResponse` 분리
  - `tenantId` 파라미터는 hidden
  - 400은 에러 코드별 예시 여러 개
  - 500도 문서화
- DTO 이름
  - `InventoryInfo.Inbound`
  - `InventoryResponse.Inbound`
  - `InventoryCommand.Inbound.of`
- `Product`의 `@UniqueConstraint`
  - 정한 것: 매핑 문서용으로 둠

## 설계와 다르게 간 것

- RETURNING 결과를 도메인 record로 바로 받지 않음
  - 원인: Hibernate 6.6이 native 쿼리의 `timestamptz`를 `Instant`로 읽음
  - 증상: `InventorySnapshot(OffsetDateTime)` 생성이 "argument type mismatch"로 실패
  - 바꾼 방식: 인프라 projection `InventoryJpaRepository.ChangedRow`로 받음
  - 바꾼 방식: `InventoryRepositoryImpl`이 UTC `OffsetDateTime`으로 바꿔 도메인 record 반환
  - `+09:00` 표기는 Jackson `time-zone` 설정이 담당
  - 다른 선택지: `@SqlResultSetMapping` + `@NativeQuery`
  - 다른 선택지: 도메인 record에 `Instant`
  - reviewer · verifier 판단: 둘 다 받아들임
    - 도메인 · application은 `OffsetDateTime`만 봄
    - 성공 응답 테스트가 모두 오프셋을 단언

## 리뷰 · 검증

- 1차 결과
  - reviewer: 높음 1건 · 낮음 3건
  - verifier (Sonnet): 중간 4건 · 낮음 9건
- 재리뷰 · 재점검 (수정분 `94a5d20`)
  - reviewer: 높음 0건
  - verifier: 높음 0건 · 낮음 1건
- 상품명 길이 단위 불일치
  - 지적: `@Size(max = 255)`는 UTF-16 단위로 셈
  - 지적: DB `VARCHAR(255)`는 문자 단위로 셈
  - 영향: 이모지 128 ~ 255자 상품명이 DB 범위 안인데 400
  - 심각도: 높음 (reviewer)
  - 처리: 상시 결정 "DB 컬럼 범위"라 묻지 않고 수정
  - 고침 SHA: `94a5d20` — `@CodePointLength(max = 255)`로 교체
  - 테스트 추가: 이모지 255자 → 200
  - 테스트 추가: 이모지 256자 → 400
  - 확인: 수정을 되돌리면 새 테스트가 실패
- 나머지 지적 (낮음 · 중간 · 재점검 낮음)
  - 처리: `docs/task_list.md` 리뷰 백로그
  - 항목별 원문은 백로그의 F2 행
- 백로그로 간 것 — TC가 없는 경우
  - 상품코드 허용 문자 위반
  - 04 §2 판정 순서 조합
  - 정의되지 않은 필드 무시
  - 상품코드 대소문자 구분
- 백로그로 간 것 — 문서
  - 상품명 · 상품코드 길이를 세는 단위
  - 02 §3 "공백"의 범위
  - 03 §9 재조회 문구와 구현의 차이
  - 04 §3 길이 위반 문구
- 백로그로 간 것 — 코드
  - F4 선행: `InventoryException`에 detail 생성자 없음
  - Swagger 스키마의 상품명 `maxLength` 누락 가능성 (미확인)
- F2 리뷰 반영에 넣을 것 (사용자 위임)
  - 03 §9 재조회 문구를 구현에 맞춤
  - `src/main/CLAUDE.md` RETURNING 규칙에 추가: native `timestamptz`는 `Instant` → 인프라 projection
  - 02 §3 · 04 §3에 "길이는 문자 단위" 명시
  - 구현 체크리스트 3줄 (아래 "자주 틀리는 것 후보")
- 05 TC 추가 제안 (F2 엣지)
  - 수량 1 · 1,000,000,000은 받는다
  - 상품명 NUL · 짝 없는 서로게이트는 400
  - 상품명 이모지 255자 200 · 256자 400
  - 세 건 모두 코드에는 TC 번호 없이 이미 있음

## 실행 확인

- `./gradlew clean spotlessApply build` → 성공
  - 구현 직후: 테스트 42개 통과 · 실패 0
  - 수정(`94a5d20`) 후: 테스트 43개 통과 · 실패 0
  - 이 수에 F2 TC-2-01 ~ TC-2-13 테스트가 들어 있음
- 거짓 통과 점검 (운영 코드를 망가뜨린 뒤 되돌림)
  - UPSERT를 읽고-계산-쓰기로 바꿈 → TC-2-11 · TC-2-12 실패
  - `ON CONFLICT`를 조회 후 INSERT로 바꿈 → TC-2-12 · TC-2-13 실패
  - `@Transactional` 제거 → TC-2-10 실패
  - 상품명 비교 완화 → TC-2-05 실패
  - 상한 비교를 `>=`로 바꿈 → 양 끝값 테스트만 실패
  - 수량 검사 삭제 → TC-2-06 실패
  - 수정(`94a5d20`)을 되돌림 → 새로 추가한 이모지 길이 테스트 실패
- `.http` 실측 → `http/inbound.http` 10건 모두 기대와 일치 (상태 · 본문 · DB)
  - 준비: DB 초기화 후 `./gradlew bootRun`
  - 수정 전 1회 · 수정 후 1회 실행
  - 서버 로그 ERROR 0건
- `.http` 케이스별 결과
  - `[TC-2-01]` 신규 상품 입고 → 200
    - 본문: `productCode` IN-A001 · `productName` Apple · `quantity` 10
    - DB: tenant-001 IN-A001 재고 10
  - `[TC-2-02]` 기존 상품 입고 → 200
    - 본문: `quantity` 40
    - DB: 재고 40
    - DB: tenant-001 IN-A001 상품 1건
  - `[TC-2-03]` 다른 업체의 같은 상품코드 → 200
    - 본문: `productName` Samsung · `quantity` 5
    - DB: tenant-002 IN-A001 재고 5
    - DB: tenant-001 IN-A001 재고 40 유지
  - `[TC-2-04]` 상품명 불일치 → 409
    - 본문: `code` PRODUCT_NAME_MISMATCH
    - DB: 재고 40 유지
    - DB: 상품명 Apple 유지
  - `[TC-2-06]` 수량 0 → 400
    - 본문: `code` INVALID_QUANTITY
    - DB: IN-Q0 상품 0건
  - `[TC-2-06]` 수량 상한 초과(1,000,000,001) → 400
    - 본문: `code` INVALID_QUANTITY
    - DB: IN-QMAX 상품 0건
  - `[TC-2-07]` `productCode` 누락 → 400
    - 본문: `code` INVALID_REQUEST
    - 본문: `message` "필수 요청 정보가 누락되었습니다."
  - `[TC-2-08]` 정수가 아닌 수량(1.5) → 400
    - 본문: `code` INVALID_REQUEST
    - 본문: `message` "요청 형식이 올바르지 않습니다."
    - DB: IN-FLOAT 상품 0건
  - `[TC-2-08]` 헤더 없음 + 깨진 JSON → 400
    - 본문: `code` INVALID_TENANT (업체 확인이 먼저)
  - `[TC-1-02]` 등록되지 않은 업체 → 400
    - 본문: `code` INVALID_TENANT
    - DB: IN-T999 상품 0건
- Swagger
  - Swagger UI → 200
  - api-docs 제목 → "재고 관리 시스템 API"
  - api-docs 경로 → `/api/v1/inventory/inbound` 노출
- 추가 확인 (F1 백로그 "확인" 항목)
  - `X-Tenant-Id`에 제어 문자 → Tomcat이 Spring 전에 HTML 400으로 막음
  - 500은 아님
  - 본문 형식은 `{code, message}`가 아님
  - README 한계 후보로 둠

## 에러와 해결

- RETURNING → record 변환 실패
  - 에러: "Cannot instantiate query result type InventorySnapshot: argument type mismatch"
  - 원인: native `timestamptz`가 `Instant`로 옴
  - 해결: 인프라 projection으로 받은 뒤 변환 ("설계와 다르게 간 것")
- Swagger 스키마 이름 충돌
  - 해결: `@Schema(name)`으로 이름 분리
- 상품명 NUL → 500 · 짝 없는 서로게이트 → 409
  - 해결: `@Pattern`으로 400 `INVALID_REQUEST`
- 게이트(`./gradlew spotlessApply build`) 재실패 없음

## 자주 틀리는 것 후보

- 문자열 길이는 DB처럼 코드포인트로 센다
  - `@Size`가 아니라 `@CodePointLength`
- PostgreSQL 문자열에 저장할 수 없는 값은 Request에서 막는다
  - NUL
  - 짝 없는 서로게이트
- native 쿼리의 `timestamptz`는 Hibernate 6.6에서 `Instant`로 온다
  - 인프라 projection으로 받아 변환한다
- 대상: `.claude/checklists/implementation.md`
- 반영: 사용자 위임으로 F2 리뷰 반영에 넣음
