---
name: verify-http
description: 서버를 띄워 .http 실행 케이스를 재연하고 기대값과 대조한다.
argument-hint: "[F번호 또는 .http 파일명 — 비우면 전체]"
disable-model-invocation: true
---

# /verify-http $ARGUMENTS — 실측

대상: `$ARGUMENTS` (비어 있으면 `http/*.http` 전체)

- 서버를 띄워 .http 실행 케이스를 curl로 재연한다
- 대조 항목: 상태코드·응답 본문·DB 상태
- 코드는 고치지 않는다

`/run-feature` 3단계도 이 절차를 그대로 따른다.
이 스킬은 **확인만** 한다.

- 코드 · 문서를 고치지 않는다
- 커밋하지 않는다

## .http 작성 규칙

기대값을 요청 바로 위 주석에 적는다. 이 주석이 대조 기준이다.

```http
@host = http://localhost:8080

### [TC-2-01] 신규 상품 입고
# @expect 200
# @expect $.quantity == 10
# @db SELECT i.quantity FROM inventory i JOIN product p ON p.id = i.product_id JOIN tenant t ON t.id = p.tenant_id WHERE t.code = 'tenant-001' AND p.product_code = 'IN-A001' => 10
POST {{host}}/api/v1/inventory/inbound
X-Tenant-Id: tenant-001
Content-Type: application/json

{ "productCode": "IN-A001", "productName": "Apple", "quantity": 10 }

### [TC-2-04] 상품명 불일치 입고
# @expect 409
# @expect $.code == "PRODUCT_NAME_MISMATCH"
POST {{host}}/api/v1/inventory/inbound
X-Tenant-Id: tenant-001
Content-Type: application/json

{ "productCode": "IN-A001", "productName": "Samsung", "quantity": 5 }
```

- `# @expect {상태코드}` — 필수
- `# @expect $.code == "..."` — 에러 케이스는 필수 (04 §2 공통 오류 응답)
  - 같은 400끼리 거짓 통과가 나지 않게
- `# @expect $.필드 == 값` — 부분 성공 · 생성 API는 필수
  - 상태코드만 보면 거짓 통과가 난다
- `# @db {SQL} => {기대값}` — 저장 · 변경이 있는 요청은 필수
- 앞 요청의 응답 값(id 등)을 쓰는 요청은 의존을 적는다
  - 선언: `# @uses {변수} = [TC-x-yy].$.경로`
  - 사용: 경로 · 헤더 · 본문에 `{{변수}}`
  - 같은 파일의 앞 요청만 참조한다 (run_http.py는 파일마다 응답을 새로 기억한다)
  - 파일에 필요한 상품은 그 파일 안의 준비 입고로 만든다
  - 상품코드에 파일별 접두어(inbound `IN-`, query `Q-`, outbound `OUT-`)를 붙여 전체 실행에서도 값이 겹치지 않게 한다

## 순서

1. **대상 결정**
   - 인자가 `F번호`면 task_list에서 브랜치 이름을 찾아 `http/{name}.http`
   - 파일명이면 그 파일
   - 없으면 전체 — task_list의 F 순서대로 파일을 돈다
2. **DB 초기화 · 서버 기동**
   - `docker compose down -v && docker compose up -d`
     - 매번 빈 DB에서 시작한다
     - 이전 실행의 데이터(상품 · 누적 재고)가 남으면 거짓 불일치가 난다
     - `schema.sql` 변경도 이렇게 반영된다 (`IF NOT EXISTS`라 볼륨을 지워야 한다)
   - 아래 명령이 성공할 때까지 대기한다
     - `docker exec inventory-system-postgres psql -h localhost -U app -d inventory-system -c "SELECT 1"`
     - `-h localhost`로 TCP 접속한다. 초기화 중인 임시 서버는 소켓으로만 받으므로 초기화가 끝나야 성공한다
   - 8080이 비어 있는지 확인한다: `lsof -i :8080`
     - 남아 있으면 `lsof -ti :8080 | xargs kill` 후 다시 확인
   - `./gradlew bootRun > build/bootrun.log 2>&1 &`
     - → `curl -s localhost:8080/actuator/health`가 `UP`일 때까지 대기
     - 최대 90초
   - 기동 실패 → `build/bootrun.log` 마지막 부분과 함께 🛑 멈춤
3. **요청 재연 · 대조**: `python3 scripts/run_http.py http/{name}.http`
   - 요청을 파일 순서대로 보내고 `@uses` 치환, 상태코드 · `$.필드` · `@db`를 대조해 표로 출력한다
   - 불일치가 있으면 종료 코드 1, "불일치" 절에 기대 · 실제가 나온다
   - DB 접속 기본값은 `inventory-system-postgres` / `inventory-system`이다 — 바꿨으면 `PG_CONTAINER` · `PG_USER` · `PG_PASSWORD` · `PG_DATABASE`로 넘긴다
   - 멀티파트 요청은 스크립트가 지원하지 않는다 → `curl -F 'files=@http/sample/파일'`로 따로 보내 대조한다
4. **로그 확인**
   - `build/bootrun.log`에 `ERROR`가 새로 찍혔는지 확인한다
     - 4xx 기대 요청에서 ERROR가 나오면 불일치로 본다
5. **서버 종료**
   - `lsof -ti :8080 | xargs kill`
   - `lsof -i :8080`이 비었는지 확인한다
   - PostgreSQL 컨테이너는 켜 둔다

## 보고

```
## 실측 결과 — 대상 … (n건)

| TC | 요청 | 상태 | 본문 | DB | 결과 |
|---|---|---|---|---|---|
| TC-2-01 | POST /api/v1/inventory/inbound | 200 ✅ | quantity ✅ | 10 ✅ | ✅ |
| TC-2-06 | POST /api/v1/inventory/inbound | 500 ❌ (기대 400) | — | — | ❌ |

## 불일치
- [TC-2-06] 기대 400 INVALID_QUANTITY / 실제 500 — 서버 로그: (핵심 줄)
  - 추정 원인: …

## 기대값이 없는 요청
- (@expect가 빠진 요청 — 작성 규칙 위반)
```

불일치가 있으면 여기서 끝낸다.
고치는 것은 호출한 쪽(`/run-feature` 또는 사용자)이 정한다.
