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
- [ ] 검사 순서가 04 API "판정 순서"와 같다 (입력 → 상태 → 한도 → 충돌) — 같은 요청에 설계와 다른 에러 코드

## 날짜 · 시간

- [ ] 날짜 입력의 상한과 하한을 짝으로 막는다 — PostgreSQL DATE 범위 밖 값은 저장 시점에 500, 0 이하 연도는 BC로 바뀌어 한 해 어긋난다 (허용 범위 1000-01-01 ~ 9999-12-31) (`DateRules.isStorable`)
- [ ] 날짜 계산(`plusDays` · `plusMonths`)보다 범위 검사가 먼저다 — ISO 파싱은 `+999999999-12-31`까지 받아 계산이 `DateTimeException`(500) (`DateRules.canAddDays`)
- [ ] 하위 기간이 부모 기간 안에 있는지 검사한다 — 부모 기간 밖 하위 기간이 부모 종료일만 늘려 규칙에 없는 혜택이 생긴다
- [ ] 부모 기간이 줄어드는 경로(해제 · 취소 · 단축)에서도 위 포함 검사를 다시 한다 — 줄어든 뒤 하위 기간이 밖으로 삐져나온다
- [ ] 여러 날 구간의 포함 여부를 시작일이 아니라 끝 날짜까지 비교한다 — 구간 끝이 기준일을 넘는 경우를 놓친다
- [ ] 비즈니스 판단 시각은 `BaseTimeEntity`가 아니라 별도 필드다 — 수정 시 판단 기준이 바뀐다

## 락 · 트랜잭션 · 동시성

- [ ] 락 → 검사 → 삽입에서 락 조회가 트랜잭션의 첫 쿼리다 — REPEATABLE READ 스냅샷이 락 이전에 잡혀 먼저 커밋된 행을 못 보고 중복 생성 (`TransactionRunner`)
- [ ] 락을 두 단계로 잡을 때 정합성 근거가 첫 일반 조회보다 앞선 락이다 — 뒤의 락이 스냅샷을 새로 잡아 주지 않는다
- [ ] `FOR UPDATE` 전에 같은 엔티티를 일반 조회로 영속성 컨텍스트에 올리지 않는다 — 잠금 조회가 1차 캐시의 옛 값을 돌려준다 (후보는 id만 조회)
- [ ] 락은 PK 등호 조회로 잡는다 — 범위 조건 `FOR UPDATE`는 갭 락으로 다른 키의 INSERT까지 대기시킨다
- [ ] `FOR UPDATE` 조건 컬럼에 인덱스가 있다 — 스캔한 행이 전부 잠겨 무관한 요청까지 직렬화된다
- [ ] 새 스냅샷이 필요한 곳은 `REQUIRES_NEW`다 — 기본 전파(REQUIRED)는 바깥 트랜잭션에 합류해 옛 스냅샷을 쓴다 (`TransactionRunner`)
- [ ] `TransactionRunner`를 호출자 트랜잭션 밖에서 부른다 — 안에서 부르면 커넥션 2개를 잡고, 같은 락을 쥐고 있으면 자기 대기로 타임아웃
- [ ] 같은 클래스 안의 `@Transactional` 호출에 기대지 않는다 — 프록시를 안 타서 트랜잭션이 안 열린다 (`TransactionRunner`)
- [ ] UNIQUE 충돌은 트랜잭션 밖에서 잡고 새 트랜잭션에서 재조회한다 — rollback-only 트랜잭션 안에서 조회가 실패한다
- [ ] 파일 · 외부 I/O와 DB 커밋의 순서를 정했다 — 커밋 후 I/O가 실패하면 레코드만 남는다 (트랜잭션 안으로 옮기거나 보상)
- [ ] 락 전략이 03 도메인 모델 §7과 같다 — 임의로 바꾸면 동시성 테스트 근거가 사라진다

## JPA · 스키마

- [ ] ERD의 UNIQUE · INDEX · NOT NULL이 `@Table` · `@Column(nullable = false)`에 선언돼 있다 — 엔티티에 없으면 DB에도 없다 (`ddl-auto: update`)
- [ ] `data.sql` INSERT에 `created_at` · `updated_at`(`NOW(6)`)이 있다 — `BaseTimeEntity` 컬럼은 NOT NULL · DEFAULT 없음이라 기동 실패
- [ ] `@OneToMany` 컬렉션 순서에 의존하면 `@OrderBy`가 있다 — 순서가 DB 마음대로 바뀐다
- [ ] `insertable = false`로 이중 매핑한 FK 필드를 저장 직후 읽지 않는다 — 메모리 값이 null (연관에서 꺼내거나 다시 조회)
- [ ] 필요한 연관은 트랜잭션 안에서 로드한다 — `open-in-view: false`라 밖에서 `LazyInitializationException`

## 페이지 · 조회

- [ ] 페이지 파라미터에 `@Min(0) @Max(PageLimits.MAX_PAGE)`, `@Min(1) @Max(PageLimits.MAX_SIZE)`가 있다 — offset이 int를 넘어 500 (`PageLimits`)
- [ ] 조회 서비스는 `@Transactional(readOnly = true)`다 — 불필요한 flush · 쓰기 락
- [ ] 없으면 예외인 조회는 `getByXxx`, 없는 게 정상이면 `findByXxx`다 — not-found가 null로 새서 500

## 테스트

- [ ] 부분 성공 API를 상태코드만이 아니라 본문 · DB 건수로 검증한다 — 200인데 저장 0건인 거짓 통과
- [ ] 날짜 경계값은 저장 후 다시 읽어 단언한다 — 메모리 값만 보면 DB에서 바뀐 날짜를 놓친다
- [ ] 구간 경계 테스트에 2일 이상 구간이 있다 — 1일짜리만 있으면 시작일 비교 버그가 통과한다
- [ ] 실패 케이스는 예외 타입과 `ErrorCode`까지 단언한다 — 엉뚱한 예외로 실패해도 통과한다
- [ ] 입력 오류 케이스가 500이 아니라 4xx인지 본다 — 검증 누락이 숨는다
- [ ] 동시성 테스트는 10스레드 이상 `ConcurrencyRunner`로 돌리고 DB에서 다시 센다 — 경합이 안 생겨 거짓 통과 (`ConcurrencyRunner`)
