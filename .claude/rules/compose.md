---
paths:
  - "**/compose*.yaml"
  - "monitoring/pinpoint/agent.yaml"
  - "monitoring/prometheus/prometheus.yml"
  - ".run/*.xml"
  - "Dockerfile"
---

# compose·실행 설정 규칙

## 공용 스택과 모듈이 할 일

| 공용 | 위치 | 모듈이 할 일 |
| --- | --- | --- |
| 이미지 빌드 | 루트 `Dockerfile` | 앱 서비스의 `build`에 `context: ..`, `dockerfile: Dockerfile`, `args: MODULE: lab-(기술)` |
| 메트릭 노출 | `core` (actuator, Prometheus 레지스트리, `lab-core.properties`) | 없음. `/actuator/prometheus`가 자동으로 열린다 |
| 지표 모니터링 | `monitoring/compose.yaml` (Prometheus, Grafana) | `prometheus.yml`에 그룹 이름과 같은 job을 만들고 앱(`role: app`, `__metrics_path__: /actuator/prometheus`)과 exporter(`role: exporter`)를 넣는다. 시나리오 대시보드 추가 |
| 추적 모니터링 | `monitoring/pinpoint/compose.yaml` (Pinpoint 3.1.1), `monitoring/pinpoint/agent.yaml` (에이전트) | `compose.pinpoint.yaml`에서 `agent.yaml`을 `extends`로 가져와 앱에 에이전트 연결 |
| 부하 테스트 | `loadtest/compose.yaml` (nGrinder controller, agent), `loadtest/scripts/` | `loadtest/scripts/(기술)/(시나리오).groovy`에 nGrinder 스크립트 추가. 규칙은 `loadtest/CLAUDE.md` |

## compose 그룹

compose는 그룹(프로젝트)마다 따로 띄운다. 원하는 그룹만 실행할 수 있게 하기 위해서다.

| 그룹 (`name:`) | compose 파일 | 컨테이너 |
| --- | --- | --- |
| `lab-(기술)` | `lab-(기술)/compose.yaml` + `compose.pinpoint.yaml` | 앱, 인프라, exporter, Pinpoint 에이전트 |
| `monitoring` | `monitoring/compose.yaml` | Prometheus, Grafana |
| `pinpoint` | `monitoring/pinpoint/compose.yaml` | Pinpoint 서버 5개 |
| `loadtest` | `loadtest/compose.yaml` | nGrinder controller, agent |

- 모든 compose 파일은 `networks.default.name: spring-boot-lab`으로 같은 네트워크를 쓴다. 그룹끼리는 서비스 이름(예: `lab-redis:8080`)으로 찾는다. 다른 그룹이 먼저 만든 네트워크를 쓸 때 compose가 경고를 띄우지만 정상이다.
- 그룹을 넘어 `depends_on`을 걸 수 없다. 앱이 꼭 필요한 것(Pinpoint 에이전트 파일)은 앱 그룹 안에 둔다.
- 컨테이너 이름은 `container_name: (그룹).(역할)`로 둔다 (예: `redis.app`, `redis.server`, `monitoring.grafana`, `pinpoint.web`). 그룹은 모듈 그룹이면 `lab-`을 뺀 기술 이름, 공용 그룹이면 프로젝트 이름 그대로다.
- 모듈 그룹의 프로필은 `local-test`만 쓴다. 공용 그룹 compose에는 프로필을 두지 않는다.

## IntelliJ 실행 설정

- IntelliJ 실행 설정은 `.run/(그룹).run.xml`에 두고 `folderName="docker"`로 묶는다. 모듈마다 전부 띄우는 `lab-(기술)-all`(예: `lab-redis-all`)은 그 모듈 그룹과 같은 compose 배포 설정에 Before launch(`RunConfigurationTask`)로 공용 그룹 설정을 순서대로 건 것이다. Compound는 그룹을 동시에 실행하다 일부를 빠뜨려서 쓰지 않는다.
- 앱을 빌드하는 그룹은 `commandLineOptions="--build"`를 켠다. 모든 그룹은 `upDetach`, `upForceRecreate`(떠 있던 컨테이너를 내리고 새로 띄움), `upRemoveOrphans`를 켠다.
- 모듈 그룹은 `secondarySourceFiles`에 `compose.pinpoint.yaml`, `profiles`에 `local-test`를 적는다. IntelliJ는 `.env`의 프로필을 적용하지 않는다.

## 포트와 이미지

- 공용 compose에는 호스트 포트가 겹치지 않게 한다. 지금 쓰는 포트: 앱 8080, nGrinder controller 8090, Prometheus 9090, Grafana 3000, Pinpoint web 8079, Redis 6379. 새 포트를 쓰면 이 목록에 추가한다.
- 이미지는 버전을 고정한다 (예: `prom/prometheus:v3.13.4`). 버전을 올리면 공용 compose 한 곳만 고친다.
