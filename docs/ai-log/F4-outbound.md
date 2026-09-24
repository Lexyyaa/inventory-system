# F4. 출고 `feature/outbound`

## 받은 지시

- `/run-feature F4`
- 브랜치: `feature/outbound`
- 범위: T4-1 ~ T4-5 구현
- T4-6(`.http`), T4-7(작업 로그)는 메인 세션

## 작업 흐름

1. 구현 전 설계 변경
   - `d0a8afb` 05에 TC-4-10 추가
     - 출고 수량 경계, 도메인 단위
     - 근거: "엔티티 메서드마다 도메인 단위 테스트" 규칙
     - T4-4 범위에 포함
2. implementer 구현
   - T4-1 `6e00bab` `feat: 재고 조건부 차감 쿼리 구현`
   - T4-2 `03f610e` `feat: 출고 API 구현`
   - T4-3 `11bf1d9` `test: 출고 성공/실패 케이스`
   - T4-4 `b519d5d` `test: 출고 엣지 케이스`
   - T4-5 `b718a9c` `test: 출고 동시성`
3. reviewer와 verifier (Sonnet)
4. 거짓 통과 점검
5. 서버 실측
   - T4-6 `91a37fc` `test: F4 .http 실행 케이스`
6. TC 추가
   - T4-8 `8c3642a` `test: 출고 판정 순서/요청 검증 케이스`
7. 출고 트랜잭션 이유 주석과 구현 체크리스트 2줄 (`docs: F4 리뷰 반영`)
8. 작업 로그 (T4-7)

## 바뀐 파일

- 차감 쿼리 (main: `src/main/java/com/deepfine/inventorysystem/`)
  - `infrastructure/persistence/inventory/InventoryJpaRepository.java` (수정) — `decreaseIfSufficient`
    - 03 §11 조건부 UPDATE … RETURNING
    - 반환 `Optional<InventoryState>`
  - `infrastructure/persistence/inventory/InventoryRepositoryImpl.java` (수정) — `decrease`
    - 빈 결과면 `INSUFFICIENT_STOCK`
  - `domain/inventory/InventoryRepository.java` (수정) — `decrease` 선언
- 출고 API (main)
  - `domain/inventory/InventoryService.java` (수정) — `decrease`, 동작 설명 주석
  - `domain/inventory/Inventory.java` (수정) — `validateOutboundQuantity`
    - 범위 판정은 입고와 함께 쓰는 `private isOutOfRange`
    - 출고 문구 상수
  - `domain/inventory/exception/InventoryException.java` (수정) — `(ErrorCode, String detail)` 생성자
    - F2 백로그 해소
  - `application/inventory/InventoryCommand.java`, `InventoryInfo.java` (수정) — `Outbound`
  - `application/inventory/InventoryApplicationService.java` (수정) — `outbound`, 트랜잭션 이유 주석
  - `presentation/controller/inventory/InventoryRequest.java`, `InventoryResponse.java` (수정) — `Outbound`
  - `presentation/controller/inventory/InventoryController.java` (수정) — `POST /api/v1/inventory/outbound`
  - `presentation/controller/inventory/InventoryApiDocs.java` (수정) — 출고 Swagger 문서
- 테스트 (test: `src/test/java/com/deepfine/inventorysystem/`)
  - `presentation/controller/inventory/OutboundApiTest.java` (생성) — TC-4-01 ~ TC-4-06, TC-4-11 ~ TC-4-12
  - `presentation/controller/inventory/OutboundConcurrencyTest.java` (생성) — TC-4-07 ~ TC-4-09
  - `domain/inventory/InventoryTest.java` (수정) — TC-4-10
- 실측
  - `http/outbound.http` (생성) — 출고 실측 13건
- 문서
  - `docs/design/05-test-cases.md` (수정) — TC-4-10 ~ TC-4-12
  - `.claude/checklists/implementation.md` (수정) — 2줄
  - `docs/task_list.md` (수정)

## 설계대로 한 것

- 차감은 03 §11 조건부 UPDATE … RETURNING 한 번
  - 결과 없음 → 409 `INSUFFICIENT_STOCK`
  - 재고 행 없음도 409 (03 §11)
- 출고 흐름 (03 §14, 트랜잭션 하나)
  1. 수량 검사 — 벗어나면 400 `INVALID_QUANTITY` (출고 문구)
  2. `productService.get` — 없으면 404 `PRODUCT_NOT_FOUND`
  3. 조건부 차감
- TC-4-01 ~ TC-4-12
  - API: TC-4-01 ~ TC-4-06, TC-4-11 ~ TC-4-12
  - 동시성: TC-4-07 ~ TC-4-09
  - 도메인 단위: TC-4-10

## 설계에 없어서 정한 것

- 차감 쿼리 반환 타입
  - `Optional<InventoryState>`
  - 근거: 상품 생성 쿼리 선례
  - 빈 결과가 `Optional.empty`로 오는지 임시 테스트로 확인 후 삭제
- 출고 문구 위치
  - `Inventory` 상수
- TC-4-09 단언 방식
  - 0번 스레드 입고, 1번 스레드 출고
  - 출고 결과(200 / 409)에 따라 분기 단언
- 출고 트랜잭션 (사용자 질문 "왜 트랜잭션 하나인가")
  - `@Transactional`을 빼도 출고 테스트는 통과함을 확인 → 정확성은 조건부 UPDATE 한 문장이 지킴
  - 그래도 유지: 차감 뒤 실패하면 차감까지 되돌려 재시도 시 두 번 빠지는 것을 막음
  - 이유를 `outbound` 주석으로 남김

## 설계와 다르게 간 것

- 없음

## 리뷰/검증

- reviewer: 높음 0, 중간 0, 낮음 0
- verifier (Sonnet): 높음 0, 중간 3, 낮음 6
- 중간 3건 — 모두 TC 부족
  - 출고 요청 검증 API TC 없음
    - productCode 누락, 빈 값, 101자, 허용 문자
    - quantity 누락, 소수, 문자열
  - "수량 범위 → 상품 존재" 판정 순서 조합 TC 없음
  - API 수준 출고 상한 초과 TC 없음
  - 처리: TC-4-11(판정 순서), TC-4-12(productCode 누락, 소수, 상한 초과) 추가
- 낮음 — 바로 반영
  - `.http` 상한 초과 라벨, 수량 -1 누락
    - 처리: 메인 세션이 `http/outbound.http` 고침
  - F2 백로그 detail 생성자 처리 칸 비어 있음
    - 처리: `docs/task_list.md`에 반영
- 낮음 — `docs/task_list.md` 리뷰 백로그
  - 출고 Swagger 설명의 "동시 요청은 서로 다른 값" 문장이 04 §4에 없음
    - 04 문서 결함 (ADR-18은 입출고 공통)
  - TC-4-09는 한 번 실행에 한 갈래만 검증
  - 05 TC-4-04 then에 `updated_at` 불변 없음
  - 재고 행 없음을 출고 409, 조회 500으로 처리
    - 03 §11, §13 정의대로
- 05 TC 추가 제안 (implementer, verifier)
  - 출고 판정 순서: 없는 상품 + 수량 0 → 400
  - 출고 요청 검증
  - API 수준 상한 초과
  - 출고 엔드포인트 업체 헤더 누락
  - 처리: 앞의 세 건은 TC-4-11과 TC-4-12로 추가, 업체 헤더 누락은 TC-1-01과 `.http`로 확인되어 추가 안 함

## 실행 확인

- `./gradlew spotlessApply build` → 통과, 테스트 60개
- 거짓 통과 점검 (운영 코드를 임시로 바꿨다가 되돌림)
  - `AND quantity >= :quantity` 삭제 → TC-4-03/07/08/09 실패
    - CHECK 위반 500
  - `>=` → `>` → TC-4-05/07/08 실패
  - 읽고 계산해서 쓰기 → TC-4-07/08 실패
    - 2회 재현
  - 출고에 입고 수량 검사 → TC-4-04 실패
    - 문구 다름
  - 수량 검사를 상품 조회 뒤로 → TC-4-11 실패
  - 설정 상한 대신 `Long.MAX_VALUE` → TC-4-12 실패
- `OutboundConcurrencyTest` 추가 3회 반복 → 통과
- `.http` 실측 → `http/outbound.http` 13건 모두 일치
  - 준비 입고 2건
  - TC-4-01 ~ TC-4-06
  - 수량 -1
  - 상한 초과
  - 필수값 누락
  - 형식 오류
  - 업체 헤더 없음
  - 서버 로그 ERROR 0
- 회귀 확인 (같은 서버)
  - `http/inbound.http` 10건 모두 일치
  - `http/query.http` 9건 모두 일치
- Swagger 경로
  - `/api/v1/inventory/inbound`
  - `/api/v1/inventory/outbound`
  - `/api/v1/inventory/{productCode}`

## 에러와 해결

- 없음

## 자주 틀리는 것 후보

- RETURNING이 빈 결과일 수 있는 native 쿼리는 `Optional<record>`로 받는다
- 동시성 장치의 조건을 빼거나 바꿨을 때 동시성 TC가 실패하는지 한 번 돌려 본다
- 처리: `.claude/checklists/implementation.md`에 추가함
