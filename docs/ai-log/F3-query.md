# F3. 조회 `feature/query`

## 받은 지시

- `/run-feature F3`
- 브랜치: `feature/query`
- 범위: T3-1 ~ T3-3 구현
- T3-4(`.http`), T3-5(작업 로그)는 메인 세션

## 작업 흐름

1. 구현 전 설계 변경
   - `6e01619` 03 §13 조회를 두 번 조회로
2. implementer 구현
   - T3-1 `6f0fbb8` `feat: 현재 재고 조회 API 구현`
   - T3-2 `f308eda` `test: 재고 조회 성공/실패 케이스`
   - T3-3 `7f7a20c` `test: 재고 조회 엣지 케이스`
3. F2 코드와 규칙 조정
   - `5e17ae1` F2 작업 로그에 `TenantService` 추가
   - `7994cfe` 수량 범위 검사를 `Inventory`로
   - `a3f23ab` TC-2-14 추가
   - `c7ac256` 05 F3/F4 given을 DB 직접 삽입으로
   - `5e66c0f` 스킬과 doc-writer 지시에서 작업 시간 기록 삭제
4. reviewer (verifier 생략)
   - `0540620` task_list 시간 계획 표와 시간 목표 삭제
5. 서버 실측
   - T3-4 `6f10c0d` `test: F3 .http 실행 케이스`
6. TC 추가
   - T3-6 `eb31115` `test: 조회 상품코드 형식/대소문자 케이스`
7. 작업 로그 (T3-5)

## 바뀐 파일

- 조회 API (main: `src/main/java/com/deepfine/inventorysystem/`)
  - `presentation/controller/inventory/InventoryController.java` (수정) — `GET /api/v1/inventory/{productCode}`
  - `presentation/controller/inventory/InventoryApiDocs.java` (수정) — 조회 Swagger 문서
  - `presentation/controller/inventory/InventoryResponse.java` (수정) — `CurrentStock.from`
  - `application/inventory/InventoryApplicationService.java` (수정) — `getCurrentStock`
  - `application/inventory/InventoryCommand.java` (수정) — `CurrentStock`
  - `application/inventory/InventoryInfo.java` (수정) — `CurrentStock.of(product, inventory)`
  - `domain/inventory/InventoryService.java` (수정) — `get`
  - `domain/inventory/InventoryRepository.java` (수정) — `findByProductId`
  - `infrastructure/persistence/inventory/InventoryRepositoryImpl.java` (수정) — `findByProductId`
- 수량 검사 이동과 주석 자리 (main)
  - `domain/inventory/Inventory.java` (수정) — `validateInboundQuantity(quantity, max)`
  - `domain/inventory/InventoryService.java` (수정) — 검사 제거, 증가 주석을 옮겨 옴
  - `application/inventory/InventoryApplicationService.java` (수정) — 설정 상한을 넘김
  - `domain/inventory/InventoryRepository.java`, `domain/product/ProductRepository.java` (수정) — 주석 삭제
- 테스트 (test: `src/test/java/com/deepfine/inventorysystem/`)
  - `presentation/controller/inventory/CurrentStockApiTest.java` (생성) — TC-3-01 ~ TC-3-06
  - `domain/inventory/InventoryTest.java` (생성) — TC-2-14
- 실측
  - `http/query.http` (생성) — 조회 실측 9건
- 문서
  - `docs/design/03-domain-model.md` (수정) — §13 두 번 조회
  - `docs/design/05-test-cases.md` (수정) — TC-2-14, TC-3-05, TC-3-06, F3/F4 given
  - `src/main/CLAUDE.md` (수정) — 엔티티 규칙과 주석 자리
  - `src/test/CLAUDE.md` (수정) — 테스트 데이터는 DB 직접 삽입
  - `.claude/skills/` (design, run-feature, wrap-up), `.claude/agents/` (doc-writer, analyst) (수정) — 작업 시간 기록과 목표 삭제
  - `docs/ai-log/F2-inbound.md` (수정) — 업체 확인을 `TenantService`로 나눈 내용 추가
  - `docs/task_list.md` (수정) — 시간 계획 표와 시간 목표 삭제

## 설계대로 한 것

- 조회 흐름 (03 §13)
  1. `ProductService.get` — 없으면 404 `PRODUCT_NOT_FOUND`
  2. `InventoryService.get` — 없으면 `IllegalStateException`(500)
- `@Transactional(readOnly = true)`
- 응답에 내부 식별자 없음
- `updatedAt`은 `BaseTimeEntity`의 `Instant`를 +09:00으로 응답
- TC-3-01 ~ TC-3-06
- 05 then 범위 안에서 더 넣은 단언
  - 응답 `updatedAt`과 DB `updated_at`이 같은 시각
  - 오류 본문 키는 `code`, `message` 둘뿐
  - 조회 전후 `updated_at` 불변

## 설계에 없어서 정한 것

- 조회 요청 DTO
  - 경로 변수뿐이라 Request DTO 없음
  - 컨트롤러가 `new InventoryCommand.CurrentStock(tenantId, productCode)`
- 규칙 위치 기준
  - repository가 필요 없는 규칙은 엔티티
  - repository가 필요한 절차만 도메인 서비스
  - 수량 상한은 ApplicationService가 설정값을 읽어 넘김
  - domain이 support를 참조하지 않음
- 주석 자리
  - 동작 설명은 도메인 서비스
  - repository 인터페이스에는 주석 없음
  - SQL 이유는 JpaRepository 쿼리 위

## 설계와 다르게 간 것

- 03 §13 조회 방식 (`6e01619`)
  - 전: JOIN 쿼리
  - 후: Spring Data 두 번 조회
  - 이유: 규칙 "직접 쓰는 SQL은 동시성 장치에만"과 부딪힘
- 05 F3/F4 given (`c7ac256`)
  - 전: 입고 API로 만든다
  - 후: DB에 직접 넣는다 (`InventoryTestDb`)
  - 이유: 입고가 깨져도 조회와 출고 테스트가 같이 깨지지 않게

## 리뷰/검증

- reviewer: 높음 0, 중간 0, 낮음 0
- 확인 필요 3건
  - `;`가 붙은 경로(`/inventory/A001;x`) → 200
    - 실측으로 확인
    - Spring이 `;` 뒤를 matrix 파라미터로 떼어 냄
    - `%3B`로 인코딩하면 404
    - 명세: 형식 밖 코드면 404
    - 처리: 그대로 두고 README 한계로 남김
  - `GET /inventory/inbound` → 405에서 404 `PRODUCT_NOT_FOUND`로 바뀜
    - "inbound"도 상품코드 형식이라 명세와 맞음
    - 처리: 그대로 둠
  - 시간 목표 문구 잔재
    - 처리: 삭제 `0540620`
- 05 TC 추가 제안 4건
  - 추가: TC-3-05 형식 밖 상품코드(101자, `A 001`) → 404
  - 추가: TC-3-06 소문자 `a001` 조회 → 404
  - 추가 안 함: `X-Tenant-Id` 없음/미등록 → TC-1-01/02와 `.http`로 이미 확인
  - 추가 안 함: 상품은 있고 재고 행이 없음 → 설계상 일어날 수 없는 경로

## 실행 확인

- `./gradlew spotlessApply build` → 통과, 테스트 48개
- 거짓 통과 점검
  - 상한 비교를 `>=`로 바꿈 → TC-2-14 실패
- `.http` 실측 → `http/query.http` 9건 모두 일치
  - 준비 입고 3건
  - TC-3-01 ~ TC-3-04
  - 업체 헤더 없음/미등록 2건
  - 서버 로그 ERROR 0
- 소문자 `z-a001` 조회 → 404 (대소문자 구분)
- Swagger 경로
  - `/api/v1/inventory/inbound`
  - `/api/v1/inventory/{productCode}`

## 에러와 해결

- 시각 비교 실패
  - 에러: `isEqualTo`로 비교하면 같은 시각인데 실패
  - 원인: DB는 UTC 오프셋, 응답은 +09:00
  - 해결: `isAtSameInstantAs`

## 자주 틀리는 것 후보

- 테스트에서 DB 시각(UTC 오프셋)과 응답 시각(+09:00)은 `isAtSameInstantAs`로 비교한다
  - `.claude/checklists/implementation.md` "테스트"에 추가함
