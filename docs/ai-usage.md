# AI 활용 내역

이 과제는 Claude Code로 진행했다.

- 결정은 사용자가 내렸다
- AI는 문서 정리, 구현, 리뷰, 실측, 기록을 맡았다
- AI 결과는 테스트, 거짓 통과 점검, `.http` 실측, 리뷰로 확인한 뒤 반영했다

---

## 기능별 작업 로그

- [F1 기초 설정](ai-log/F1-setup.md)
  - 스키마, 업체 seed, 에러 응답 형식, 업체 헤더 확인
  - TC-1-01 ~ TC-1-04
  - 경로/메서드 오류와 업체 확인의 순서를 정해 02와 04에 반영
- [F2 입고](ai-log/F2-inbound.md)
  - 상품 생성 `ON CONFLICT DO NOTHING`, 재고 UPSERT
  - reviewer 높음 1건(상품명 길이 단위) 수정
  - 사용자 코드 리뷰 14건 합의와 반영
- [F3 조회](ai-log/F3-query.md)
  - 조회를 Spring Data 두 번 조회로 변경
  - 수량 검사를 재고 엔티티로 이동
  - TC-3-05, TC-3-06 추가
- [F4 출고](ai-log/F4-outbound.md)
  - 조건부 차감 `UPDATE … WHERE quantity >= :quantity`
  - verifier 중간 3건을 TC-4-11과 TC-4-12로 보강
  - 동시성 장치 거짓 통과 점검 6가지

---

## AI를 쓴 작업

- 설계 문서 정리 및 점검
  - 사용자 결정(01 ADR)을 02 ~ 05 설계 문서와 작업 목록으로 정리했다
  - 문서와 코드 사이 어긋남을 찾아 수정안을 냈다
  - 설계 문서 수정은 사용자 승인 후 반영했다
- 기능 구현 (`implementer`)
  - 작업(T) 하나를 커밋 하나로 구현했다
  - 05의 TC 38개를 테스트로 썼다
- 리뷰 (`reviewer`)
  - 기능 브랜치 변경분의 구조를 구현 체크리스트 기준으로 봤다
- 추적성 점검 (`verifier`)
  - 원문, 설계, 코드, 테스트 사이의 누락과 거짓 통과 가능성을 봤다
- 실측 (`/verify-http`)
  - 서버를 띄워 `http/*.http`를 재연하고 대조했다
- 문서화 (`doc-writer`)
  - 기능별 작업 로그, README, 이 문서

---

## 사용한 도구 구성

### 규칙 파일

- [`CLAUDE.md`](../CLAUDE.md)
  - 실행 명령과 완료 기준
  - 묻지 않고 적용할 상시 결정
  - 하지 말 것 (설계 문서 임의 수정, 스펙에 없는 기능 추가, 실행 확인 없는 완료 보고)
- [`src/main/CLAUDE.md`](../src/main/CLAUDE.md)
  - 패키지, 도메인, 트랜잭션, 예외, 주석 규칙
- [`src/test/CLAUDE.md`](../src/test/CLAUDE.md)
  - TC 이름, 테스트 종류, 단언, 동시성 테스트 규칙
- [`.claude/checklists/implementation.md`](../.claude/checklists/implementation.md)
  - 구현 체크리스트
  - `implementer`는 커밋 전에, `reviewer`는 리뷰 기준으로 쓴다
  - 기능마다 새로 겪은 실수를 한 줄씩 추가했다

### 에이전트 (`.claude/agents`)

- `implementer`: 기능 하나를 구현하고 테스트까지 쓴다
- `reviewer`: 기능 구현 뒤 브랜치 변경분의 구조를 리뷰한다
- `verifier`: 원문, 설계, 코드, 테스트 사이의 추적성을 점검한다 (모델 Sonnet)
- `doc-writer`: 설계 문서, 작업 로그, README를 쓴다

### 스킬 (`.claude/skills`)

- `/run-feature F{n}`
  - 기능 하나를 구현 → 리뷰 → 검증 → 실측 → 기록 → 게이트 순서로 실행한다
  - 멈춤 지점에서 사용자에게 결정을 받는다
- `/verify-http`
  - DB를 비우고 서버를 띄워 `.http`를 재연한다
  - `scripts/run_http.py`가 상태 코드, 응답 본문, DB 값을 대조한다
- `/wrap-up`
  - 리뷰 백로그 일괄 처리 → 제출물 작성 → 전체 점검 → 최종 게이트

---

## 결과를 검증한 방법

- 게이트: 작업(T)마다 `./gradlew spotlessApply build`
  - Spotless 포매팅
  - ArchUnit 구조 규칙
  - 전체 테스트 (현재 49개)
- 거짓 통과 점검
  - 운영 코드를 일부러 망가뜨려 해당 TC가 실패하는지 보고 되돌렸다
  - 인터셉터 등록 제거 → TC-1-01, TC-1-02 실패
  - UNIQUE/CHECK 제약 제거 → TC-1-03, TC-1-04 실패
  - 입고 UPSERT를 읽고-계산-쓰기로 바꿈 → TC-2-11, TC-2-12 실패
  - 출고의 `AND quantity >= :quantity` 삭제 → TC-4-03/07/08/09 실패
  - 수량 검사를 상품 조회 뒤로 옮김 → TC-4-11 실패
  - 전체 목록은 각 기능 로그의 "실행 확인"
- `.http` 실측
  - 빈 DB에서 서버를 띄워 요청을 재연했다
  - 상태 코드, 응답 본문, DB 값을 기대값과 대조했다
  - 서버 로그에 ERROR가 새로 찍히지 않았는지 확인했다
- 리뷰 지적 처리
  - 높음: 그 기능 안에서 고치고 수정분만 다시 리뷰했다
  - 중간/낮음: `docs/task_list.md` "리뷰 백로그"에 쌓았다
  - 마무리에서 항목마다 고침(SHA), README 한계, 그대로 둠(이유) 중 하나로 처리했다

---

## AI 결과를 그대로 쓰지 않고 고친 것

검증에서 잡은 문제와 고친 방식이다.

- 상품명 길이를 `@Size(max = 255)`로 검사
  - `@Size`는 UTF-16 단위라 DB 범위 안인 이모지 상품명이 400
  - reviewer가 찾았고 `@CodePointLength`로 바꿨다 (`94a5d20`)
  - 이모지 255자와 256자 테스트를 추가하고 05에 TC-2-15로 등록
- 상품명의 NUL 및 짝 없는 서로게이트
  - NUL은 500, 짝 없는 서로게이트는 다른 문자로 저장돼 다음 입고가 409
  - 요청 검증에서 400 `INVALID_REQUEST`로 막고 05에 TC-2-16으로 등록
- 상품코드 허용 문자 위반 TC 없음
  - verifier가 `@Pattern`을 지워도 테스트가 통과함을 찾았다
  - TC-2-09에 `A 001` 요청 추가
- native 쿼리의 `timestamptz`를 `OffsetDateTime` record로 받음
  - Hibernate 6.6이 `Instant`로 읽어 "argument type mismatch"
  - record의 시각 필드를 `Instant`로 두고 `InventoryState`로 바로 받는다 (`9427b3a`)
- 테스트에서 DB 시각과 응답 시각을 `isEqualTo`로 비교
  - 오프셋이 달라 같은 시각인데 실패
  - `isAtSameInstantAs`로 바꿨다
- 멀티파트 예외 처리를 "쓰지 않는 코드"로 보고 지움
  - multipart 요청이 500이 됐고, MockMvc 테스트로는 드러나지 않았다
  - 제출 전 전체 리뷰가 찾았고 실서버로 재현한 뒤 multipart 해석을 껐다
- 매핑에 produces가 없어 Accept 협상이 커밋 뒤에 일어남
  - `Accept: text/plain`이면 재고는 빠졌는데 406이 나갔다 (F2부터 있던 문제)
  - 제출 전 전체 리뷰가 찾았고 매핑에 produces JSON을 두어 처리 전에 406이 나게 했다
- 출고 요청 검증 및 판정 순서 TC 부족
  - verifier 중간 3건
  - TC-4-11(수량 → 상품 존재 순서), TC-4-12(필수값, 형식, 상한) 추가

---

## 사람이 판단하고 수정한 것

- 사용자 코드 리뷰 14건 합의 ([F2 로그 "사용자 코드 리뷰"](ai-log/F2-inbound.md#사용자-코드-리뷰))
  - 인터셉터와 컨트롤러 패키지 정리
  - `/api/v1` 접두사는 `WebConfig` 한 곳에서 붙인다
  - 입고와 업체 확인을 도메인 서비스로 분리
  - ApplicationService는 조합과 트랜잭션만
  - 재고 변경 결과를 `InventoryState` 하나로
  - 모든 테이블에 `created_at`과 `updated_at`
  - 쓰지 않는 코드 삭제
  - 주석은 기본 없음, 필요한 "왜"만 짧게
  - 합의 내용을 규칙 파일 3개, 체크리스트, 03에 반영
- 조회 방식 변경
  - 03 §13 조회를 JOIN 쿼리에서 Spring Data 두 번 조회로 바꿨다
  - 이유: "직접 쓰는 SQL은 동시성 장치에만" 규칙
- 수량 검사를 재고 엔티티로 이동
  - `Inventory.validateInboundQuantity`, `validateOutboundQuantity`
  - 기준: repository가 필요 없는 규칙은 엔티티에 둔다
  - 도메인 단위 TC-2-14, TC-4-10 추가
- 테스트 데이터 준비 방식
  - 조회/출고 테스트의 given을 입고 API 대신 DB 직접 삽입으로 바꿨다
  - 입고가 깨져도 조회/출고 테스트가 같이 깨지지 않게 하려는 것이다
- TC 추가 및 정리
  - 추가: TC-2-14/15/16, TC-3-05/06, TC-4-10/11/12
  - 추가하지 않음: 출고/조회의 업체 헤더 누락 (TC-1-01과 `.http`가 확인)
  - 추가하지 않음: 상품은 있고 재고 행이 없는 경우 (설계상 생길 수 없음)
  - 삭제: 수량 양 끝 값 테스트 (TC-2-14, TC-2-06과 겹침)
- 출고 트랜잭션 유지
  - 사용자 질문: "왜 트랜잭션 하나인가"
  - `@Transactional`을 빼도 출고 테스트가 통과함을 확인했다
  - 차감 뒤 실패하면 차감까지 되돌리려고 유지하고, 이유를 주석으로 남겼다
- 경로/메서드 오류와 업체 확인의 순서
  - 405는 업체 확인보다 먼저, 없는 경로 404는 업체 확인 뒤
  - 02와 04에 반영했다
- 작업 규칙 조정
  - `verifier` 모델을 Sonnet으로 지정
  - 체크리스트 추가는 사용자 승인 후 반영
  - 리뷰 백로그의 코드/테스트 항목은 항목마다 고칠지 README 한계로 둘지 사용자가 정함
