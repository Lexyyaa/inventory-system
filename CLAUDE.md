# CLAUDE.md

재고 관리 시스템 MVP 과제. Spring Boot 3.5.5 · Java 21 · JPA · PostgreSQL 17 · Testcontainers

## 실행

- 인프라: `docker compose up -d`
  - `inventory-system-postgres`, 호스트 포트 **5433**, `app`/`app`, DB `inventory-system`
- `schema.sql`을 바꿨으면 `docker compose down -v` 후 다시 올린다
  - `CREATE TABLE IF NOT EXISTS`라 기존 테이블은 바뀌지 않는다
- 서버: `./gradlew bootRun` — 프로파일 인자 불필요 (`local`이 기본)
- 전체 테스트: `./gradlew test` — Docker만 켜져 있으면 된다
- 단일 테스트: `./gradlew test --tests '*InboundApiTest'`
- 커밋 전 게이트: `./gradlew spotlessApply build`
- 확인
  - `curl localhost:8080/actuator/health`
  - `http://localhost:8080/swagger-ui.html`
  - `docker exec -it inventory-system-postgres psql -h localhost -U app -d inventory-system`

## 작업 시작 전

1. `docs/task_list.md`에서 **현재** 작업과 커밋 메시지를 확인한다
2. 아래 표에서 읽을 문서를 고르고, 읽은 문서와 계획을 먼저 보고한다 (메인 세션만. implementer는 중간 보고 없이 구현하고 끝에 한 번 보고한다)
3. 스펙에 없는 결정이 필요하면 구현하지 말고 선택지 2~3개로 묻는다

| 작업 | 읽을 문서 (`docs/design/`) |
|---|---|
| F1 기초 설정 | 02 §3 · §8 / 03 §2~§5 · §7 / 04 §2 · §3~§5(오류 응답 문구) · §7 / 05 F1 |
| F2 입고 | 02 §3 · §4 · §7 · §8 / 03 §2~§4 · §6~§10 · §14 · §15 / 04 §2 · §3 · §7 / 05 F2 |
| F3 조회 | 02 §6 · §8 / 03 §6 · §13 / 04 §2 · §5 / 05 F3 |
| F4 출고 | 02 §5 · §7 · §8 / 03 §6 · §11 · §12 · §14 · §15 / 04 §2 · §4 · §7 / 05 F4 |
| F5 마무리 | 00 / 01 / 02 §1 / `task_list` 보류 · 리뷰 백로그 |

## 상시 결정 — 묻지 않고 적용한다

- 요청 본문 입력은 DB 컬럼 범위 안에서만 받는다. 벗어나면 400 (04 §7의 코드)
  - 단 `X-Tenant-Id`는 길이 · 형식과 상관없이 `INVALID_TENANT`, 경로의 상품코드는 검증하지 않고 없으면 404 `PRODUCT_NOT_FOUND` (04 §2 · §5)
- 정책 수치(수량 상한 등)는 `application.yml` + `@ConfigurationProperties`에 둔다
- 요청 본문에 정의되지 않은 필드는 무시한다
- 엔티티는 `BaseTimeEntity`를 상속하지 않고, 서로 식별자(Long)로 참조한다

## 하지 말 것

- `docs/design` 임의 수정 — 변경은 제안만
- 스펙에 없는 기능 · 필드 · API 추가
- 실행 확인 없이 완료 보고
- 스킬 밖에서 사용자가 말하기 전에 커밋 · push · PR · 머지
  - `/run-feature` · `/wrap-up` 안의 작업 커밋은 스킬 실행이 승인이다. push · PR은 스킬의 멈춤 지점에서 묻는다

## 규칙 — 도구가 검증한다

- 포매팅 · 미사용 import: Spotless
- 레이어 의존 방향 · presentation의 domain 참조 금지 · 클래스 위치와 이름: ArchUnit (`architecture/ArchitectureTest`)

## 규칙 — 사람이 본다

- 비즈니스 규칙 · 불변식은 도메인 객체에. Service는 조율만
- DTO에는 변환 메서드(`from`/`of`)만
- 재고 변경과 상품 생성은 03의 원자 SQL로만
- 입력 오류는 4xx. 500은 서버 결함일 때만
- 세부 규칙: 코드는 `src/main/CLAUDE.md`, 테스트는 `src/test/CLAUDE.md`

## 작업 흐름

- 기능(F) 하나 = 브랜치 `feature/{name}`, 작업(T) 하나 = 커밋 하나
- 커밋 메시지는 task_list 작업 줄 그대로 (`타입: 한글 설명`)
- 작업이 끝나면 task_list 체크와 **현재** 갱신을 같은 커밋에 넣는다
- 기능 진행은 `/run-feature F{n}`, 마무리는 `/wrap-up` — 순서와 멈춤 지점은 스킬에 있다

## 완료 기준

- 작업: `./gradlew spotlessApply build` 통과
- 기능: 05의 해당 TC 전부 + 서버를 띄워 `.http` 실측 + task_list 갱신 + ai-log

## 자주 틀리는 것 — 겪을 때마다 추가

- 구현 체크리스트: `.claude/checklists/implementation.md`
