# 구현 체크리스트

이전 과제에서 반복된 실수를 분류별로 모았다.

## 사용법

- implementer
  - 커밋 전에 이번 작업이 건드린 분류를 훑는다
  - 해당 없는 분류는 건너뛴다
- reviewer
  - 이 목록을 리뷰 기준으로 쓴다
  - 걸린 항목은 번호 대신 문장 앞부분을 인용해 지적한다
- 형식: `확인할 것 — 틀리면 생기는 일 (막는 부품)`
- 새로 겪은 실수는 해당 분류 끝에 한 줄씩 추가한다

## 입력 · 검증

- [ ] 리스트 요청 필드에 `@Valid`가 붙어 있다 — 요소 검증이 안 돌아 null·음수가 도메인까지 가서 500
- [ ] 리스트 입력의 요소 중복을 검사한다 — 같은 항목이 두 번 처리된다
- [ ] 잘못된 입력은 도메인에서 `BusinessException`으로 먼저 막는다 — VO 생성자의 `IllegalArgumentException`이 새서 500
- [ ] 계산·검증은 DTO가 아니라 도메인 메서드에 있다 — 규칙이 흩어지고 도메인 테스트로 못 잡는다
- [ ] "무시한다"고 정한 입력 필드에 Bean Validation이 남아 있지 않다 — 무시될 값 때문에 400
- [ ] `Info` · `Command`에 domain Enum · VO · 엔티티를 담지 않는다 — presentation이 domain을 참조해 ArchUnit 실패 (`name()` · 원시값으로 푼다)
- [ ] `catch (RuntimeException | Exception)`이 `BusinessException`을 삼키지 않는다 — 원래 에러 코드 대신 엉뚱한 응답
- [ ] 문자열 길이 상한은 DB처럼 글자(코드포인트) 단위로 센다 (`@CodePointLength`) — `@Size`는 UTF-16 단위라 이모지 상품명이 DB 범위 안인데 400
- [ ] PostgreSQL에 저장할 수 없는 NUL · 짝 없는 서로게이트는 Request에서 400으로 막는다 — NUL은 500, 서로게이트는 다른 문자로 저장돼 다음 입고가 409
- [ ] 컨트롤러 매핑에 `consumes` · `produces`를 두지 않는다 — Content-Type 오류가 업체 확인보다 먼저 400 `INVALID_REQUEST`가 되어 04 §2 순서가 뒤집힌다 (TC-2-08 요청 4)
- [ ] 출고의 `INVALID_QUANTITY`는 출고 문구("출고 수량이 허용 범위를 벗어났습니다.")를 `detail`로 넘긴다 — `ErrorCode` 기본 문구가 입고 문구라 빠뜨리면 출고 응답에 "입고 수량이 …"가 나간다
- [ ] 검사 순서가 04 §2 "오류 판정 순서"와 같다 (Tenant → 형식 → 필수값·길이 → 수량 범위 → 상품 존재 → 상품·재고 상태) — 같은 요청에 설계와 다른 에러 코드

## 날짜 · 시간

- [ ] 하위 기간이 부모 기간 안에 있는지 검사한다 — 부모 기간 밖 하위 기간이 부모 종료일만 늘려 규칙에 없는 혜택이 생긴다
- [ ] 부모 기간이 줄어드는 경로(해제 · 취소 · 단축)에서도 위 포함 검사를 다시 한다 — 줄어든 뒤 하위 기간이 밖으로 삐져나온다
- [ ] 여러 날 구간의 포함 여부를 시작일이 아니라 끝 날짜까지 비교한다 — 구간 끝이 기준일을 넘는 경우를 놓친다
- [ ] 시각 컬럼은 DB가 채운다 — 엔티티가 `BaseTimeEntity`를 상속하면 03 §7과 어긋나고 native 쿼리에 Auditing이 적용되지 않는다

## 락 · 트랜잭션 · 동시성

- [ ] 같은 클래스 안의 `@Transactional` 호출에 기대지 않는다 — 프록시를 안 타서 트랜잭션이 안 열린다 (`TransactionRunner`)
- [ ] 파일 · 외부 I/O와 DB 커밋의 순서를 정했다 — 커밋 후 I/O가 실패하면 레코드만 남는다 (트랜잭션 안으로 옮기거나 보상)
- [ ] 상품 생성 경쟁은 `INSERT … ON CONFLICT DO NOTHING RETURNING id` 후 같은 트랜잭션 재조회다 — 예외를 잡아 재조회하면 PostgreSQL이 이후 쿼리를 거부한다 (03 §9)
- [ ] 입고는 트랜잭션 하나, 재고 변경은 원자 SQL 한 문장이다. `FOR UPDATE` · `REQUIRES_NEW` · 격리 수준 상향을 쓰지 않는다 — 상품만 남는 상태가 생긴다 (03 §14 · §15)
- [ ] 동시성 처리가 03 §9~§15(원자 SQL · READ COMMITTED)와 같다 — 임의로 바꾸면 동시성 테스트 근거가 사라진다

## JPA · 스키마

- [ ] native 쿼리(RETURNING · JOIN)의 `timestamptz`를 인프라 projection으로 받아 RepositoryImpl에서 `OffsetDateTime`으로 바꾼다 — Hibernate 6.6이 `Instant`로 읽어 도메인 record로 바로 받으면 변환 실패

- [ ] 03 §7의 UNIQUE · CHECK · FK가 `schema.sql`에 있고 엔티티 매핑이 그와 같다 — `ddl-auto: validate`는 제약을 검사하지 않는다
- [ ] `schema.sql`은 `CREATE TABLE IF NOT EXISTS`, `data.sql`은 `ON CONFLICT DO NOTHING`이다 — 재기동할 때 기동이 실패한다
- [ ] `@OneToMany` 컬렉션 순서에 의존하면 `@OrderBy`가 있다 — 순서가 DB 마음대로 바뀐다
- [ ] `insertable = false`로 이중 매핑한 FK 필드를 저장 직후 읽지 않는다 — 메모리 값이 null (연관에서 꺼내거나 다시 조회)
- [ ] 필요한 연관은 트랜잭션 안에서 로드한다 — `open-in-view: false`라 밖에서 `LazyInitializationException`

## 페이지 · 조회

- [ ] 조회 서비스는 `@Transactional(readOnly = true)`다 — 불필요한 flush · 쓰기 락
- [ ] 없으면 예외인 조회는 `getByXxx`, 없는 게 정상이면 `findByXxx`다 — not-found가 null로 새서 500

## 테스트

- [ ] 부분 성공 API를 상태코드만이 아니라 본문 · DB 건수로 검증한다 — 200인데 저장 0건인 거짓 통과
- [ ] 날짜 경계값은 저장 후 다시 읽어 단언한다 — 메모리 값만 보면 DB에서 바뀐 날짜를 놓친다
- [ ] 구간 경계 테스트에 2일 이상 구간이 있다 — 1일짜리만 있으면 시작일 비교 버그가 통과한다
- [ ] 실패 케이스는 예외 타입과 `ErrorCode`까지 단언한다 — 엉뚱한 예외로 실패해도 통과한다
- [ ] 입력 오류 케이스가 500이 아니라 4xx인지 본다 — 검증 누락이 숨는다
- [ ] 동시성 테스트는 05에 적힌 스레드 수 그대로 `ConcurrencyRunner`로 돌리고, 결과를 DB나 05의 조회 API로 다시 읽는다 — 경합이 안 생겨 거짓 통과 (`ConcurrencyRunner`)
