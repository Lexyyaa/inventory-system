# 도메인 모델

> 본 문서는 업무 규칙을 만족시키기 위한 도메인 구조와 데이터 정합성 설계를 정의한다.

---

# 1. 도메인 개요

재고관리 시스템의 핵심 도메인은 다음 세 가지로 구성한다.

```text
Tenant
   │
   │ 1:N
   ▼
Product
   │
   │ 1:1
   ▼
Inventory
```

각 도메인의 책임을 분리한다.

| 도메인 | 책임 |
| --- | --- |
| Tenant    | 업체별 데이터 경계     |
| Product   | 상품의 식별 및 기본 정보 |
| Inventory | 상품의 현재 재고 상태   |

---

# 2. 업체 (Tenant)

## 역할

Tenant는 상품과 재고를 구분하는 업체 단위다.

하나의 Tenant는 여러 Product를 가질 수 있다.

서로 다른 Tenant는 동일한 상품코드를 사용할 수 있다.

```text
Tenant A / A001
Tenant B / A001
```

---

## 필드

| 필드 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| id         | BIGINT       | PK, IDENTITY     | 내부 식별자       |
| code       | VARCHAR(100) | UNIQUE, NOT NULL | Tenant 식별 코드 |
| name       | VARCHAR(255) | NOT NULL         | 업체명          |
| created_at | TIMESTAMPTZ  | NOT NULL, DEFAULT now() | 생성 시각        |
| updated_at | TIMESTAMPTZ  | NOT NULL, DEFAULT now() | 마지막 변경 시각   |

---

## 식별과 준비

요청 Header `X-Tenant-Id`의 값은 `tenant.code`다.

서버는 요청마다 이 값으로 `tenant.id`를 찾는다. 찾지 못하면 요청을 거부한다.

Tenant 생성 API는 현재 MVP에서 제공하지 않는다.

Tenant는 기동 시 seed(`data.sql`)로 미리 등록한다. seed는 `INSERT ... ON CONFLICT DO NOTHING`으로 써서 재기동해도 중복되지 않는다.

```text
tenant-001 / 업체 A
tenant-002 / 업체 B
```

---

# 3. 상품 (Product)

## 역할

Product는 상품의 식별 정보와 기본 정보를 관리한다.

재고 수량은 Product에 저장하지 않는다.

---

## 필드

| 필드 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| id           | BIGINT       | PK, IDENTITY | 내부 식별자 (API에 노출하지 않음) |
| tenant_id    | BIGINT       | FK, NOT NULL | 소속 Tenant     |
| product_code | VARCHAR(100) | NOT NULL | Tenant 내 상품코드 |
| name         | VARCHAR(255) | NOT NULL     | 상품명           |
| created_at   | TIMESTAMPTZ  | NOT NULL, DEFAULT now() | 생성 시각         |
| updated_at   | TIMESTAMPTZ  | NOT NULL, DEFAULT now() | 마지막 변경 시각    |

---

## 상품 식별

상품은 다음 조합으로 식별한다.

```text
tenant_id + product_code
```

따라서 Database에서 다음 제약을 적용한다.

```sql
UNIQUE (tenant_id, product_code)
```

---

# 4. 재고 (Inventory)

## 역할

Inventory는 하나의 Product에 대한 현재 재고 상태를 관리한다.

현재 MVP에서는 Product 하나당 Inventory 하나를 가진다.

---

## 필드

| 필드 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| product_id | BIGINT      | PK, FK     | 대상 Product  |
| quantity   | BIGINT      | NOT NULL, CHECK (quantity >= 0) | 현재 재고       |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() | 최초 입고 시각   |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() | 마지막 변경 시각   |

---

## 관계

```text
Product 1 ───── 1 Inventory
```

`product_id`를 Primary Key로 사용하여 하나의 Product에 Inventory가 하나만 존재하도록 한다.

---

# 5. 엔티티 관계

```text
┌──────────────────────────┐
│ tenant                   │
├──────────────────────────┤
│ id PK                    │
│ code UNIQUE              │
│ name                     │
│ created_at               │
│ updated_at               │
└────────────┬─────────────┘
             │
             │ 1 : N
             │
┌────────────▼─────────────┐
│ product                  │
├──────────────────────────┤
│ id PK                    │
│ tenant_id FK             │
│ product_code             │
│ name                     │
│ created_at               │
│ updated_at               │
├──────────────────────────┤
│ UNIQUE                   │
│ (tenant_id, product_code)│
└────────────┬─────────────┘
             │
             │ 1 : 1
             │
┌────────────▼─────────────┐
│ inventory                │
├──────────────────────────┤
│ product_id PK/FK         │
│ quantity                 │
│ created_at               │
│ updated_at               │
├──────────────────────────┤
│ CHECK(quantity >= 0)     │
└──────────────────────────┘
```

---

# 6. 업체별 데이터 경계

Product 접근은 항상 요청 Tenant를 기준으로 제한한다.

상품코드만으로 상품을 식별하지 않고 다음 조건을 함께 사용한다.

```text
tenant_id = 현재 요청의 Tenant
product_code = 요청 상품코드
```

예:

```sql
SELECT *
FROM product
WHERE tenant_id = :tenantId
  AND product_code = :productCode;
```

따라서 Tenant A의 `A001` 요청이 Tenant B의 `A001` 상품에 접근할 수 없다.

---

# 7. 데이터베이스 제약

## 업체

```text
PRIMARY KEY (id)
UNIQUE (code)
```

---

## 상품

```text
PRIMARY KEY (id)
FOREIGN KEY (tenant_id)
REFERENCES tenant(id)

UNIQUE (tenant_id, product_code)
```

---

## 재고

```text
PRIMARY KEY (product_id)
FOREIGN KEY (product_id)
REFERENCES product(id)

CHECK (quantity >= 0)
```

데이터베이스 제약은 애플리케이션 로직과 별개로 최종 데이터 무결성을 보장하기 위해 사용한다.

---

## 스키마 관리

`schema.sql`이 제출용 DDL이자 스키마의 유일한 기준이다.

* 애플리케이션은 기동 시 `schema.sql`과 `data.sql`(Tenant seed)을 실행한다
* `schema.sql`은 `CREATE TABLE IF NOT EXISTS`로 써서 재기동해도 로컬 데이터가 남는다
* JPA는 스키마를 만들지 않고 검증만 한다 (`ddl-auto: validate`)
* `defer-datasource-initialization: false`로 두어 `schema.sql` → `data.sql` → JPA 검증 순서로 실행한다
* 테스트와 local 프로파일도 같은 `schema.sql`을 사용하며, 프로파일에서 `ddl-auto`를 덮어쓰지 않는다
* id는 `GENERATED ... AS IDENTITY`로 생성하고 엔티티도 IDENTITY로 매핑한다

---

## 시각 컬럼

모든 테이블에 `created_at`과 `updated_at`을 두고, 값은 DB가 채운다.

* 컬럼에 `DEFAULT now()`를 두고, 원자 쿼리에서도 값을 직접 넣는다
* 엔티티는 매핑 전용 `BaseTimeEntity`를 상속해 두 시각을 읽기만 한다
* 저장이 native 쿼리라 JPA Auditing은 쓰지 않는다
* Java에서는 `Instant`로 다루고, 응답을 만들 때 `+09:00`으로 표기한다
* 업체와 상품의 `updated_at`은 변경 기능이 없어 생성 시각과 같게 남는다

`updated_at`은 `GREATEST(inventory.updated_at, clock_timestamp())`로 갱신한다.

* `CURRENT_TIMESTAMP`는 Transaction 시작 시각이다
* 먼저 시작했지만 잠금을 늦게 얻은 Transaction이 더 과거 시각으로 덮어쓸 수 있다
* `clock_timestamp()`는 실제 실행 시각이고, `GREATEST`로 값이 뒤로 가지 않게 한다

---

# 8. 상품 입고의 데이터 흐름

입고는 신규 상품과 기존 상품을 나누지 않고 하나의 Transaction 안에서 같은 흐름으로 처리한다.

```text
입고 요청
    ↓
Tenant 확인 (Transaction 밖, 요청 본문 해석 전)
    ↓
Product 생성 시도 (이미 있으면 기존 Product 확인)
    ↓
상품명 검증
    ↓
Inventory 생성 또는 재고 증가
    ↓
Commit
```

Tenant 확인은 요청 본문을 해석하기 전에 웹 계층에서 한다. 그래야 Tenant 오류가 본문 검증 오류보다 먼저 반환된다.

Transaction에는 확인된 `tenant_id`만 넘어간다.

Product 생성과 Inventory 반영 중 하나라도 실패하면 전체 Transaction을 rollback한다.

---

# 9. 신규 상품 동시 생성

동일 Tenant의 동일 `product_code`에 대해 여러 요청이 동시에 Product 생성을 시도할 수 있다.

Product의 유일성은 애플리케이션의 존재 여부 확인만으로 보장하지 않고 Database의 다음 제약으로 최종 보장한다.

```text
UNIQUE (tenant_id, product_code)
```

상품 생성은 `INSERT ... ON CONFLICT DO NOTHING`으로 한다.

```sql
INSERT INTO product (tenant_id, product_code, name, created_at)
VALUES (:tenantId, :productCode, :name, CURRENT_TIMESTAMP)
ON CONFLICT (tenant_id, product_code) DO NOTHING
RETURNING id;
```

`RETURNING` 결과와 관계없이 같은 Transaction에서 상품을 다시 조회한다. `RETURNING`이 비어 있으면 이미 있는 상품이라는 뜻이다.

다시 조회하면 신규 · 기존 상품이 같은 검증 경로를 타고, 응답은 항상 저장된 값을 쓴다. 신규 상품일 때 SELECT가 한 번 더 나간다.

```sql
SELECT id, name
FROM product
WHERE tenant_id = :tenantId
  AND product_code = :productCode;
```

동시에 생성한 다른 요청이 먼저 커밋했다면, 다시 조회할 때 그 상품이 보인다.

하나의 Product만 생성되고, 이후 각 요청은 상품명을 검증한 뒤 해당 Product에 자신의 입고 수량을 반영한다.

JPA `save` 후 중복 예외를 잡아 다시 조회하는 방식은 쓰지 않는다.

PostgreSQL은 오류가 난 Transaction에서 이후 쿼리를 실행하지 않기 때문이다.

---

# 10. 재고 증가

재고 증가 시 현재 수량을 애플리케이션에서 조회한 후 계산하여 저장하는 read-modify-write 방식은 사용하지 않는다.

다음과 같은 원자적인 연산으로 처리한다.

```text
현재 quantity + 요청 quantity
```

구체적으로는 Inventory UPSERT를 사용한다.

```sql
INSERT INTO inventory (
    product_id,
    quantity,
    updated_at
)
VALUES (
    :productId,
    :quantity,
    clock_timestamp()
)
ON CONFLICT (product_id)
DO UPDATE SET
    quantity = inventory.quantity + EXCLUDED.quantity,
    updated_at = GREATEST(inventory.updated_at, clock_timestamp())
RETURNING quantity, updated_at;
```

이를 통해 동시에 여러 입고가 발생해도 각 요청의 증가량이 덮어써지지 않도록 한다.

`RETURNING` 값이 이 요청이 반영된 직후의 재고이며, 입고 응답에 그대로 사용한다.

---

# 11. 재고 감소

출고에서는 재고 확인과 차감을 별도의 작업으로 분리하지 않는다.

먼저 요청 Tenant와 상품코드로 Product를 조회한다. 없으면 상품이 없는 것으로 처리한다. 다른 Tenant에만 있는 상품코드도 여기서 걸러진다.

그다음 다음 조건을 만족할 때만 하나의 상태 변경으로 처리한다.

```text
현재 quantity >= 요청 quantity
```

```sql
UPDATE inventory
SET
    quantity = quantity - :quantity,
    updated_at = GREATEST(updated_at, clock_timestamp())
WHERE product_id = :productId
  AND quantity >= :quantity
RETURNING quantity, updated_at;
```

반환된 행이 있으면 출고 성공이며, 그 값을 출고 응답에 사용한다.

반환된 행이 없으면 재고 부족으로 판단한다.

Product가 있으면 Inventory도 반드시 있으므로 반환된 행이 없는 이유는 재고 부족뿐이다.

이를 통해 동시에 출고 요청이 발생하더라도 실제 재고를 초과하는 출고가 성공하지 않도록 한다.

---

# 12. 동시 입고와 출고

입고와 출고는 각각 Transaction 안에서 재고를 변경한다.

재고 변경은 데이터베이스의 원자적인 UPDATE/UPSERT를 통해 수행한다.

따라서 재고의 최종 상태는 실제로 먼저 변경되어 커밋된 결과와 이후 요청의 처리 결과에 따라 결정된다.

커밋되지 않은 중간 상태를 다른 Transaction의 정상적인 재고 상태로 사용하지 않는다.

---

## 재고 변경 경로

Inventory는 위의 원자 쿼리로만 변경한다.

원자 쿼리 전에 읽어 둔 엔티티는 변경 전 값을 가지므로 응답에 사용하지 않는다.

---

# 13. 재고 조회

재고 조회는 요청 업체의 Product를 찾은 뒤, 그 Product의 Inventory를 식별자로 조회한다.

* Product: `tenant_id + product_code`로 조회한다. 없으면 상품 없음으로 응답한다
* Inventory: `product_id`(PK)로 조회한다
* 직접 작성한 쿼리 없이 Spring Data 조회 메서드로 처리한다

상품명은 생성 뒤 바뀌지 않으므로, 두 번 나누어 읽어도 응답 값은 JOIN 한 번으로 읽은 값과 같다.

Product가 있으면 Inventory도 있다. 입고가 두 행을 한 Transaction에서 만들기 때문이다. Inventory가 없으면 서버 결함으로 본다.

조회에는 재고 변경을 위한 별도의 비관적 Lock을 사용하지 않는다.

조회와 출고가 동시에 발생할 수 있으며, 조회 완료 이후 재고가 변경되는 것은 정상적인 상태 변화다.

---

# 14. 트랜잭션 경계

## 입고

```text
BEGIN
  ├─ Product 생성 시도 또는 기존 Product 조회
  ├─ 상품명 검증
  └─ Inventory 생성 또는 증가
COMMIT
```

---

## 출고

```text
BEGIN
  ├─ Product 조회
  └─ Inventory 조건부 차감
COMMIT
```

Tenant 확인은 두 흐름 모두 Transaction 밖, 요청 본문 해석 전에 한다.

Transaction 경계 밖에서 상품 생성만 성공하거나 재고 변경만 성공한 상태가 남지 않도록 한다.

---

# 15. 동시성 제어 선택

현재 MVP에서는 Redis Distributed Lock이나 별도의 애플리케이션 Lock을 사용하지 않는다.

재고 변경 자체가 PostgreSQL에서 원자적으로 처리되도록 하여 데이터베이스를 최종적인 상태 변경 주체로 사용한다.

현재 요구사항은 다음 방식으로 처리한다.

```text
입고
→ Atomic UPSERT

출고
→ Conditional Atomic UPDATE

신규 상품
→ UNIQUE + INSERT ON CONFLICT DO NOTHING
```

Optimistic Lock의 version 관리나 Pessimistic Lock의 명시적 row lock은 현재 요구사항에 추가하지 않는다.

---

## 격리 수준

모든 입출고와 조회는 PostgreSQL 기본 격리 수준인 READ COMMITTED에서 실행하고, 격리 수준을 올리지 않는다.

이 설계는 READ COMMITTED의 다음 동작에 기댄다.

* 조건부 UPDATE는 다른 Transaction이 잡은 행 잠금을 기다린 뒤, 최신 커밋 값으로 조건을 다시 평가한다. 그래서 재고가 충분한 출고는 경합 때문에 실패하지 않는다
* `INSERT ... ON CONFLICT DO NOTHING` 뒤의 SELECT는 경쟁에서 이긴 Transaction이 커밋한 상품을 본다

REPEATABLE READ 이상에서는 같은 경합이 직렬화 실패(40001)로 끝나므로 쓰지 않는다.

그래도 직렬화 실패, 교착(40P01), 커넥션 대기 초과가 생기면 500으로 응답한다. Transaction은 rollback되어 요청은 반영되지 않으며, 서버는 자동으로 재시도하지 않는다.

---

# 16. 도메인 불변 조건

도메인에서 항상 유지해야 하는 조건은 다음과 같다.

```text
1. Tenant 내부에서 productCode는 유일하다.

2. Product 하나에 Inventory 하나만 존재한다.

3. Inventory.quantity >= 0

4. 정상적으로 처리된 입고 수량은 재고에 반영된다.

5. 현재 재고보다 많은 출고는 성공할 수 없다.
```

이 중 데이터 구조로 직접 표현할 수 있는 조건은 Database Constraint로도 보장한다.

2번은 두 부분으로 나누어 보장한다.

* 최대 하나: `inventory.product_id` Primary Key
* 반드시 하나: Product와 Inventory를 같은 Transaction에서 생성하고, 삭제 기능이 없음
