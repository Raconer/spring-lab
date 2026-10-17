---
name: new-scenario
description: 기능 모듈(lab-*)에 실습 시나리오를 새로 추가할 때 쓴다. 코드·테스트·README·메트릭·대시보드·부하 스크립트·문서 표까지 빠짐없이 만든다. "시나리오 추가", "새 시나리오 만들어", "(기술)로 (상황) 실습 추가" 같은 요청에 쓴다.
---

# 시나리오 추가

규칙은 루트 `CLAUDE.md`와 대상 모듈 `CLAUDE.md`에 있다. 이 스킬은 순서와 빠뜨리기 쉬운 파일만 정한다. 기준 예시는 `lab-redis`의 `couponfirstcome`이다.

## 0. 먼저 정할 것

아래가 요청에 없으면 한 번에 묻는다. 추측으로 채우지 않는다.

- 모듈과 시나리오 패키지 이름 (예: `lab-redis`, `ranking`)
- 실습할 상황과 지켜야 할 규칙 (예: "수량을 넘겨 발급하지 않는다")
- 구현 방식이 여러 개인지. 여러 개면 방식 이름 (`?method=` 값과 같다)과 기준선 방식이 있는지
- 동시성이 핵심인지 (동시 요청 테스트 여부)

## 1. 코드

- `src/main/kotlin/com/spring/lab/(기술)/(시나리오)/`에 계층 패키지를 만든다. 방식이 여러 개면 `common` + 방식 패키지로 나눈다.
- 키·토픽 이름 규칙 객체, 리소스(`src/main/resources/(기술)/(시나리오)/`)를 모듈 `CLAUDE.md` 규칙대로 둔다.
- 핵심 동작 하나에 Micrometer `Timer`를 service에 둔다 (`(대상).(동작)`, `publishPercentileHistogram()`).
- URL은 `/(기술)/(시나리오 복수형)/...`, 실패는 `ProblemDetail`.

## 2. 테스트

대상 클래스 하나에 테스트 클래스 하나. 같은 패키지 구조로 둔다.

- repository·방식 구현체: 슬라이스 테스트 + `(기술)TestContainerConfig`
- service: Mockito 단위 테스트 + 메트릭 기록 확인
- controller: `@WebMvcTest`
- 흐름: `(시나리오)FlowTest`. 방식이 여러 개면 `@ParameterizedTest`
- 방식이 여러 개면 `common`에 추상 계약 테스트, 동시성 핵심이면 동시 요청 계약 테스트

## 3. 문서

- 시나리오 `README.md` (방식이 여러 개면 방식 패키지에도). `scenario-readme` 스킬의 양식과 mermaid 템플릿 A, B, C를 그대로 복사해서 채운다.
- 모듈 `CLAUDE.md`의 시나리오 표, 부하 테스트·모니터링 표
- 루트 `README.md`의 기능 모듈 패키지 구조와 API 표

## 4. 부하 테스트와 모니터링

- `loadtest/scripts/(기술)/(시나리오).groovy`: `loadtest/CLAUDE.md` 규칙대로. 기준 예시는 `loadtest/scripts/redis/couponfirstcome.groovy`
- `monitoring/grafana/dashboards/(기술)/(시나리오).json`: `monitoring/CLAUDE.md` 규칙대로. 기준 예시는 `redis/couponfirstcome.json`
- 새 인프라·exporter가 생기면 모듈 `compose.yaml`과 `monitoring/prometheus/prometheus.yml`에 추가

## 5. 확인

1. `./gradlew :(모듈):test --tests '*.(시나리오).*'` (Docker 필요. 못 돌리면 `--tests '*.(시나리오).*.service.*' --tests '*.(시나리오).*.controller.*'`만 돌리고 그렇다고 알린다)
2. `jq empty monitoring/grafana/dashboards/(기술)/(시나리오).json`
3. `readme-check` 스킬로 새 README 점검
4. `progress.md`의 시나리오 표에 행을 추가하고 이력·다음 할 일을 고친다

끝나면 만든 파일 목록과 확인 결과(통과·실패·못 돌린 것)를 보고한다. 커밋은 하지 않는다.
