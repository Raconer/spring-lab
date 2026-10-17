# monitoring

모든 모듈이 같이 쓰는 지표 모니터링(Prometheus, Grafana)과 추적 모니터링(Pinpoint)이다. Gradle 모듈이 아니다. 루트 `CLAUDE.md`의 공통 규칙을 함께 따른다.

## 구조

```
monitoring
├── compose.yaml                     # monitoring 그룹: Prometheus(monitoring.prometheus), Grafana(monitoring.grafana)
├── prometheus
│   └── prometheus.yml               # 수집 대상
├── grafana
│   ├── provisioning
│   │   ├── datasources/prometheus.yaml   # 데이터소스 (uid: prometheus)
│   │   └── dashboards/dashboards.yaml    # dashboards/ 아래 JSON 자동 등록
│   └── dashboards
│       ├── common.json              # 모든 모듈 공통 (HTTP, JVM, 시스템)
│       └── (기술)/(시나리오).json    # 시나리오 대시보드. 하위 폴더가 Grafana 폴더가 된다
└── pinpoint
    ├── compose.yaml                 # pinpoint 그룹: 서버 5개. 호스트 포트는 web(8079)만 연다
    └── agent.yaml                   # 에이전트. 단독으로 띄우지 않고 모듈의 compose.pinpoint.yaml이 extends로 가져간다
```

## Prometheus

- job 이름은 compose 그룹 이름과 같게 한다 (예: `lab-redis`). 대상은 `서비스 이름:포트`로 적는다.
- 대상마다 `role` 라벨을 단다. 앱은 `role: app`에 `__metrics_path__: /actuator/prometheus`, exporter는 `role: exporter`다.
- 떠 있지 않은 그룹의 대상은 Targets 화면에서 DOWN으로 보인다. 정상이다.
- `prometheus.yml`을 고친 뒤에는 `docker compose -f monitoring/compose.yaml restart prometheus`로 반영한다.

## Grafana

- 대시보드 원본은 이 폴더의 JSON이다. Grafana 화면에서 고쳤으면 JSON을 내보내 파일에 반영한다.
- 이름 규칙: `uid`는 `lab-(기술)-(시나리오)`, `title`은 `(기술) / (시나리오): (한글 이름)`이다 (예: `lab-redis-couponfirstcome`, `redis / couponfirstcome: 쿠폰 선착순`).
- 패널의 데이터소스는 `{ "type": "prometheus", "uid": "prometheus" }`로 적는다.
- 시나리오 대시보드에는 시나리오 메트릭, 인프라 지표(exporter), 시나리오 API의 `http_server_requests_seconds_*{uri="..."}`를 같이 둔다. 패널 제목은 한국어로 무엇을 보는지 적는다.
- Micrometer 이름은 Prometheus에서 바뀐다. `Timer` `coupon.issue`는 `coupon_issue_seconds_count`, `_sum`, `_bucket`으로 보인다. p95는 `histogram_quantile(0.95, sum by (le, ...) (rate(..._bucket[...])))`로 구한다.
- 모듈 전체의 HTTP·JVM 지표는 `common.json`에 둔다. 시나리오 대시보드의 HTTP 지표는 시나리오 API `uri`로 거른 것만 둔다.

## Pinpoint

- 3.1.1로 고정한다. 서버 이미지와 에이전트 이미지 버전을 같이 올린다.
- 이미지가 amd64뿐이라 `platform: linux/amd64`로 띄운다. Apple Silicon에서는 에뮬레이션으로 돌아 느리고 메모리를 많이 쓴다.
- 에이전트의 agentId와 applicationName은 모듈 이름으로 한다. 모듈의 `compose.pinpoint.yaml`에서 `JAVA_TOOL_OPTIONS`로 넘긴다.
- 실습이라 모든 요청을 추적한다 (`PROFILER_SAMPLING_COUNTING_SAMPLING_RATE: '1'`).

## 확인

- compose 문법: `docker compose -f monitoring/compose.yaml config -q`, `docker compose -f monitoring/pinpoint/compose.yaml config -q`
- 대시보드 JSON 문법: `jq empty monitoring/grafana/dashboards/(기술)/(시나리오).json`
- 수집 상태: `http://localhost:9090/targets`에서 그룹별 job이 UP인지 본다.
