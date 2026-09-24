# 테스트 케이스

본 문서는 구현할 테스트 케이스를 기능별로 정의한다.

- TC 번호의 가운데 숫자는 기능 번호다 (1 기초 설정, 2 입고, 3 조회, 4 출고)
- 기능마다 성공 · 실패 · 엣지 · 동시성 케이스로 나눈다 (기초 설정은 데이터 제약)
- 테스트 종류
  - 도메인 단위: Spring과 DB 없이 도메인 객체만 검증한다
  - 통합 (API): MockMvc 요청부터 DB까지 검증한다
  - 통합 (저장소): API를 거치지 않고 DB에 직접 저장해 제약을 검증한다
  - 통합 테스트는 모두 Testcontainers로 띄운 PostgreSQL을 쓴다
- 테스트의 `@DisplayName`은 각 TC의 DisplayName을 그대로 쓴다
- 테스트 본문은 `// given` `// when` `// then`으로 나누고 TC의 given · when · then을 따른다
- 별도 표기가 없으면 요청 업체는 `tenant-001`이다
- given의 상품 · 재고는 입고 API를 거치지 않고 DB에 직접 넣는다. 입고가 깨져도 조회 · 출고 테스트가 같이 깨지지 않게 하려는 것이다
- 성공 응답의 `updatedAt`은 `+09:00` 오프셋이 붙은 ISO-8601이다. TC마다 적지 않지만 모든 성공 응답에서 확인한다
- 동시성 TC는 다음 판정 식이 성립해야 한다
  - 최종 재고 = 초기 재고 + 성공한 입고 수량의 합 − 성공한 출고 수량의 합

---

## F1 기초 설정 (업체 식별 · 데이터 제약)

- 업체 확인(TC-1-01~02)은 아직 입고 · 조회 · 출고 API가 없어서, 테스트에만 두는 엔드포인트 `GET /api/v1/test/tenant`로 확인한다
- 데이터 제약(TC-1-03~04)은 API를 거치지 않고 DB에 직접 저장해서 확인한다
- 테스트 종류
  - 통합 (API): TC-1-01 ~ TC-1-02
  - 통합 (저장소): TC-1-03 ~ TC-1-04

---

### 실패 케이스

#### TC-1-01 X-Tenant-Id가 없거나 비어 있음

- DisplayName: `[TC-1-01] X-Tenant-Id가 없거나 빈 문자열이거나 공백뿐이면 400 INVALID_TENANT로 거부한다`
- 테스트: 통합 (API)
- given
  - tenant 테이블에 seed 업체 tenant-001과 tenant-002가 있다
- when
  - 공통
    - `GET /api/v1/test/tenant`
  - 사전 확인 (엔드포인트가 등록됐는지 확인)
    - 헤더 `X-Tenant-Id: tenant-001`
  - 요청 1
    - `X-Tenant-Id` 헤더를 넣지 않는다
  - 요청 2
    - 헤더 `X-Tenant-Id`의 값은 빈 문자열
  - 요청 3
    - 헤더 `X-Tenant-Id`의 값은 공백 3칸
- then
  - 사전 확인은 응답 200
    - `tenantCode`: `tenant-001`
  - 요청 1~3 모두 응답 400
    - `code`: `INVALID_TENANT`
    - `message`: 빈 문자열이 아니다

---

#### TC-1-02 등록되지 않은 업체 코드

- DisplayName: `[TC-1-02] 등록되지 않은 업체 코드로 요청하면 100자를 넘는 코드여도 400 INVALID_TENANT로 거부한다`
- 테스트: 통합 (API)
- given
  - tenant 테이블에는 seed 업체 tenant-001과 tenant-002의 2건만 있다
- when
  - 공통
    - `GET /api/v1/test/tenant`
  - 요청 1
    - 헤더 `X-Tenant-Id: tenant-999`
  - 요청 2
    - 헤더 `X-Tenant-Id`의 값은 `t`를 101번 반복한 101자 문자열
- then
  - 두 요청 모두 응답 400 (요청 2는 500이 아니다)
    - `code`: `INVALID_TENANT`
  - DB
    - tenant 행: 2건 (변경 없음)

---

### 데이터 제약 케이스

#### TC-1-03 같은 업체에 같은 상품코드 중복 저장

- DisplayName: `[TC-1-03] 같은 업체에 같은 상품코드의 상품을 두 번 저장하면 유일 제약 위반으로 두 번째 저장이 실패한다`
- 테스트: 통합 (저장소)
- given
  - tenant-001의 id는 `SELECT id FROM tenant WHERE code = 'tenant-001'`로 구한다
  - DB에 직접 넣는다
    - product (tenant_id = tenant-001의 id, product_code = `A001`, name = `Apple`)
- when
  - DB에 직접 넣는다
    - product (tenant_id = tenant-001의 id, product_code = `A001`, name = `Samsung`)
- then
  - 두 번째 INSERT는 `DuplicateKeyException`(SQLState 23505)을 던진다
  - DB
    - tenant-001 / A001: product 행 1건, 상품명 Apple (변경 없음)

---

#### TC-1-04 음수 재고 저장

- DisplayName: `[TC-1-04] 재고를 음수로 저장하면 CHECK 제약 위반으로 저장이 실패하고 기존 재고를 유지한다`
- 테스트: 통합 (저장소)
- given
  - tenant-001의 id는 `SELECT id FROM tenant WHERE code = 'tenant-001'`로 구한다
  - `A001`과 `B001`의 product id는 INSERT의 `RETURNING id`로 받는다
  - DB에 직접 넣는다
    - product (tenant_id = tenant-001의 id, product_code = `A001`, name = `Apple`)
    - product (tenant_id = tenant-001의 id, product_code = `B001`, name = `Banana`)
    - inventory (product_id = `B001`의 id, quantity = 10)
  - `A001`의 inventory 행은 만들지 않는다
- when
  - 저장 1
    - DB에 직접 넣는다: inventory (product_id = `A001`의 id, quantity = -1)
  - 저장 2
    - `UPDATE inventory SET quantity = quantity - 11 WHERE product_id = ?` (바인드 값은 `B001`의 id)
- then
  - 두 저장 모두 `DataIntegrityViolationException`(SQLState 23514)을 던진다
  - DB
    - tenant-001 / A001: inventory 행 0건 (변경 없음)
    - tenant-001 / B001: inventory 행의 quantity 10 (변경 없음)

---

## F2 입고

- 테스트 종류
  - 도메인 단위: TC-2-05, TC-2-14
  - 통합 (API): 나머지 전부

---

### 성공 케이스

#### TC-2-01 신규 상품 입고

- DisplayName: `[TC-2-01] 등록되지 않은 상품코드로 입고하면 요청 업체의 상품을 만들고 입고 수량을 재고로 반영한다`
- 테스트: 통합 (API)
- given
  - 업체: tenant-001 (seed)
  - tenant-001에 상품코드 A001 상품 없음
  - tenant-002에 상품코드 A001 상품 없음
- when
  - POST /api/v1/inventory/inbound
  - 헤더 `X-Tenant-Id: tenant-001`
  - 본문 `{"productCode":"A001","productName":"Apple","quantity":10}`
- then
  - 응답 200
    - `productCode`: `A001`
    - `productName`: `Apple`
    - `quantity`: 10
    - 본문에 내부 식별자(id) 필드 없음
  - DB
    - tenant-001 / A001: 상품 1개, 상품명 Apple, 재고 행 1개, 재고 10
    - tenant-002 / A001: 상품 0개

---

#### TC-2-02 기존 상품 입고

- DisplayName: `[TC-2-02] 재고 100인 기존 상품에 30을 입고하면 재고가 130이 된다`
- 테스트: 통합 (API)
- given
  - DB에 직접 넣는다
    - tenant-001 / A001: 상품명 Apple, 재고 100
- when
  - POST /api/v1/inventory/inbound
  - 헤더 `X-Tenant-Id: tenant-001`
  - 본문 `{"productCode":"A001","productName":"Apple","quantity":30}`
- then
  - 응답 200
    - `productCode`: `A001`
    - `productName`: `Apple`
    - `quantity`: 130
  - DB
    - tenant-001 / A001: 상품 1개 (새 상품 없음), 재고 130

---

#### TC-2-03 다른 업체의 같은 상품코드

- DisplayName: `[TC-2-03] 다른 업체에만 있는 상품코드로 입고하면 요청 업체의 신규 상품을 만들고 다른 업체의 상품과 재고는 바꾸지 않는다`
- 테스트: 통합 (API)
- given
  - DB에 직접 넣는다
    - tenant-001 / A001: 상품명 Apple, 재고 10
  - tenant-002에 상품코드 A001 상품 없음
- when
  - POST /api/v1/inventory/inbound
  - 헤더 `X-Tenant-Id: tenant-002`
  - 본문 `{"productCode":"A001","productName":"Samsung","quantity":5}`
- then
  - 응답 200
    - `productCode`: `A001`
    - `productName`: `Samsung`
    - `quantity`: 5
  - DB
    - tenant-002 / A001: 상품 1개, 상품명 Samsung, 재고 5
    - tenant-001 / A001: 상품 1개, 상품명 Apple, 재고 10 (변경 없음)

---

### 실패 케이스

#### TC-2-04 상품명 불일치

- DisplayName: `[TC-2-04] 기존 상품과 다른 상품명으로 입고하면 PRODUCT_NAME_MISMATCH로 거부하고 재고와 상품명을 유지한다`
- 테스트: 통합 (API)
- given
  - DB에 직접 넣는다
    - tenant-001 / A001: 상품명 Apple, 재고 10
  - 삽입한 재고 행의 updated_at 값을 기록
- when
  - POST /api/v1/inventory/inbound
  - 헤더 `X-Tenant-Id: tenant-001`
  - 본문 `{"productCode":"A001","productName":"Samsung","quantity":5}`
- then
  - 응답 409
    - `code`: `PRODUCT_NAME_MISMATCH`
  - DB
    - tenant-001 / A001: 상품 1개, 상품명 Apple, 재고 10 (변경 없음)
      - 재고 updated_at은 given에서 기록한 값과 같음

---

#### TC-2-05 상품명 정확 일치 판정

- DisplayName: `[TC-2-05] 상품명은 대소문자와 공백까지 같을 때만 같은 상품명으로 판정한다`
- 테스트: 도메인 단위
- given
  - 업체 id 1, 상품코드 `A001`, 상품명 `Apple`인 상품을 정적 팩토리로 만든다
- when
  - 상품명 `Apple`, `apple`, `Apple `(뒤 공백), ` Apple`(앞 공백)으로 각각 상품명 일치를 검증한다
- then
  - `Apple`은 통과한다
  - 다음은 각각 `ErrorCode.PRODUCT_NAME_MISMATCH` 예외를 던진다
    - `apple`
    - `Apple `(뒤 공백)
    - ` Apple`(앞 공백)
  - 상품의 상품명은 `Apple` 그대로다

---

#### TC-2-06 수량 0과 음수

- DisplayName: `[TC-2-06] 입고 수량이 0이나 -1이거나 상한 1,000,000,000을 넘으면 INVALID_QUANTITY로 거부하고 상품을 만들지 않는다`
- 테스트: 통합 (API)
- given
  - tenant-001에 상품코드 A001 상품 없음
- when
  - 공통
    - POST /api/v1/inventory/inbound
    - 헤더 `X-Tenant-Id: tenant-001`
  - 요청 1
    - 본문 `{"productCode":"A001","productName":"Apple","quantity":0}`
  - 요청 2
    - 본문 `{"productCode":"A001","productName":"Apple","quantity":-1}`
  - 요청 3 (상한 초과)
    - 본문 `{"productCode":"A001","productName":"Apple","quantity":1000000001}`
- then
  - 요청 1~3 각각
    - 응답 400
      - `code`: `INVALID_QUANTITY`
  - DB
    - tenant-001 / A001: 상품 0개, 재고 행 0개

---

#### TC-2-07 필수값 누락과 빈 문자열

- DisplayName: `[TC-2-07] productCode·productName·quantity가 없거나 productCode·productName이 빈 문자열이거나 productName이 공백뿐이면 INVALID_REQUEST로 거부한다`
- 테스트: 통합 (API)
- given
  - tenant-001에 상품코드 A001 상품 없음
  - 요청 전 tenant-001의 상품 건수를 기록
- when
  - 공통
    - POST /api/v1/inventory/inbound
    - 헤더 `X-Tenant-Id: tenant-001`
  - 요청 1 (productCode 누락)
    - 본문 `{"productName":"Apple","quantity":10}`
  - 요청 2 (productName 누락)
    - 본문 `{"productCode":"A001","quantity":10}`
  - 요청 3 (quantity 누락)
    - 본문 `{"productCode":"A001","productName":"Apple"}`
  - 요청 4 (productCode 빈 문자열)
    - 본문 `{"productCode":"","productName":"Apple","quantity":10}`
  - 요청 5 (productName 빈 문자열)
    - 본문 `{"productCode":"A001","productName":"","quantity":10}`
  - 요청 6 (productName 공백 3칸)
    - 본문 `{"productCode":"A001","productName":"   ","quantity":10}`
- then
  - 요청 1~6 각각
    - 응답 400
      - `code`: `INVALID_REQUEST`
  - DB
    - tenant-001 / A001: 상품 0개
    - tenant-001: 상품 건수가 요청 전과 같음

---

#### TC-2-08 정수가 아닌 수량과 깨진 JSON

- DisplayName: `[TC-2-08] 수량이 1.5나 문자열 "10"이거나 JSON이 깨졌으면 INVALID_REQUEST로 거부하고, 헤더까지 없으면 INVALID_TENANT가 먼저다`
- 테스트: 통합 (API)
- given
  - tenant-001에 상품코드 A001 상품 없음
- when
  - 공통
    - POST /api/v1/inventory/inbound
    - 헤더 `X-Tenant-Id: tenant-001` (요청 4는 제외)
  - 요청 1 (소수)
    - 본문 `{"productCode":"A001","productName":"Apple","quantity":1.5}`
  - 요청 2 (문자열)
    - 본문 `{"productCode":"A001","productName":"Apple","quantity":"10"}`
  - 요청 3 (깨진 JSON)
    - 본문 `{"productCode":"A001","productName":"Apple","quantity":10` (닫는 중괄호 없음)
  - 요청 4 (헤더 없음 + 깨진 JSON)
    - `X-Tenant-Id` 헤더를 넣지 않는다
    - 본문은 요청 3과 같다
- then
  - 요청 1~3 각각
    - 응답 400 (500 아님)
      - `code`: `INVALID_REQUEST`
  - 요청 4는 응답 400
    - `code`: `INVALID_TENANT` (업체 확인이 본문 해석보다 먼저다)
  - DB
    - tenant-001 / A001: 상품 0개, 재고 행 0개

---

### 엣지 케이스

#### TC-2-09 상품코드 · 상품명 길이 경계

- DisplayName: `[TC-2-09] 상품코드 100자와 상품명 255자는 받고, 101자 상품코드나 256자 상품명은 INVALID_REQUEST로 거부한다`
- 테스트: 통합 (API)
- given
  - tenant-001에 아래 상품코드의 상품이 하나도 없다
- when
  - 공통
    - `POST /api/v1/inventory/inbound`
    - 헤더 `X-Tenant-Id: tenant-001`
    - 수량 10
  - 요청 1
    - 상품코드 `C` 100자, 상품명 `N` 255자
  - 요청 2
    - 상품코드 `D` 101자, 상품명 `Apple`
  - 요청 3
    - 상품코드 `A001`, 상품명 `N` 256자
- then
  - 요청 1
    - 응답 200
      - `productCode`: 요청 값 그대로
      - `productName`: 요청 값 그대로
      - `quantity`: 10
  - 요청 2 · 3 각각
    - 응답 400 (500이 아니다)
      - `code`: `INVALID_REQUEST`
  - DB
    - tenant-001 / `C` 100자: 상품 1개, 재고 10
    - tenant-001 / `D` 101자: 상품 0개
    - tenant-001 / `A001`: 상품 0개

---

#### TC-2-10 재고 반영 실패 시 롤백

- DisplayName: `[TC-2-10] 신규 상품 입고 중 재고 반영 단계에서 실패하면 INTERNAL_SERVER_ERROR로 응답하고 상품과 재고를 모두 남기지 않는다`
- 테스트: 통합 (API)
- given
  - tenant-001에 상품코드 A001 상품 없음
  - 예외 주입: 재고 반영(inventory UPSERT) 호출 시 `RuntimeException("injected")` 발생
  - 상품 생성(product INSERT)은 정상 실행
- when
  - POST /api/v1/inventory/inbound
  - 헤더 `X-Tenant-Id: tenant-001`
  - 본문 `{"productCode":"A001","productName":"Apple","quantity":10}`
- then
  - 응답 500
    - `code`: `INTERNAL_SERVER_ERROR`
    - `message`: 주입한 예외 메시지 `injected`가 없음
  - DB
    - tenant-001 / A001: 상품 0개, 재고 행 0개

---

#### TC-2-14 입고 수량 경계 판정

- DisplayName: `[TC-2-14] 입고 수량이 1 이상 상한 이하일 때만 통과하고 벗어나면 INVALID_QUANTITY로 거부한다`
- 테스트: 도메인 단위
- given
  - 상한 1,000,000,000
- when
  - 수량 -1, 0, 1, 1,000,000,000, 1,000,000,001로 각각 입고 수량을 검증한다
- then
  - 1과 1,000,000,000은 통과한다
  - 다음은 각각 `ErrorCode.INVALID_QUANTITY` 예외를 던진다
    - -1
    - 0
    - 1,000,000,001

---

### 동시성 케이스

#### TC-2-11 기존 상품 동시 입고

- DisplayName: `[TC-2-11] 재고 100인 상품에 10·20·30을 동시에 입고하면 모두 성공하고 재고가 160이 된다`
- 테스트: 통합 (API)
- given
  - DB에 직접 넣는다
    - tenant-001 / A001: 상품명 Apple, 재고 100
- when
  - 공통
    - POST /api/v1/inventory/inbound
    - 헤더 `X-Tenant-Id: tenant-001`
  - 스레드 3개를 시작 래치로 같은 시점에 출발시킨다
  - 스레드 1
    - 본문 `{"productCode":"A001","productName":"Apple","quantity":10}`
  - 스레드 2
    - 본문 `{"productCode":"A001","productName":"Apple","quantity":20}`
  - 스레드 3
    - 본문 `{"productCode":"A001","productName":"Apple","quantity":30}`
- then
  - 응답
    - 200 3건
      - `productCode`: 모두 `A001`
      - `productName`: 모두 `Apple`
      - `quantity`: 최댓값 160
      - 초기 재고 100과 응답 quantity 3개를 오름차순 정렬하면 이웃한 값의 차이 3개가 10·20·30의 한 순열
        - 예: 100, 120, 130, 160 → 20, 10, 30
    - 실패 0건
  - DB
    - tenant-001 / A001: 상품 1개, 재고 행 1개, 재고 160
  - 판정 식
    - 160 = 100 + (10 + 20 + 30) − 0

---

#### TC-2-12 신규 상품 동시 입고

- DisplayName: `[TC-2-12] 등록되지 않은 상품에 같은 상품명으로 10·20·30을 동시에 입고하면 상품을 하나만 만들고 재고가 60이 된다`
- 테스트: 통합 (API)
- given
  - tenant-001에 상품코드 A001 상품 없음
- when
  - 공통
    - POST /api/v1/inventory/inbound
    - 헤더 `X-Tenant-Id: tenant-001`
  - 스레드 3개를 시작 래치로 같은 시점에 출발시킨다
  - 스레드 1
    - 본문 `{"productCode":"A001","productName":"Apple","quantity":10}`
  - 스레드 2
    - 본문 `{"productCode":"A001","productName":"Apple","quantity":20}`
  - 스레드 3
    - 본문 `{"productCode":"A001","productName":"Apple","quantity":30}`
- then
  - 응답
    - 200 3건
      - `productCode`: 모두 `A001`
      - `productName`: 모두 `Apple`
      - `quantity`: 최댓값 60
    - 실패 0건
  - DB
    - tenant-001 / A001: 상품 1개, 재고 행 1개, 재고 60
  - 판정 식
    - 60 = 0 + (10 + 20 + 30) − 0

---

#### TC-2-13 신규 상품 동시 입고 중 상품명 불일치

- DisplayName: `[TC-2-13] 등록되지 않은 상품에 다른 상품명으로 동시에 입고하면 한 건만 성공하고 나머지는 PRODUCT_NAME_MISMATCH로 거부한다`
- 테스트: 통합 (API)
- given
  - tenant-001에 상품코드 A001 상품 없음
- when
  - 공통
    - POST /api/v1/inventory/inbound
    - 헤더 `X-Tenant-Id: tenant-001`
  - 스레드 2개를 시작 래치로 같은 시점에 출발시킨다
  - 스레드 1
    - 본문 `{"productCode":"A001","productName":"Apple","quantity":10}`
  - 스레드 2
    - 본문 `{"productCode":"A001","productName":"Samsung","quantity":20}`
- then
  - 응답
    - 200 1건
      - `productCode`: `A001`
      - `productName`: 성공한 요청의 productName
      - `quantity`: 성공한 요청의 quantity (Apple이면 10, Samsung이면 20)
    - 409 1건
      - `code`: `PRODUCT_NAME_MISMATCH`
  - DB
    - tenant-001 / A001: 상품 1개, 재고 행 1개, 상품명은 성공한 요청의 productName, 재고는 성공한 요청의 quantity
  - 판정 식
    - 최종 재고 = 0 + 성공한 입고 수량 − 0

---

## F3 조회

- 테스트 종류
  - 통합 (API): 전부

---

### 성공 케이스

#### TC-3-01 등록된 상품 조회

- DisplayName: `[TC-3-01] 등록된 상품을 조회하면 200으로 상품코드·상품명·재고 수량·마지막 변경 시각을 반환하고 내부 식별자는 반환하지 않는다`
- 테스트: 통합 (API)
- given
  - 업체: tenant-001
  - DB에 직접 넣는다
    - tenant-001 / A001 / Apple, 재고 10
- when
  - GET /api/v1/inventory/A001
  - 헤더 `X-Tenant-Id: tenant-001`
- then
  - 응답 200
    - `productCode`: `A001`
    - `productName`: `Apple`
    - `quantity`: 10
    - 본문 필드는 `productCode`, `productName`, `quantity`, `updatedAt` 4개뿐이다
    - 본문에 `id`, `productId`, `tenantId` 필드가 없다
  - DB
    - tenant-001: 상품 1개 (A001 / Apple)
    - tenant-001 / A001: 재고 10, 재고 updated_at이 조회 전과 같음 (변경 없음)

---

#### TC-3-02 업체별 같은 상품코드 조회

- DisplayName: `[TC-3-02] tenant-001과 tenant-002가 각자의 A001을 조회하면 각각 200과 자기 업체의 재고 10과 20을 반환한다`
- 테스트: 통합 (API)
- given
  - DB에 직접 넣는다
    - tenant-001 / A001 / Apple, 재고 10
    - tenant-002 / A001 / Samsung, 재고 20
- when
  - 공통
    - GET /api/v1/inventory/A001
  - 요청 1
    - 헤더 `X-Tenant-Id: tenant-001`
  - 요청 2
    - 헤더 `X-Tenant-Id: tenant-002`
  - 두 요청을 순서대로 보낸다
- then
  - 요청 1
    - 응답 200
      - `productCode`: `A001`
      - `productName`: `Apple`
      - `quantity`: 10
  - 요청 2
    - 응답 200
      - `productCode`: `A001`
      - `productName`: `Samsung`
      - `quantity`: 20
  - DB
    - tenant-001 / A001: 상품 1개, 상품명 Apple, 재고 10 (변경 없음)
    - tenant-002 / A001: 상품 1개, 상품명 Samsung, 재고 20 (변경 없음)

---

### 실패 케이스

#### TC-3-03 등록되지 않은 상품 조회

- DisplayName: `[TC-3-03] 요청 업체에 등록되지 않은 상품코드를 조회하면 404 PRODUCT_NOT_FOUND를 반환하고 재고 정보를 반환하지 않는다`
- 테스트: 통합 (API)
- given
  - 업체: tenant-001
  - DB에 직접 넣는다
    - tenant-001 / A001 / Apple, 재고 10
  - tenant-001에 B001 상품은 없다
- when
  - GET /api/v1/inventory/B001
  - 헤더 `X-Tenant-Id: tenant-001`
- then
  - 응답 404
    - `code`: `PRODUCT_NOT_FOUND`
    - `message`: `상품을 찾을 수 없습니다.`
    - 본문에 `productCode`, `productName`, `quantity`, `updatedAt` 필드가 없다
  - DB
    - tenant-001: 상품 1개 (A001만 있음)
    - B001: 상품이 생성되지 않음
    - tenant-001 / A001: 재고 10 (변경 없음)

---

### 엣지 케이스

#### TC-3-04 다른 업체에만 있는 상품 조회

- DisplayName: `[TC-3-04] tenant-002가 tenant-001에만 있는 A001을 조회하면 404 PRODUCT_NOT_FOUND를 반환하고 tenant-001의 재고는 노출하지 않는다`
- 테스트: 통합 (API)
- given
  - 업체: tenant-001, tenant-002
  - DB에 직접 넣는다
    - tenant-001 / A001 / Apple, 재고 10
  - tenant-002에는 상품이 없다
- when
  - GET /api/v1/inventory/A001
  - 헤더 `X-Tenant-Id: tenant-002`
- then
  - 응답 404
    - `code`: `PRODUCT_NOT_FOUND`
    - `message`: `상품을 찾을 수 없습니다.`
    - 본문에 `productCode`, `productName`, `quantity`, `updatedAt` 필드가 없다
  - DB
    - tenant-002: 상품 0개
    - tenant-001: 상품 1개 (A001 / Apple)
    - tenant-001 / A001: 재고 10 (변경 없음)

---

## F4 출고

- 테스트 종류
  - 통합 (API): 전부

---

### 성공 케이스

#### TC-4-01 정상 출고

- DisplayName: `[TC-4-01] 재고 100에서 30을 출고하면 200과 출고 직후 재고 70을 반환한다`
- 테스트: 통합 (API)
- given
  - 업체 `tenant-001`, 상품 `A001` / `Apple`, 재고 100
  - DB에 직접 넣는다: tenant-001 / A001 / Apple, 재고 100
- when
  - `POST /api/v1/inventory/outbound`
  - 헤더 `X-Tenant-Id: tenant-001`
  - 본문 `{"productCode":"A001","quantity":30}`
- then
  - 응답 200
    - `productCode`: `A001`
    - `productName`: `Apple`
    - `quantity`: 70
    - 필드는 `productCode`, `productName`, `quantity`, `updatedAt` 4개다
    - 내부 `id`가 없다
  - 조회 API `GET /api/v1/inventory/A001` 응답 200
    - `quantity`: 70
    - `updatedAt`: 출고 응답의 `updatedAt`과 같다
  - DB
    - `tenant-001` / `A001`: 상품 1건, 상품명 `Apple`

---

### 실패 케이스

#### TC-4-02 등록되지 않은 상품 출고

- DisplayName: `[TC-4-02] 등록되지 않은 상품을 출고하면 PRODUCT_NOT_FOUND로 거부하고 상품을 만들지 않는다`
- 테스트: 통합 (API)
- given
  - 업체 `tenant-001`
  - `A001` 상품을 넣지 않는다
- when
  - `POST /api/v1/inventory/outbound`
  - 헤더 `X-Tenant-Id: tenant-001`
  - 본문 `{"productCode":"A001","quantity":10}`
- then
  - 응답 404
    - `code`: `PRODUCT_NOT_FOUND`
  - 조회 API `GET /api/v1/inventory/A001` 응답 404
    - `code`: `PRODUCT_NOT_FOUND`
  - DB
    - `tenant-001` / `A001`: 상품 0건

---

#### TC-4-03 재고 부족

- DisplayName: `[TC-4-03] 재고 10에서 11을 출고하면 INSUFFICIENT_STOCK으로 거부하고 재고를 그대로 둔다`
- 테스트: 통합 (API)
- given
  - 업체 `tenant-001`, 상품 `A001` / `Apple`, 재고 10
  - DB에 직접 넣는다: tenant-001 / A001 / Apple, 재고 10
  - 재고 행의 `updated_at`을 기록한다
- when
  - `POST /api/v1/inventory/outbound`
  - 헤더 `X-Tenant-Id: tenant-001`
  - 본문 `{"productCode":"A001","quantity":11}`
- then
  - 응답 409
    - `code`: `INSUFFICIENT_STOCK`
  - 이어서 조회 API `GET /api/v1/inventory/A001` 응답 200
    - `quantity`: 10 (변경 없음)
    - `updatedAt`: given에서 기록한 `updated_at`과 같은 시각

---

#### TC-4-04 수량 0과 음수

- DisplayName: `[TC-4-04] 출고 수량이 0 또는 -1이면 INVALID_QUANTITY로 거부하고 재고를 유지한다`
- 테스트: 통합 (API)
- given
  - 업체 `tenant-001`, 상품 `A001` / `Apple`, 재고 10
  - DB에 직접 넣는다: tenant-001 / A001 / Apple, 재고 10
- when
  - 공통
    - `POST /api/v1/inventory/outbound`
    - 헤더 `X-Tenant-Id: tenant-001`
  - 요청 1
    - 본문 `{"productCode":"A001","quantity":0}`
  - 요청 2
    - 본문 `{"productCode":"A001","quantity":-1}`
- then
  - 요청 1
    - 응답 400
      - `code`: `INVALID_QUANTITY`
  - 요청 2
    - 응답 400
      - `code`: `INVALID_QUANTITY`
  - 조회 API `GET /api/v1/inventory/A001` 응답 200
    - `quantity`: 10 (변경 없음)

---

### 엣지 케이스

#### TC-4-05 재고 전량 출고

- DisplayName: `[TC-4-05] 재고 10에서 10을 출고하면 200과 재고 0을 반환하고 상품은 계속 조회된다`
- 테스트: 통합 (API)
- given
  - 업체 `tenant-001`, 상품 `A001` / `Apple`, 재고 10
  - DB에 직접 넣는다: tenant-001 / A001 / Apple, 재고 10
- when
  - `POST /api/v1/inventory/outbound`
  - 헤더 `X-Tenant-Id: tenant-001`
  - 본문 `{"productCode":"A001","quantity":10}`
- then
  - 응답 200
    - `productCode`: `A001`
    - `productName`: `Apple`
    - `quantity`: 0
  - 조회 API `GET /api/v1/inventory/A001` 응답 200
    - `quantity`: 0
  - DB
    - `tenant-001` / `A001`: 상품 1건

---

#### TC-4-06 다른 업체 상품 출고

- DisplayName: `[TC-4-06] tenant-002가 tenant-001에만 있는 A001을 출고하면 PRODUCT_NOT_FOUND로 거부하고 tenant-001의 재고를 유지한다`
- 테스트: 통합 (API)
- given
  - 업체 `tenant-001`, 상품 `A001` / `Apple`, 재고 10
  - DB에 직접 넣는다: tenant-001 / A001 / Apple, 재고 10
  - 재고 행의 `updated_at`을 기록한다
  - `tenant-002`에는 `A001` 상품이 없다
- when
  - `POST /api/v1/inventory/outbound`
  - 헤더 `X-Tenant-Id: tenant-002`
  - 본문 `{"productCode":"A001","quantity":5}`
- then
  - 응답 404
    - `code`: `PRODUCT_NOT_FOUND`
  - `tenant-001`로 조회 API `GET /api/v1/inventory/A001` 응답 200
    - `quantity`: 10 (변경 없음)
    - `productName`: `Apple` (변경 없음)
    - `updatedAt`: given에서 기록한 `updated_at`과 같은 시각
  - `tenant-002`로 조회 API `GET /api/v1/inventory/A001` 응답 404
    - `code`: `PRODUCT_NOT_FOUND`
  - DB
    - `tenant-002` / `A001`: 상품 0건

---

### 동시성 케이스

#### TC-4-07 두 건 동시 출고

- DisplayName: `[TC-4-07] 재고 10에서 10개 출고 두 건이 동시에 오면 한 건만 성공하고 최종 재고는 0이다`
- 테스트: 통합 (API)
- given
  - 업체 `tenant-001`, 상품 `A001` / `Apple`, 재고 10
  - DB에 직접 넣는다: tenant-001 / A001 / Apple, 재고 10
- when
  - `ConcurrencyRunner.run`으로 스레드 2개를 시작 래치로 동시에 출발시킨다
  - 스레드마다
    - `POST /api/v1/inventory/outbound`
    - 헤더 `X-Tenant-Id: tenant-001`
    - 본문 `{"productCode":"A001","quantity":10}`
- then
  - 응답
    - 200: 1건
      - `quantity`: 0
    - 409: 1건
      - `code`: `INSUFFICIENT_STOCK`
    - 그 밖의 상태(500 등): 0건
  - 조회 API `GET /api/v1/inventory/A001` 응답 200
    - `quantity`: 0
  - 판정 식
    - 최종 재고 0 = 초기 재고 10 + 성공한 입고 합 0 − 성공한 출고 합 10

---

#### TC-4-08 스무 건 동시 출고

- DisplayName: `[TC-4-08] 재고 100에서 10개 출고 스무 건이 동시에 오면 열 건만 성공하고 최종 재고는 0이다`
- 테스트: 통합 (API)
- given
  - 업체 `tenant-001`, 상품 `A001` / `Apple`, 재고 100
  - DB에 직접 넣는다: tenant-001 / A001 / Apple, 재고 100
- when
  - `ConcurrencyRunner.run`으로 스레드 20개를 시작 래치로 동시에 출발시킨다
  - 스레드마다
    - `POST /api/v1/inventory/outbound`
    - 헤더 `X-Tenant-Id: tenant-001`
    - 본문 `{"productCode":"A001","quantity":10}`
- then
  - 응답
    - 200: 10건
      - `quantity` 집합: {90, 80, 70, 60, 50, 40, 30, 20, 10, 0}
    - 409: 10건
      - `code`: `INSUFFICIENT_STOCK`
    - 그 밖의 상태(500 등): 0건
  - 조회 API `GET /api/v1/inventory/A001` 응답 200
    - `quantity`: 0
  - 판정 식
    - 최종 재고 0 = 초기 재고 100 + 성공한 입고 합 0 − 성공한 출고 합 100(10건 × 10)

---

#### TC-4-09 입고와 출고 동시

- DisplayName: `[TC-4-09] 재고 10에서 10 입고와 15 출고가 동시에 오면 출고 결과에 따라 최종 재고가 5 또는 20이 된다`
- 테스트: 통합 (API)
- given
  - 업체 `tenant-001`, 상품 `A001` / `Apple`, 재고 10
  - DB에 직접 넣는다: tenant-001 / A001 / Apple, 재고 10
- when
  - `ConcurrencyRunner.run`으로 스레드 2개를 시작 래치로 동시에 출발시킨다
  - 공통
    - 헤더 `X-Tenant-Id: tenant-001`
  - 스레드 1
    - `POST /api/v1/inventory/inbound`
    - 본문 `{"productCode":"A001","productName":"Apple","quantity":10}`
  - 스레드 2
    - `POST /api/v1/inventory/outbound`
    - 본문 `{"productCode":"A001","quantity":15}`
- then
  - 응답
    - 입고: 200
      - `quantity`: 20
    - 출고: 200 또는 409
      - 200이면 `quantity`: 5
      - 409면 `code`: `INSUFFICIENT_STOCK`
    - 그 밖의 상태(500 등): 0건
  - 조회 API `GET /api/v1/inventory/A001` 응답 200
    - 출고가 200이면 `quantity`: 5
    - 출고가 409면 `quantity`: 20
  - 판정 식
    - 최종 재고 = 초기 재고 10 + 성공한 입고 합 10 − 성공한 출고 합(200이면 15, 409면 0)
