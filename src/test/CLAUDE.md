# src/test 테스트 규칙

`src/test` 아래 파일을 읽을 때 루트 CLAUDE.md와 함께 로드된다.
테스트 케이스의 원본은 `docs/design/05-test-cases.md`다.
05에 없는 케이스를 만들었다면 05에 추가를 제안한다.

## 이름과 위치

- `@DisplayName`은 05에 적힌 문장을 그대로 쓴다
  - 형식: `@DisplayName("[TC-x-yy] …")`
- 테스트 클래스는 검증 대상의 이름과 패키지를 따른다
  - application/presentation: 서비스/컨트롤러 기준
    - `InboundConcurrencyTest`, `InboundApiTest`
- 본문은 `// given` `// when` `// then` 주석으로 나누고, 05의 given/when/then을 그대로 따른다
- 주석은 `@DisplayName`과 `// given` `// when` `// then`만 둔다
- 테스트 도구는 `support/` 바로 아래에 둔다
  - `support/` 하위 패키지(`support/exception` 등)에는 main `support` 코드의 테스트만 둔다

## 종류

05의 "테스트" 줄이 종류를 정한다.

| 종류 | 방식 | 위치 |
|---|---|---|
| 도메인 단위 | 순수 단위 테스트 (Spring, DB 없음) | `domain/**` |
| 통합 (API) | `@IntegrationTest` + 주입받은 `MockMvc` | `presentation/**` |
| 통합 (저장소) | `@IntegrationTest` + `JdbcTemplate`으로 직접 저장 | `infrastructure/**` |
| 구조 | ArchUnit | `architecture/ArchitectureTest` |

- TC-1-01/02의 테스트 전용 엔드포인트 `GET /api/v1/test/tenant`는 `src/test/java/…/presentation/interceptor` 아래 **최상위** `@RestController` 클래스로 둔다
  - 매핑은 `/test/tenant`만 적는다. `/api/v1`은 `WebConfig`가 presentation 패키지 컨트롤러에 붙인다
  - `src/main`에 두지 않는다
  - 테스트 클래스 안에 중첩하지 않는다 — 중첩하면 스캔에서 빠져 등록되지 않고, 인터셉터만 동작해 테스트가 거짓으로 통과한다

- 통합 테스트는 `support/IntegrationTest` 하나만 붙인다
  - `@SpringBootTest`를 직접 쓰거나 클래스마다 구성을 바꾸면
    - → 컨텍스트와 컨테이너가 매번 새로 뜬다
  - 예외: 예외 주입이 필요한 TC-2-10만 그 테스트 클래스 하나에 `@MockitoSpyBean`(재고 반영을 하는 RepositoryImpl)을 허용한다
    - 운영 코드에 테스트용 분기를 만들지 않는다
- 프로파일은 `test`다
  - 덮어쓸 설정은 `src/test/resources/application-test.yml`에 둔다
  - main 설정을 통째로 대체하지 않는다
  - `ddl-auto`는 덮어쓰지 않는다. 테스트도 `schema.sql`을 쓴다
- 원자 SQL, 트랜잭션, UNIQUE/CHECK 제약은 실제 PostgreSQL이 필요하다
  - H2로 대체하지 않는다

## 단언

- 상태코드만 단언하지 않는다
  - 응답 본문의 핵심 필드까지 확인한다
  - DB 상태(건수, 값)까지 확인한다
- 실패 케이스는 에러 코드까지 단언한다
  - API: 응답 본문의 `code`
  - 서비스/도메인: 예외 타입과 `ErrorCode`
- 롤백 케이스는 "예외가 났다"로 끝내지 않는다
  - 부분 저장이 없는지 확인한다
- 입력 오류 케이스는 500이 아니라 4xx인지 확인한다

## 동시성

- `support/ConcurrencyRunner.run(스레드 수, 작업)`으로 실제 경합을 만든다
  - 내부 동작
    - 시작 래치로 모든 스레드를 같은 시점에 출발시킨다
    - 30초 안에 안 끝나면 실패시킨다 (데드락 의심)
- 통합(API) 동시성은 스레드마다 MockMvc로 요청하고 `MvcResult`의 상태코드로 성공/실패를 센다
  - `ConcurrencyRunner`의 `successCount`는 예외 여부만 센다
- 결과는 DB 또는 05에 적힌 조회 API로 다시 읽어 확인한다
  - 생성 건수
  - 재고 값
  - 성공/실패 건수와 판정 기준 식(최종 재고 = 초기 + Σ성공 입고 − Σ성공 출고)
- 동시성 테스트는 `@Transactional` 롤백을 쓸 수 없다
  - 테스트가 만든 데이터는 직접 정리한다

## 데이터

- 테스트 DB에도 `data.sql` seed(`tenant-001`, `tenant-002`)가 들어간다
  - 건수 단언은 테스트가 만든 상품코드로 필터한다
- 테스트에 필요한 상품과 재고는 테스트가 직접 만든다
  - 입고 API를 거치지 않고 `support/InventoryTestDb`로 DB에 직접 넣는다 (입고가 깨져도 조회와 출고 테스트가 같이 깨지지 않게)
  - 각 테스트 전에 product와 inventory만 비운다. seed 업체는 그대로 둔다
- POST 요청에는 `Content-Type: application/json`을 붙인다
- DB 제약 테스트(데이터 제약 케이스)
  - `ON CONFLICT` 없는 INSERT로 직접 저장한다
  - 테스트 메서드에 트랜잭션을 걸지 않고 문장마다 커밋한다
  - PostgreSQL은 오류가 난 트랜잭션에서 이후 쿼리를 실행하지 않는다
- seed를 바꾸거나 지우는 테스트를 만들지 않는다
  - 같은 컨텍스트를 쓰는 다른 테스트가 깨진다
