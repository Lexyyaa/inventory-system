# API 명세

> 본 문서는 재고관리 시스템 MVP에서 제공하는 API의 요청 및 응답 형식을 정의한다.

---

# 1. API 개요

## 기본 경로

```text
/api/v1
```

---

## API 목록

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| POST   | `/inventory/inbound`       | 상품 입고       |
| POST   | `/inventory/outbound`      | 상품 출고       |
| GET    | `/inventory/{productCode}` | 현재 재고 조회    |

현재 MVP에서는 재고 목록 조회와 검색 API를 제공하지 않는다.

---

# 2. 공통

## 업체 식별

`/api/v1/**`의 모든 요청은 요청 대상 업체를 식별하기 위한 `X-Tenant-Id` Header를 포함한다.

```http
X-Tenant-Id: tenant-001
```

값은 미리 등록된 업체 코드다.

Header가 없거나, 비어 있거나, 공백뿐이거나, 등록되지 않은 업체 코드면 `400 INVALID_TENANT`를 반환한다.

현재 MVP에서는 인증/인가 시스템을 구현하지 않으므로 Header를 통해 Tenant Context를 전달한다.

실제 서비스에서는 인증 정보에서 Tenant Context를 결정하는 방식으로 대체할 수 있다.

---

## 오류 판정 순서

한 요청에 여러 오류가 있으면 다음 순서로 먼저 걸리는 오류 하나만 반환한다.

```text
1. Tenant 확인           → 400 INVALID_TENANT
2. 요청 형식 (JSON, 타입) → 400 INVALID_REQUEST
3. 필수값 · 길이         → 400 INVALID_REQUEST
4. 수량 범위             → 400 INVALID_QUANTITY
5. 상품 존재             → 404 PRODUCT_NOT_FOUND
6. 상품 · 재고 상태       → 409
```

---

## 시각 표기

`updatedAt`은 `+09:00` 오프셋을 포함한 ISO-8601 문자열이다.

```text
2026-09-24T22:10:00+09:00
```

---

## 상품 식별

상품은 요청한 업체 안에서 `productCode`로 식별한다. 서로 다른 업체는 같은 `productCode`를 쓸 수 있다.

시스템 내부 식별자는 응답에 노출하지 않는다.

---

## 정의되지 않은 필드

요청 본문에 정의되지 않은 필드가 있으면 무시한다.

---

## 공통 오류 응답

모든 오류 응답은 다음 형식으로 반환한다.

```json
{
  "code": "ERROR_CODE",
  "message": "오류 메시지"
}
```

---

# 3. 상품 입고

## POST `/inventory/inbound`

상품을 입고한다.

요청 업체에 상품이 이미 등록되어 있으면 해당 상품의 재고를 증가시키고, 등록되지 않은 상품이면 요청 업체의 상품으로 생성한 후 재고를 반영한다.

---

## 요청

### 헤더

```http
X-Tenant-Id: tenant-001
Content-Type: application/json
```

### 본문

```json
{
  "productCode": "A001",
  "productName": "Apple",
  "quantity": 10
}
```

### 필드

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | :---: | --- |
| productCode | String | Y | 상품 코드 (업체 안에서 유일) |
| productName | String |        Y | 상품명            |
| quantity    | Long   |        Y | 입고 수량          |

### 검증

* `productCode`는 영문 대소문자, 숫자, `_`, `-`로 이루어진 1~100자다. 대소문자를 구분한다.
* `productName`은 1~255자이며 비어 있거나 공백만 있을 수 없다.
* `quantity`는 1 이상 1,000,000,000 이하의 정수여야 한다. 상한은 설정으로 관리한다.
* `quantity`는 JSON 정수만 받는다. 소수(`1.5`)나 문자열(`"10"`)은 `INVALID_REQUEST`다.

---

## 신규 상품인 경우

입고 요청의 상품코드가 요청 업체에 등록되어 있지 않으면 요청 업체의 상품으로 생성한 후 입고를 처리한다. 다른 업체에 같은 상품코드가 있어도 신규 상품이다.

예:

```text
요청
A001 / Apple / 10

처리 결과
Product 생성
Inventory 생성
재고 10
```

---

## 기존 상품인 경우

이미 존재하는 상품이라면 기존 상품의 정보를 사용하여 재고를 증가시킨다.

* 요청의 `productName`이 기존 상품명과 다르면 입고를 처리하지 않는다 (`PRODUCT_NAME_MISMATCH`)
  * 대소문자와 공백까지 정확히 같아야 같은 상품명이다

---

## 성공 응답

### 200 OK

```json
{
  "productCode": "A001",
  "productName": "Apple",
  "quantity": 110,
  "updatedAt": "2026-09-24T22:10:00+09:00"
}
```

`quantity`는 이 요청의 입고가 반영된 직후의 재고 수량이다.

동시에 처리된 입고 요청들은 서로 다른 `quantity`를 받을 수 있다.

`productName`은 저장된 상품명이다.

`updatedAt`은 재고가 마지막으로 변경된 시각이다.

---

## 오류 응답

### 400 Bad Request

#### 잘못된 수량

```json
{
  "code": "INVALID_QUANTITY",
  "message": "입고 수량이 허용 범위를 벗어났습니다."
}
```

#### 필수 요청 정보 누락

```json
{
  "code": "INVALID_REQUEST",
  "message": "필수 요청 정보가 누락되었습니다."
}
```

#### 요청 형식 오류

```json
{
  "code": "INVALID_REQUEST",
  "message": "요청 형식이 올바르지 않습니다."
}
```

#### 업체 정보 누락 또는 미등록

```json
{
  "code": "INVALID_TENANT",
  "message": "Tenant 정보가 없거나 등록되지 않았습니다."
}
```

### 409 Conflict

#### 기존 상품명과 불일치

```json
{
  "code": "PRODUCT_NAME_MISMATCH",
  "message": "동일한 상품코드에 등록된 상품명과 일치하지 않습니다."
}
```

---

# 4. 상품 출고

## POST `/inventory/outbound`

상품을 출고한다.

현재 재고보다 많은 수량은 출고할 수 없다.

---

## 요청

### 헤더

```http
X-Tenant-Id: tenant-001
Content-Type: application/json
```

### 본문

```json
{
  "productCode": "A001",
  "quantity": 10
}
```

### 필드

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | :---: | --- |
| productCode | String |        Y | 상품 코드 |
| quantity    | Long   |        Y | 출고 수량          |

### 검증

* `productCode`는 영문 대소문자, 숫자, `_`, `-`로 이루어진 1~100자다. 대소문자를 구분한다.
* `quantity`는 1 이상 1,000,000,000 이하의 정수여야 한다. 상한은 설정으로 관리한다.
* `quantity`는 JSON 정수만 받는다. 소수(`1.5`)나 문자열(`"10"`)은 `INVALID_REQUEST`다.

---

## 성공 응답

### 200 OK

```json
{
  "productCode": "A001",
  "productName": "Apple",
  "quantity": 90,
  "updatedAt": "2026-09-24T22:10:00+09:00"
}
```

`quantity`는 이 요청의 출고가 반영된 직후의 재고 수량이다.

---

## 오류 응답

### 400 Bad Request

#### 잘못된 수량

```json
{
  "code": "INVALID_QUANTITY",
  "message": "출고 수량이 허용 범위를 벗어났습니다."
}
```

#### 필수 요청 정보 누락

```json
{
  "code": "INVALID_REQUEST",
  "message": "필수 요청 정보가 누락되었습니다."
}
```

#### 요청 형식 오류

```json
{
  "code": "INVALID_REQUEST",
  "message": "요청 형식이 올바르지 않습니다."
}
```

#### 업체 정보 누락 또는 미등록

```json
{
  "code": "INVALID_TENANT",
  "message": "Tenant 정보가 없거나 등록되지 않았습니다."
}
```

### 404 Not Found

#### 상품 없음

요청 업체에 해당 상품코드의 상품이 없다.

```json
{
  "code": "PRODUCT_NOT_FOUND",
  "message": "상품을 찾을 수 없습니다."
}
```

### 409 Conflict

#### 재고 부족

```json
{
  "code": "INSUFFICIENT_STOCK",
  "message": "출고 수량이 현재 재고보다 많습니다."
}
```

---

# 5. 현재 재고 조회

## GET `/inventory/{productCode}`

요청 업체에 속한 상품의 현재 재고를 조회한다.

---

## 요청

### 헤더

```http
X-Tenant-Id: tenant-001
```

### 경로 변수

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | :---: | --- |
| productCode | String |        Y | 상품 코드 |

경로의 상품코드는 별도로 형식을 검증하지 않는다. 허용 문자나 길이를 벗어난 코드는 등록될 수 없으므로 404로 응답한다.

---

## 성공 응답

### 200 OK

```json
{
  "productCode": "A001",
  "productName": "Apple",
  "quantity": 100,
  "updatedAt": "2026-09-24T22:10:00+09:00"
}
```

`quantity`는 조회 쿼리가 실행된 시점에 커밋되어 있던 재고 수량이다.

---

## 오류 응답

### 400 Bad Request

#### 업체 정보 누락 또는 미등록

```json
{
  "code": "INVALID_TENANT",
  "message": "Tenant 정보가 없거나 등록되지 않았습니다."
}
```

### 404 Not Found

#### 상품 없음

요청 업체에 해당 상품코드의 상품이 없다.

```json
{
  "code": "PRODUCT_NOT_FOUND",
  "message": "상품을 찾을 수 없습니다."
}
```

---

# 6. HTTP 상태 코드

| 상태 | 의미 |
| --- | --- |
| 200 OK                    | 요청을 정상적으로 처리함         |
| 400 Bad Request           | Tenant 정보, 요청 형식 또는 입력값이 올바르지 않음 |
| 404 Not Found             | 요청 업체의 상품 중 대상 상품을 찾을 수 없음, 또는 존재하지 않는 경로     |
| 405 Method Not Allowed    | 지원하지 않는 HTTP 메서드   |
| 409 Conflict              | 현재 상품 또는 재고 상태와 충돌함   |
| 500 Internal Server Error | 예상하지 못한 서버 오류         |

---

# 7. 에러 코드

| 코드 | 상태 | 설명 |
| --- | ---: | --- |
| `INVALID_TENANT`        |    400 | Tenant 정보가 없거나, 비어 있거나, 등록되지 않음 |
| `INVALID_REQUEST`       |    400 | 필수 요청 정보가 누락되었거나, 형식이 올바르지 않거나, 허용 문자·길이를 벗어남 |
| `INVALID_QUANTITY`      |    400 | 입고/출고 수량이 1 미만이거나 상한을 넘음 |
| `PRODUCT_NOT_FOUND` | 404 | 요청 업체에 해당 상품코드의 상품이 없음 |
| `PRODUCT_NAME_MISMATCH` |    409 | 기존 상품과 요청 상품명이 다름            |
| `INSUFFICIENT_STOCK`    |    409 | 현재 재고보다 출고 요청 수량이 많음         |
| `RESOURCE_NOT_FOUND`    |    404 | 존재하지 않는 경로 |
| `METHOD_NOT_ALLOWED`    |    405 | 지원하지 않는 HTTP 메서드 |
| `INTERNAL_SERVER_ERROR` |    500 | 예상하지 못한 서버 오류 (DB 동시성 오류 포함) |

`INTERNAL_SERVER_ERROR` 응답에는 내부 처리 내용을 노출하지 않으며, 요청은 반영되지 않는다.

---

# 8. 제공하지 않는 API

현재 MVP에서는 다음 API를 제공하지 않는다.

```text
GET  /inventory
GET  /inventory/search
POST /inventory/inbound/bulk
POST /inventory/outbound/bulk
POST /products
```

상품은 별도의 상품 등록 API를 통해 생성하지 않으며, 신규 상품의 최초 입고 과정에서 생성한다.

입고 및 출고는 한 요청에서 하나의 상품을 처리한다.
