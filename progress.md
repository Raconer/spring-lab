# 진행 상황

마지막 갱신: 2026-10-17 (커밋 `8ec98b1` 기준)

모듈이나 시나리오의 상태가 바뀌면 이 파일을 같이 고친다. 규칙은 `CLAUDE.md`, 사용법은 `README.md`에 있다.

## 요약

- 공통 기반(멀티모듈 빌드, `core`, 공통 `Dockerfile`, 모니터링, 부하 테스트)은 갖춰졌다.
- 기능 모듈은 `lab-redis` 하나다. 시나리오 `base`, `couponfirstcome` 두 개를 구현했다.

## 기능 모듈과 시나리오

| 모듈 | 시나리오 | 코드 | 테스트 | README | 메트릭 | 대시보드 | 부하 스크립트 | 상태 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `lab-redis` | `base` (String, TTL, PING) | 완료 | 완료 | 완료 | 없음 (HTTP 공통 지표) | 공통 대시보드 | 없음 | 완료 |
| `lab-redis` | `couponfirstcome` (`naive`, `lua`, `incr`, `lock`) | 완료 | 완료 (계약·동시성·흐름) | 완료 (시나리오 + 방식 4개) | `coupon.issue` | `redis/couponfirstcome.json` | `redis/couponfirstcome.groovy` | 완료 |

## 공용 스택

| 항목 | 위치 | 상태 |
| --- | --- | --- |
| 공통 모듈 | `core` (actuator, Prometheus 레지스트리, springdoc, 테스트 fixtures) | 완료 |
| 이미지 빌드 | 루트 `Dockerfile` (`MODULE` build arg) | 완료 |
| 지표 모니터링 | `monitoring/compose.yaml` (Prometheus v3.13.4, Grafana 13.2.4) | 완료 |
| 추적 모니터링 | `monitoring/pinpoint/` (Pinpoint 3.1.1) | 완료 |
| 부하 테스트 | `loadtest/` (nGrinder) | 완료 |
| IntelliJ 실행 설정 | `.run/` (`lab-redis`, `lab-redis-all`, `monitoring`, `pinpoint`, `loadtest`) | 완료 |

## 이력

| 날짜 | 커밋 | 내용 |
| --- | --- | --- |
| 2026-10-14 | `6e2848d` | 멀티모듈 골격, `lab-core`, `lab-redis` 연결 테스트 |
| 2026-10-14 | `f8e7797` | `base` 계층 분리(Command/Query), `couponfirstcome` 4개 방식과 테스트 |
| 2026-10-15 | `6adc658`, `931fa03` | 시나리오 README 양식과 mermaid 다이어그램 |
| 2026-10-17 | `8ec98b1` | 모니터링(Prometheus, Grafana, Pinpoint), nGrinder 부하 테스트, 공통 `Dockerfile`, `lab-core` → `core` 이름 변경 |

## 규칙과 실제가 다른 곳

- 이미지 태그: `lab-redis/compose.yaml`의 `redis` 서비스와 `RedisTestContainerConfig.kt`가 `redis:latest`를 쓴다. `.claude/rules/compose.md`는 이미지 버전 고정을 요구한다.
- README 섹션: `couponfirstcome/README.md`에 양식에 없는 `## 지켜야 할 규칙`이 있다.

## 다음 할 일

- [ ] `couponfirstcome` 방식별 부하 테스트 결과(TPS, p95, 초과 발급 수) 기록
- [ ] 다음 시나리오 또는 다음 기능 모듈 정하기
