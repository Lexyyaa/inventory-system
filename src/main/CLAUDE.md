# src/main 코드 규칙

`src/main` 아래 파일을 읽을 때 루트 CLAUDE.md와 함께 로드된다.
도구(ArchUnit, Spotless)가 막는 규칙은 적지 않는다.
사람이 리뷰로 확인하는 규칙만 둔다.

## 패키지

```text
com.deepfine.inventorysystem
├── presentation
│   ├── controller/{domain}              Controller, {Resource}ApiDocs, {Resource}Request, {Resource}Response
│   └── interceptor                      TenantInterceptor
├── application/{domain}                 {Resource}ApplicationService, {Resource}Command, {Resource}Info
├── domain
│   ├── {domain}                         엔티티, VO, Enum, {Domain}Service(도메인 서비스), {Aggregate}Repository(인터페이스)
│   │   └── exception                    {Domain}Exception
│   ├── common                           BaseTimeEntity
│   └── exception                        ErrorCode, BusinessException
├── infrastructure
│   └── persistence/{domain}             {Aggregate}RepositoryImpl, {Aggregate}JpaRepository
└── support
    ├── config                           WebConfig, OpenApiConfig
    ├── properties                       @ConfigurationProperties
    └── exception                        ErrorResponse, GlobalExceptionHandler
```

- support는 어느 층과도 참조할 수 있다 (ArchUnit 층 검사에서 뺐다)

## 도메인

- 엔티티에 `@Entity`를 직접 붙인다
  - 별도 영속 모델과 매퍼는 두지 않는다
- 생성은 정적 팩토리로 한다
  - 불변식 검증을 그 안에서 끝낸다
- `@Setter` 금지
  - 상태 변경은 의도가 드러나는 메서드로 한다
- repository가 필요 없는 규칙은 엔티티 메서드로 둔다 (예: `Inventory.validateInboundQuantity`)
  - 인스턴스가 아직 없을 수 있는 검사는 정적 메서드로 둔다
  - 엔티티 메서드마다 도메인 단위 테스트를 둔다
- 여러 엔티티를 넘나드는 로직과 repository를 부르는 절차만 도메인 서비스(`domain/{domain}/{Domain}Service`)로 뺀다
  - 도메인 서비스는 자기 도메인의 repository와 엔티티 규칙으로 절차를 묶는다
  - 도메인 서비스에는 `@Transactional`을 두지 않는다
- 규칙이 실제로 여러 갈래일 때만 다형성(정책 인터페이스)을 쓴다
  - 규칙이 하나면 클래스 하나로 둔다
- 도메인 메서드에 DTO(Command 등)를 넘기지 않는다
- 엔티티/VO 생성자에서 null 검사를 반복하지 않는다
  - 입력 검증은 Request, 최후 방어선은 DB 제약이다
  - 엔티티에는 업무 규칙만 둔다
- 엔티티는 `domain/common/BaseTimeEntity`를 상속한다
  - 매핑 전용이다. 시각은 DB가 채우고 엔티티는 읽기만 한다 (Auditing 없음)
  - 모든 테이블에 `created_at`, `updated_at`을 둔다
  - Java 타입은 `Instant`. `+09:00` 표기는 응답을 만들 때 맞춘다
- 스키마의 기준은 `schema.sql`이다
  - 엔티티 매핑은 `ddl-auto: validate`를 통과하도록 맞춘다
  - 엔티티 간 참조는 식별자(Long)로 둔다

### VO 기준

- VO는 불변식이나 행동이 있을 때만 만든다
- 도메인 메서드 인자가 4개 이하이면 입력 VO 없이 그대로 넘긴다
  - 5개 이상이면 애그리거트 옆에 입력 VO(record)를 둔다
- 도메인 메서드의 반환용 Result 객체는 만들지 않는다
  - 필요한 값은 애그리거트에서 꺼낸다
  - 단 원자 SQL의 `RETURNING`(quantity, updated_at)은 `domain/{domain}`의 읽기 전용 record로 돌려준다
    - 예: `InventoryState(Long quantity, Instant updatedAt)`

### 메서드 길이

- 도메인 메서드는 30줄 이하로 둔다
- 검사가 여러 개면 의미 단위의 `private validateXxx()`로 묶는다
  - 순서의 원본은 `docs/design/04-api-spec.md` §2 "오류 판정 순서"다

## Repository 3단

- 도메인 인터페이스: `domain/{domain}/{Aggregate}Repository`
  - 조회 메서드는 `getByXxx()`
    - 없으면 예외
  - 없는 게 정상인 경우만 `findByXxx()`
    - `Optional`을 돌려준다
  - 예외: 있어야 하는데 없으면 서버 결함(500)인 경우도 `findByXxx()`
    - 도메인 서비스가 `orElseThrow(IllegalStateException)`로 푼다 (예: 생성을 시도한 상품의 재조회)
- 구현: `infrastructure/persistence/{domain}/{Aggregate}RepositoryImpl`
  - `@Repository`
  - `Optional`을 풀고 not-found 예외를 던지는 곳은 여기다
  - 조건부 차감(03 §11)이 빈 결과를 돌려주면 여기서 `INSUFFICIENT_STOCK`으로 던진다 (Product가 있으면 빈 결과는 재고 부족뿐이다)
- Spring Data: `{Aggregate}JpaRepository`
  - Impl만 사용한다
- 직접 쓰는 SQL은 동시성 장치(상품 생성 경합, 재고 증감)에만 쓴다
  - 나머지 조회와 검증은 Spring Data 메서드 이름과 엔티티 메서드로 한다
- 재고 변경과 상품 생성은 `03`의 원자 SQL(native)로 한다
  - `RETURNING` 결과를 받아 응답에 쓴다
  - `@Modifying`은 영향 행 수만 돌려주므로 `RETURNING`이 필요한 쿼리에 쓰지 않는다
  - `RETURNING` 결과는 domain record로 바로 받는다. 별칭을 record 필드명과 맞춘다 (`updated_at AS updatedAt`)
  - native 쿼리의 `timestamptz`는 Hibernate 6.6에서 `Instant`로 온다. record의 시각 필드는 `Instant`로 둔다 (`OffsetDateTime`이면 "argument type mismatch")

## DTO

- record + 이너 record
- 리소스 하나에 홀더 4개
  - presentation: `{Resource}Request`, `{Resource}Response`
  - application: `{Resource}Command`, `{Resource}Info`
- 홀더 안의 이너 record 이름은 실제 행위를 따른다
  - 예: `InventoryRequest.Inbound`, `InventoryRequest.Outbound`
  - `Create`/`Read` 같은 범용 이름은 피한다
- 흐름: Request → Command → (도메인 호출) → Info → Response
- `Info`와 `Command` 필드에 domain 타입(Enum, VO, 엔티티)을 담지 않는다
  - presentation은 domain을 참조할 수 없다
  - 담으면 `Response.from(info)`가 ArchUnit에 걸린다
- 변환 메서드 이름
  - `from(x)` — 정적. 재료 하나를 이 타입으로 바꾼다 (`Response.from(info)`)
  - `of(a, b)` — 정적. 재료 여러 개를 조립한다 (`Info.of(product, state)`)
  - `toXxx(...)` — 인스턴스. 나 자신을 다음 계층 타입으로 바꾸고, 부족한 값은 인자로 받는다 (`request.toCommand(tenantId)`)
  - 생성자와 인자가 같은 정적 팩토리는 만들지 않는다
- 요청 검증(필수값, 길이, 허용 문자)은 Request의 Bean Validation으로 한다. 수량 범위는 제외
- 컨트롤러는 세 줄로 쓴다. 한 줄에 중첩하지 않는다
  - `request.toCommand(tenantId)` → 서비스 호출 → `Response.from(result)`
- ApplicationService는 도메인 서비스를 조합하고 repository를 직접 부르지 않는다
- 응답에 내부 식별자(`id`)를 담지 않는다

## API 문서 (Swagger)

- 애너테이션은 `presentation/controller/{domain}/{Resource}ApiDocs` 인터페이스에 모은다
  - 컨트롤러는 이 인터페이스를 구현하고, 컨트롤러에는 Swagger 애너테이션을 붙이지 않는다
- 인터페이스에 붙이는 것
  - `@Tag`, `@Operation`(요약, 설명)
  - `@Parameter(in = HEADER)`로 `X-Tenant-Id`
  - `@ApiResponse`로 성공과 04의 오류 응답(상태, 에러 코드)을 API마다 모두
- Request/Response 필드에는 `@Schema`(설명, 예시값)를 붙인다

## 트랜잭션과 동시성

- `@Transactional`은 ApplicationService에 둔다
  - 조회는 `readOnly = true`
  - 격리 수준은 기본값(PostgreSQL READ COMMITTED)을 쓰고 올리지 않는다
- `open-in-view: false`다
  - 필요한 데이터는 트랜잭션 안에서 로드한다
- 트랜잭션은 ApplicationService 메서드 하나에 하나만 둔다
  - `REQUIRES_NEW`로 나누지 않는다. 상품만 남고 재고는 반영되지 않는 상태가 생긴다
- 상품 생성 경쟁은 `INSERT ... ON CONFLICT DO NOTHING`으로 처리한다
  - `DataIntegrityViolationException`을 잡아 재조회하지 않는다
  - PostgreSQL은 오류가 난 트랜잭션에서 이후 쿼리를 실행하지 않는다
- 원자 SQL 전에 읽어 둔 재고 값(quantity, updated_at)을 응답에 쓰지 않는다. 상품코드와 상품명은 먼저 읽은 Product 값을 쓴다 (상품명은 바뀌지 않는다)
- Tenant 확인은 `presentation/interceptor/TenantInterceptor.preHandle`(`API_PREFIX + "/**"`)에서 한다
  - Filter와 `@RequestHeader` 안에서 확인하지 않는다 (본문 해석보다 늦거나 에러 형식이 달라진다)
  - 인터셉터는 `TenantApplicationService`를 부르고, `INVALID_TENANT` 예외는 도메인의 `TenantService`가 던진다
  - 확인된 id는 request attribute(`TENANT_ID`)에 넣는다
  - 컨트롤러는 `@RequestAttribute(TENANT_ID) Long tenantId`로 받는다. ArgumentResolver는 만들지 않는다
  - 서비스에는 확인된 `tenantId`를 Command에 담아 넘긴다

## 예외

- 공통 베이스 `BusinessException`, `ErrorCode`는 `domain/exception`에 둔다
- 도메인별 예외는 `domain/{domain}/exception/{Domain}Exception` 하나만 둔다
  - `ErrorCode`를 생성자로 받는다
  - 상황별 예외 클래스는 만들지 않는다
- `ErrorCode`가 HTTP 상태를 갖는다
- `GlobalExceptionHandler`가 `BusinessException`을 한 곳에서 응답으로 바꾼다
  - 5xx 코드는 error 로그, 4xx는 info 로그로 남는다
  - Bean Validation/역직렬화 실패는 모두 `INVALID_REQUEST`로 바꾼다
  - `INVALID_QUANTITY`(1 미만, 상한 초과)는 ApplicationService가 properties 상한을 읽어 `Inventory.validateInboundQuantity`에 넘겨 검사한다
    - ApplicationService의 맨 앞, 상품 INSERT와 조회보다 먼저 부른다 (04 §2 판정 순서). Request의 quantity에는 `@NotNull`만 둔다
- 설계상 일어날 수 없는 상황(생성을 시도한 상품을 다시 읽지 못함 등)은 404가 아니라 500이다
- 에러 응답 본문은 `{ "code", "message" }`다 (`04` §2)
- 응답 `message`는 04 §3~§5 "오류 응답"의 문구를 그대로 쓴다
  - 같은 코드에 문구가 둘 이상이면 `BusinessException(errorCode, detail)`로 넘긴다
  - `INVALID_QUANTITY`는 입고/출고 문구, `INVALID_REQUEST`는 Bean Validation 위반이면 "필수 요청 정보가 누락되었습니다.", 역직렬화 실패면 "요청 형식이 올바르지 않습니다."
- `ErrorCode`에는 04 §7에 있는 코드만 둔다
  - 없는 코드가 필요하면 04 §7 추가안을 보고하고 멈춘다
- `catch (RuntimeException | Exception)`으로 뭉개지 않는다
  - 비즈니스 예외는 원래 코드로 흘려보낸다

## Lombok

- 의존성 주입은 `@RequiredArgsConstructor`만 쓴다
  - 직접 생성자는 금지다
- 엔티티
  - `@Getter`
  - `@NoArgsConstructor(access = AccessLevel.PROTECTED)`
  - 생성은 정적 팩토리 + `private` 생성자
- 값 객체는 record를 우선한다
- 인스턴스를 만들지 않는 홀더
  - `@NoArgsConstructor(access = AccessLevel.PRIVATE)`

## 설정

- 정책성 값은 코드에 하드코딩하지 않는다
  - 예: 수량 상한
  - `application.yml`에 둔다
  - `support/properties`의 `@ConfigurationProperties`로 둔다
  - ApplicationService가 읽어 엔티티 메서드 인자로 넘긴다
- 스키마는 `src/main/resources/schema.sql`
  - `CREATE TABLE IF NOT EXISTS`
  - `ddl-auto: validate`, `defer-datasource-initialization: false`
- seed는 `src/main/resources/data.sql`에 `INSERT ... ON CONFLICT DO NOTHING`으로 쓴다
  - 기동마다 실행된다 (`sql.init.mode: always`)
- Jackson은 소수 → 정수, 문자열 → 숫자 강제 변환을 끈다
- API 제목과 버전은 `support/config/OpenApiConfig`에 둔다
- API 경로 접두사는 `WebConfig.API_PREFIX` 한 곳에서 붙인다
  - 컨트롤러 `@RequestMapping`에는 `/api/v1`을 쓰지 않는다 (`/inventory`만)
  - presentation 패키지의 컨트롤러에만 붙는다
- 시간은 `Asia/Seoul` 기준이다 (Hibernate, Jackson, PostgreSQL 모두 설정됨)

## 주석

- 기본은 주석 없음. 이름으로 드러낸다
- 코드만 봐서는 알 수 없는 "왜"가 있을 때만 쓴다
  - 형식: 한 줄 요약 + 불릿 2~3개, 줄 끝마다 `<br>`
- 쓰지 않는 것
  - 문서 번호 (`03 §9`, `04 §2의 4번`, `TC-2-05`)
  - 이름을 되풀이하는 문장 ("입고한다")
  - 변명하듯 긴 이유, 비즈니스 규칙 설명 (설계 문서가 원본이다)
  - `{@code}`, `{@link}`, `<p>` 같은 Javadoc 태그 (`<br>`만 쓴다)
- Request/Response 필드의 `@Schema` 설명은 Swagger 문서라 남기되 짧게 쓴다
- 주석을 두는 자리
  - 동작 설명은 도메인 서비스 메서드에 둔다
  - 도메인 repository 인터페이스에는 주석을 두지 않는다
  - SQL을 그렇게 쓴 이유는 JpaRepository 쿼리 위에만 둔다

```java
/**
 * 입고 <br>
 * - 상품 생성, 상품명 검증, 재고 증가를 한 트랜잭션으로 처리 <br>
 * - 어느 단계든 실패하면 모두 롤백 <br>
 */
```
