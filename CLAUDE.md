# spring-boot-lab

Spring Boot에서 기술을 하나씩 실습하는 Gradle 멀티모듈 프로젝트다. 모듈 구조, 실행 방법, 새 모듈 추가 절차는 README를 따른다.

@README.md

이 파일에는 모든 모듈에 공통인 규칙만 둔다. 모듈마다 다른 규칙은 그 모듈의 `CLAUDE.md`(예: `lab-redis/CLAUDE.md`)에 두고, 모듈에서 작업할 때는 두 파일을 함께 따른다. 둘이 충돌하면 모듈 `CLAUDE.md`를 따른다.

## 빌드 규칙

- 플러그인 버전은 루트 `build.gradle.kts`에만 선언한다. 모듈은 `kotlin("jvm")`, `id("org.springframework.boot")`처럼 버전 없이 적용한다.
- 의존성 버전은 Spring Boot BOM에 맡긴다. BOM에 없는 라이브러리만 버전을 직접 적고, 이유를 주석으로 남긴다.
- Java toolchain은 Java 25(Temurin)로 고정한다. 모듈에서 따로 바꾸지 않는다.
- Spring Boot 4 기준 starter 이름을 쓴다. 예: `spring-boot-starter-webmvc`, `spring-boot-starter-<기술>-test`.
- Testcontainers는 2.x 아티팩트 이름을 쓴다. 예: `org.testcontainers:testcontainers-junit-jupiter`.

## 모듈 규칙

- 모든 모듈에 공통인 의존성과 설정만 `core`에 둔다. 특정 기술에만 쓰는 것은 그 기능 모듈에 둔다.
- 기능 모듈은 `implementation(project(":core"))`와 `testImplementation(testFixtures(project(":core")))`로 공통 모듈을 가져간다.
- 기능을 실습하는 모듈은 `lab-<기술>`(예: `lab-redis`), 그 외 모듈은 목적 이름(`core`)으로 한다. 패키지는 `com.spring.lab.<기술 또는 목적>`으로 한다.
- 기능 모듈의 애플리케이션 클래스는 `@Import(LabCoreConfiguration::class)`로 공통 설정을 가져온다.
- 인프라는 그 모듈 디렉터리의 `compose.yaml`에 둔다. 앱 컨테이너와 exporter는 `profiles: ['local-test']`로 분리해서 `bootRun`이 같이 띄우지 않게 한다.
- 모듈을 추가하면 `settings.gradle.kts`, README의 모듈 구조, 그 모듈의 `CLAUDE.md`를 같이 만든다. 모듈을 없애면 셋 다 정리한다.
- 모듈 `CLAUDE.md`에는 그 모듈의 시나리오 목록, 계층을 나누는 기준, 기술 전용 규칙(키·토픽 이름, 테스트 도구 등)을 적는다.

## 공용 스택 (이미지, 부하 테스트, 모니터링)

여러 모듈이 같이 쓰는 것은 한 곳에만 두고, 모듈은 가져다 쓰기만 한다. 억지로 공용화하지 않는다. 기술에만 쓰는 것(예: `redis-exporter`)은 그 모듈에 둔다.

| 공용 | 위치 | 모듈이 할 일 |
| --- | --- | --- |
| 이미지 빌드 | 루트 `Dockerfile` | 앱 서비스의 `build`에 `context: ..`, `dockerfile: Dockerfile`, `args: MODULE: lab-(기술)` |
| 메트릭 노출 | `core` (actuator, Prometheus 레지스트리, `lab-core.properties`) | 없음. `/actuator/prometheus`가 자동으로 열린다 |
| 지표 모니터링 | `monitoring/compose.yaml` (Prometheus, Grafana) | `prometheus.yml`에 그룹 이름과 같은 job을 만들고 앱(`role: app`, `__metrics_path__: /actuator/prometheus`)과 exporter(`role: exporter`)를 넣는다. 시나리오 대시보드 추가 |
| 추적 모니터링 | `monitoring/pinpoint/compose.yaml` (Pinpoint 3.1.1), `monitoring/pinpoint/agent.yaml` (에이전트) | `compose.pinpoint.yaml`에서 `agent.yaml`을 `extends`로 가져와 앱에 에이전트 연결 |
| 부하 테스트 | `loadtest/compose.yaml` (nGrinder controller, agent), `loadtest/scripts/` | `loadtest/scripts/(기술)/(시나리오).groovy`에 nGrinder 스크립트 추가. 규칙은 `loadtest/CLAUDE.md` |

- compose는 그룹(프로젝트)마다 따로 띄운다. 원하는 그룹만 실행할 수 있게 하기 위해서다.

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
- IntelliJ 실행 설정은 `.run/(그룹).run.xml`에 두고 `folderName="docker"`로 묶는다. 모듈마다 전부 띄우는 `lab-(기술)-all`(예: `lab-redis-all`)은 그 모듈 그룹과 같은 compose 배포 설정에 Before launch(`RunConfigurationTask`)로 공용 그룹 설정을 순서대로 건 것이다. Compound는 그룹을 동시에 실행하다 일부를 빠뜨려서 쓰지 않는다.
  - 앱을 빌드하는 그룹은 `commandLineOptions="--build"`를 켠다. 모든 그룹은 `upDetach`, `upForceRecreate`(떠 있던 컨테이너를 내리고 새로 띄움), `upRemoveOrphans`를 켠다.
  - 모듈 그룹은 `secondarySourceFiles`에 `compose.pinpoint.yaml`, `profiles`에 `local-test`를 적는다. IntelliJ는 `.env`의 프로필을 적용하지 않는다.
- 공용 compose에는 호스트 포트가 겹치지 않게 한다. 지금 쓰는 포트: 앱 8080, nGrinder controller 8090, Prometheus 9090, Grafana 3000, Pinpoint web 8079, Redis 6379.
- 이미지는 버전을 고정한다 (예: `prom/prometheus:v3.13.4`). 버전을 올리면 공용 compose 한 곳만 고친다.
- 시나리오 메트릭은 그 시나리오의 핵심 동작 하나에 Micrometer `Timer` 하나를 둔다. 이름은 `(시나리오 대상).(동작)`(예: `coupon.issue`), 태그는 비교 축(예: `method`, `result`)으로 하고 `publishPercentileHistogram()`을 켠다. 메트릭 기록은 service에서 한다.
- 대시보드는 `monitoring/grafana/dashboards/(기술)/(시나리오).json`에 두고, 시나리오 메트릭·인프라 지표·시나리오 API 요청 지표(`http_server_requests_seconds_*{uri="(시나리오 API 경로)"}`)를 한 화면에 둔다. nGrinder는 Prometheus로 지표를 내보내지 않으므로 부하 쪽 결과는 nGrinder 화면에서 본다.

## 패키지 구조

기능 모듈은 먼저 목적(시나리오)별로 패키지를 나누고, 그 안을 MVC 계층으로 나눈다. 계층 패키지는 필요한 것만 만든다.

```
com.spring.lab.<기술>
├── Lab<기술>Application   # 애플리케이션 클래스는 루트 패키지에 둔다
├── config                 # 모듈 전체가 쓰는 @Configuration (필요할 때만)
├── base                   # 모듈을 만들 때 항상 만든다. 그 기술의 기본 명령 실습
│   ├── controller         # @RestController. 요청을 받아 service를 호출한다
│   ├── service            # @Service. 비즈니스 로직, 트랜잭션 경계
│   ├── repository         # 저장소·외부 기술 접근 (MQ 모듈이면 producer/consumer 등 모듈 CLAUDE.md에서 정한다)
│   ├── domain             # 엔티티, 저장 모델, 결과 enum 같은 도메인 타입
│   ├── dto                # 요청/응답 객체
│   └── config             # 이 시나리오 전용 @Configuration
└── <시나리오>              # 실전 상황 하나당 패키지 하나
    └── (base와 같은 계층 구조)
```

- 시나리오는 목적 하나만 다룬다. 다른 목적이 섞이면 시나리오 패키지를 새로 만든다.
- 시나리오끼리 서로의 클래스를 가져다 쓰지 않는다. 같은 코드가 필요하면 각자 둔다. 모듈 전체에 필요한 설정만 루트 `config`에 둔다.
- 계층 안은 역할로 나눈다 (예: 저장소 모듈은 쓰기/읽기, MQ 모듈은 발행/구독). 구체적인 기준은 모듈 `CLAUDE.md`에 정한다.
- controller는 저장소나 외부 기술 API를 직접 쓰지 않고 service를 거친다.
- controller는 `domain` 객체를 그대로 반환하지 않고 `dto`로 바꿔서 반환한다.
- 실패 이유는 `ProblemDetail`(RFC 9457) 본문으로 알려준다.
- URL은 `/<기술>/...`로 시작한다. `base`는 `/<기술>/<리소스>`, 시나리오는 `/<기술>/<시나리오 복수형>/...`로 한다.
- 리소스(스크립트, 스키마 등)는 `src/main/resources/<기술>/<시나리오>/`에 둔다.

### 구현 방식이 여러 개인 시나리오

한 시나리오를 여러 방식으로 구현해 비교할 때는 한 단계 더 나눈다. 방식마다 다른 부분만 방식 패키지에 두고, 나머지는 `common`에 한 번만 둔다.

```
com.spring.lab.<기술>.<시나리오>
├── common        # 모든 방식이 같이 쓰는 API·service·조회·dto·domain, 방식 인터페이스
├── <방식1>        # 그 방식의 구현체와 전용 config만
└── <방식2>
```

- 방식 패키지 이름은 방식을 한 단어로 적는다. 같은 값을 `common/domain`의 방식 enum과 API 파라미터(`?method=<방식>`)에 쓴다.
- 방식 구현체는 `common`의 인터페이스를 구현한 빈으로 두고, `common`의 service가 enum으로 골라 쓴다.
- 방식 패키지끼리는 서로 가져다 쓰지 않는다. `common`만 의존한다.
- 방식 전용 설정과 리소스는 그 방식 패키지의 `config`, `src/main/resources/<기술>/<시나리오>/<방식>/`에 둔다.
- 올바르지 않은 방식도 비교 기준선으로 둘 수 있다. 이름과 주석에 기준선임을 적는다.

## 시나리오 문서 (README.md)

시나리오 패키지마다 `README.md`를 둔다 (예: `src/main/kotlin/com/spring/lab/(기술)/(시나리오)/README.md`). 구현 방식이 여러 개면 방식 패키지에도 하나씩 두고, 시나리오 README는 방식 비교와 공통 흐름을 맡는다.

README는 무조건 아래 양식으로 만든다. 섹션 순서, 제목, mermaid 설정(init 줄, 색, 모양)을 바꾸지 않는다. 해당 없는 섹션만 뺀다.

### README 양식

````markdown
# (시나리오 또는 방식) — (한 줄 제목)

(한 줄 요약: 무엇을 어떻게 하는지)

## 사용 기술

| 기술·명령 | 역할 |
| --- | --- |
| (예: `EVALSHA`) | (예: 확인과 발급을 원자적으로 실행) |

## 데이터 구조

| 키·토픽·큐 | 타입 | 값 |
| --- | --- | --- |
| (예: `coupon:{couponId}:quantity`) | (예: String) | (예: 총 수량) |

## 1. 물리 구성도

(템플릿 A)

## 2. 흐름도

노드는 실행되는 서버별로 묶었다. Client(회색), (앱) 컨테이너(초록), (인프라) 컨테이너((팔레트 색)).

(템플릿 B. API가 여러 개면 API마다 `### (API 이름)` 제목 아래 하나씩)

## 3. 옵션: (무엇을 보여주는지)

(왜 1, 2로는 부족한지 한 줄)

(템플릿 C)

## 부하 테스트

nGrinder 스크립트 `loadtest/scripts/(기술)/(시나리오).groovy`로 부하를 준다.

| 항목 | 값 |
| --- | --- |
| Parameter (`key.value`를 쉼표로) | (key, 고를 수 있는 값과 기본값. 예: `method`: `naive`, `lua` (기본 `naive`)) |
| 실행 조건 | (agent 수, 프로세스 수, 가상 사용자 수, 실행 횟수) |
| 검증 | (부하 후 확인하는 규칙. `@AfterProcess` 로그로 남김) |

1. `http://localhost:8090`(nGrinder, `admin` / `admin`)의 `Script`에 스크립트를 올린다.
2. `Performance Test` → `Create Test`에서 스크립트를 고르고, 실행 조건과 `상세 설정 보기`의 Parameter를 넣고 시작한다.

## 모니터링

| 도구 | 보는 곳 | 볼 것 |
| --- | --- | --- |
| Grafana | `(기술) / (시나리오)` 대시보드 | (시나리오 메트릭 이름과 의미) |
| Pinpoint | (application 이름) | (추적에서 보이는 구간) |

## 한계와 주의할 점

- (한계)

## 검증하는 테스트

| 테스트 | 확인하는 것 |
| --- | --- |
| (테스트 클래스) | (확인하는 것) |
````

### mermaid 공통 규칙

- 다이어그램은 아래 템플릿 A, B, C를 복사해서 만든다. 첫 줄 init은 템플릿 그대로 둔다.
- 기본 배경(다이어그램 바탕, 기본 노드)은 흰색이다. IDE가 다크 테마여도 흰 바탕으로 보이게 하기 위해서다.
- 색은 배경(`fill`)만 파스텔톤이다. 선(테두리, 화살표)은 배경과 대비되는 진한 색(`stroke-width:2px`)이다.
- 텍스트는 전부 검정(`#000000`) 굵게다. 노드 글자, 구역 제목, 화살표 라벨, 시퀀스 글자 모두 해당한다. `classDef`의 `color`만으로는 렌더러에 따라 적용되지 않으므로 init의 `themeCSS`로 강제한다.
- 화살표 위 라벨은 배경 없이 글자만 보인다.
- flowchart 선은 무조건 꺾인선(`curve: step`)이다.
- 노드 이름은 실제 클래스 이름과 인프라 이름으로 적는다.
- 라벨 안에 꺾쇠(`<값>`)를 쓰지 않는다. mermaid가 HTML 태그로 보고 지울 수 있다. `(값)`으로 쓴다.
- 특수문자(`{ } ( ) :` 등)가 들어간 라벨은 큰따옴표로 감싼다.
- 라이브러리 내부 동작(예: Redisson 락 스크립트)을 그릴 때는 무엇을 기준으로 요약했는지 본문에 적는다.

### 1. 물리 구성도 (템플릿 A, `flowchart LR`)

- 서버의 물리 정보만 그린다. 그 외에는 전부 작성 금지다.
- 허용: 서버 노드(Client, 로드밸런서, 앱 서버, Redis, DB, 브로커 등), 서버 이름, Docker 이미지, 포트, Docker 호스트 경계, 서버 사이 연결의 프로토콜·주소(예: `HTTP :8080`, `TCP redis:6379`)
- 금지: 서버 내부(스레드, 클래스, 패키지, 리소스, 커넥션 풀, 라이브러리 이름), 데이터(키, 토픽, 큐, 테이블, 채널), API 경로, 명령, 왕복 횟수, 요청 예시, 설명 문구, Docker 호스트 경계 외의 `subgraph`. 이런 내용은 2번 흐름도나 본문에 적는다.
- 실제 실행 구성대로 그린다. 지금은 모듈의 `compose.yaml`(`--profile local-test`)로 앱과 인프라가 모두 Docker 컨테이너로 뜬다. 컨테이너 노드에는 서비스 이름, 이미지, 포트를 적고, 컨테이너끼리 연결은 compose 서비스 이름으로 적는다.
- 동시성이나 확장이 핵심이면 클라이언트나 앱 서버를 두 개 이상 그린다. 확장 시 추가되는 서버는 `springScale`(점선)로 그린다.
- 서버는 팔레트의 모양과 `classDef`로 그린다.

```
%%{init: {"flowchart": {"curve": "step"}, "themeVariables": {"background": "#FFFFFF", "mainBkg": "#FFFFFF", "edgeLabelBackground": "transparent", "lineColor": "#212121", "primaryTextColor": "#000000", "secondaryTextColor": "#000000", "tertiaryTextColor": "#000000", "textColor": "#000000", "nodeTextColor": "#000000", "titleColor": "#000000", "actorTextColor": "#000000", "signalTextColor": "#000000", "noteTextColor": "#000000", "labelTextColor": "#000000", "loopTextColor": "#000000"}, "themeCSS": "& { background-color: #FFFFFF !important; } .edgeLabel, .edgeLabel p, .edgeLabel span, .labelBkg { background-color: transparent !important; } .edgeLabel rect { fill: transparent !important; opacity: 0 !important; } text, tspan { fill: #000000 !important; font-weight: bold !important; } .nodeLabel, .edgeLabel, .cluster-label, .label, span, p { color: #000000 !important; font-weight: bold !important; }"}}%%
flowchart LR
    Client(["Client"])
    subgraph docker["Docker 호스트 (docker compose)"]
        direction LR
        App("lab-(기술) 컨테이너<br/>eclipse-temurin:25-jre :8080")
        Infra[("(서비스) 컨테이너<br/>(이미지) :(포트)")]
    end

    Client -->|"HTTP :8080"| App
    App -->|"TCP (서비스):(포트)"| Infra

    classDef client fill:#ECEFF1,stroke:#263238,stroke-width:2px,color:#000000,font-weight:bold
    classDef spring fill:#D8EFC8,stroke:#1E3D0C,stroke-width:2px,color:#000000,font-weight:bold
    classDef redis fill:#F9D2CF,stroke:#5C120D,stroke-width:2px,color:#000000,font-weight:bold
    class Client client
    class App spring
    class Infra redis
    style docker fill:#E3F2FD,stroke:#0B3D6B,stroke-width:2px,color:#000000,font-weight:bold
```

### 2. 흐름도 (템플릿 B, `flowchart TD`)

- 요청 하나가 들어와 응답이 나갈 때까지의 절차를 그린다.
- 노드는 실행되는 물리 서버별 `subgraph`로 묶는다. 서버 구분은 1번과 같게 한다.
  - Client: 요청 시작
  - 앱 컨테이너: 판단, 변환, 응답 생성
  - 인프라 컨테이너: 실제 명령 실행, 스크립트 내부
- 시작·끝은 `([ ])`, 처리는 `[ ]`, 분기는 `{ }`로 그린다. 처리 노드에는 실제 호출과 실제 명령, 실제 키 이름을 적는다 (예: `SADD coupon:{welcome}:issued-users user-1`).
- 원자적으로 실행되는 구간이나 락 안 구간은 그 서버 `subgraph` 안에 점선 `subgraph`로 한 번 더 묶고 제목에 이유를 적는다.
- 서버 `subgraph`는 팔레트와 같은 파스텔 색, 노드는 `classDef default`(흰 바탕, 진한 선)로 그린다.
- 인프라 `subgraph`의 id는 서비스 이름으로 한다 (예: `redis`).

```
%%{init: {"flowchart": {"curve": "step"}, "themeVariables": {"background": "#FFFFFF", "mainBkg": "#FFFFFF", "edgeLabelBackground": "transparent", "lineColor": "#212121", "primaryTextColor": "#000000", "secondaryTextColor": "#000000", "tertiaryTextColor": "#000000", "textColor": "#000000", "nodeTextColor": "#000000", "titleColor": "#000000", "actorTextColor": "#000000", "signalTextColor": "#000000", "noteTextColor": "#000000", "labelTextColor": "#000000", "loopTextColor": "#000000"}, "themeCSS": "& { background-color: #FFFFFF !important; } .edgeLabel, .edgeLabel p, .edgeLabel span, .labelBkg { background-color: transparent !important; } .edgeLabel rect { fill: transparent !important; opacity: 0 !important; } text, tspan { fill: #000000 !important; font-weight: bold !important; } .nodeLabel, .edgeLabel, .cluster-label, .label, span, p { color: #000000 !important; font-weight: bold !important; }"}}%%
flowchart TD
    subgraph client["Client"]
        Start(["(메서드) (경로)<br/>(요청 값)"])
    end

    subgraph app["lab-(기술) 컨테이너"]
        Handle["(Controller)<br/>→ (Service)"]
        Decide{"(판단)"}
        Ok(["(성공 응답)"])
        Fail(["(실패 응답)"])
    end

    subgraph infra["(서비스) 컨테이너"]
        Cmd["(실제 명령과 키)"]
        subgraph atomic["(원자·락 구간인 이유)"]
            Inner["(구간 안에서 실행되는 명령)"]
        end
    end

    Start --> Handle --> Cmd --> Inner --> Decide
    Decide -->|예| Ok
    Decide -->|아니오| Fail

    classDef default fill:#FFFFFF,stroke:#212121,stroke-width:2px,color:#000000,font-weight:bold
    style client fill:#ECEFF1,stroke:#263238,stroke-width:2px,color:#000000,font-weight:bold
    style app fill:#D8EFC8,stroke:#1E3D0C,stroke-width:2px,color:#000000,font-weight:bold
    style infra fill:#F9D2CF,stroke:#5C120D,stroke-width:2px,color:#000000,font-weight:bold
    style atomic fill:#FFFFFF,stroke:#5C120D,stroke-dasharray:5 5,stroke-width:2px,color:#000000,font-weight:bold
```

### 3. 옵션 (템플릿 C, `sequenceDiagram`)

- 1, 2로 보기 힘들 때만 추가한다. 예: 요청 두 개 이상이 겹치는 시간 순서(경쟁 상태, 락 대기)
- 제목 아래에 왜 1, 2로는 부족한지 한 줄 적는다.
- 도형(참여자 박스, Note, `par`·`alt`·`loop` 라벨 박스, 활성 막대, `autonumber` 번호 동그라미)은 배경 없이 진한 테두리만 그린다. 메시지 흐름선과 화살촉은 검정이다. `rect` 색 구간은 쓰지 않는다.

```
%%{init: {"themeVariables": {"background": "#FFFFFF", "mainBkg": "#FFFFFF", "edgeLabelBackground": "transparent", "actorBkg": "transparent", "signalColor": "#000000", "sequenceNumberColor": "#000000", "actorBorder": "#212121", "noteBkgColor": "transparent", "noteBorderColor": "#212121", "labelBoxBkgColor": "transparent", "labelBoxBorderColor": "#212121", "activationBkgColor": "transparent", "activationBorderColor": "#212121", "lineColor": "#212121", "primaryTextColor": "#000000", "secondaryTextColor": "#000000", "tertiaryTextColor": "#000000", "textColor": "#000000", "nodeTextColor": "#000000", "titleColor": "#000000", "actorTextColor": "#000000", "signalTextColor": "#000000", "noteTextColor": "#000000", "labelTextColor": "#000000", "loopTextColor": "#000000"}, "themeCSS": "& { background-color: #FFFFFF !important; } .edgeLabel, .edgeLabel p, .edgeLabel span, .labelBkg { background-color: transparent !important; } .edgeLabel rect { fill: transparent !important; opacity: 0 !important; } rect.actor, rect.note, .labelBox, rect[class^=activation] { fill: transparent !important; stroke: #212121 !important; } .messageLine0, .messageLine1 { stroke: #000000 !important; } marker circle { fill: transparent !important; stroke: #000000 !important; } marker path, .arrowheadPath { fill: #000000 !important; stroke: #000000 !important; } text, tspan { fill: #000000 !important; font-weight: bold !important; } .nodeLabel, .edgeLabel, .cluster-label, .label, span, p { color: #000000 !important; font-weight: bold !important; }"}}%%
sequenceDiagram
    autonumber
    participant A as 요청 A (user-10)
    participant I as (인프라)
    participant B as 요청 B (user-11)

    Note over I: (시작 상태)
    par 거의 동시에 전송
        A->>I: (명령)
    and
        B->>I: (명령)
    end
    I-->>A: (결과)
    I-->>B: (결과)
```

### 팔레트

배경(`fill`)은 브랜드 색을 옅게 한 파스텔, 선(`stroke`)은 브랜드 색 계열의 진한 색, 글자는 검정이다. 팔레트에 없는 서버를 쓰면 같은 방식으로 `classDef`를 만들고 이 표에 추가한다. 저장소 서버는 원통, 메시지 브로커는 평행사변형, 모니터링 도구는 이중 사각형으로 그린다.

| 대상 | 모양 | classDef / style |
| --- | --- | --- |
| Docker 호스트 경계 | `subgraph docker[...]` | `style docker fill:#E3F2FD,stroke:#0B3D6B,stroke-width:2px,color:#000000,font-weight:bold` |
| Client | `(["..."])` 스타디움 | `classDef client fill:#ECEFF1,stroke:#263238,stroke-width:2px,color:#000000,font-weight:bold` |
| 로드밸런서 | `{{"..."}}` 육각형 | `classDef lb fill:#E5D9FF,stroke:#2A1466,stroke-width:2px,color:#000000,font-weight:bold` |
| Spring Boot 앱 서버 | `("...")` 둥근 사각형 | `classDef spring fill:#D8EFC8,stroke:#1E3D0C,stroke-width:2px,color:#000000,font-weight:bold` |
| 확장 시 추가되는 앱 서버 | `("...")` 둥근 사각형, 점선 | `classDef springScale fill:#F0F8EA,stroke:#1E3D0C,stroke-dasharray:5 5,stroke-width:2px,color:#000000,font-weight:bold` |
| Redis 서버 | `[("...")]` 원통 | `classDef redis fill:#F9D2CF,stroke:#5C120D,stroke-width:2px,color:#000000,font-weight:bold` |
| MySQL 서버 | `[("...")]` 원통 | `classDef mysql fill:#D6E4F0,stroke:#14304A,stroke-width:2px,color:#000000,font-weight:bold` |
| PostgreSQL 서버 | `[("...")]` 원통 | `classDef postgres fill:#D3E0EC,stroke:#11273D,stroke-width:2px,color:#000000,font-weight:bold` |
| MongoDB 서버 | `[("...")]` 원통 | `classDef mongo fill:#D5EDD5,stroke:#143A15,stroke-width:2px,color:#000000,font-weight:bold` |
| Kafka 브로커 | `[/"..."/]` 평행사변형 | `classDef kafka fill:#DADADA,stroke:#000000,stroke-width:2px,color:#000000,font-weight:bold` |
| RabbitMQ 브로커 | `[/"..."/]` 평행사변형 | `classDef rabbitmq fill:#FFE0CC,stroke:#6B2B00,stroke-width:2px,color:#000000,font-weight:bold` |
| Prometheus, exporter | `[["..."]]` 이중 사각형 | `classDef prometheus fill:#FBD9CF,stroke:#8A2A0F,stroke-width:2px,color:#000000,font-weight:bold` |
| Grafana | `[["..."]]` 이중 사각형 | `classDef grafana fill:#FDE3CC,stroke:#8A3A00,stroke-width:2px,color:#000000,font-weight:bold` |
| Pinpoint (collector, web, agent) | `[["..."]]` 이중 사각형 | `classDef pinpoint fill:#D9E8FB,stroke:#0D3B73,stroke-width:2px,color:#000000,font-weight:bold` |
| 흐름도 노드 (공통) | `[ ]`, `{ }`, `([ ])` | `classDef default fill:#FFFFFF,stroke:#212121,stroke-width:2px,color:#000000,font-weight:bold` |
| 흐름도 원자·락 구간 | 점선 `subgraph` | `style (id) fill:#FFFFFF,stroke:(그 서버의 선 색),stroke-dasharray:5 5,stroke-width:2px,color:#000000,font-weight:bold` |

코드의 동작을 바꾸면 같은 커밋에서 README도 고친다.

## 테스트

테스트는 main과 같은 패키지 구조를 따른다. 시나리오마다, 계층마다, 역할마다 클래스를 나누고, 대상 클래스 하나에 테스트 클래스 하나를 둔다. 전체 흐름 테스트는 시나리오 패키지 바로 아래에 `<시나리오>FlowTest`로 하나 둔다.

| 대상 | 방식 | 인프라 | 검증할 것 |
| --- | --- | --- | --- |
| 인프라 접근 계층 (repository 등) | 기술별 슬라이스 테스트 + `@Import(컨테이너 설정, 대상 클래스)` | Testcontainers | 실제 인프라에 대한 동작 |
| service | 순수 단위 테스트. 의존 대상은 Mockito `mock()` | 없음 | 변환·분기 로직 |
| controller | `@WebMvcTest(대상::class)` + service는 `@MockitoBean` | 없음 | 요청 매핑, JSON 변환, 상태 코드 |
| 전체 흐름 | `@SpringBootTest` + `@AutoConfigureMockMvc` | Testcontainers | 시나리오의 API 흐름이 실제 인프라로 끝까지 이어지는지 |

- 인프라 접근 테스트는 데이터 준비와 결과 확인을 대상 클래스가 아니라 기술의 기본 API(템플릿, 클라이언트)로 직접 한다. 대상 클래스끼리 서로를 검증 도구로 쓰지 않게 하기 위해서다.
- Testcontainers 테스트는 `@BeforeEach`에서 데이터를 비운다.
- 컨테이너는 모듈의 `<기술>TestContainerConfig`(`@TestConfiguration` + `@Bean @ServiceConnection`)에 한 번만 정의하고 `@Import`로 쓴다.
- 컨테이너 이미지는 그 모듈의 `compose.yaml`과 같은 이미지를 쓴다. 로컬에 띄운 compose 컨테이너에 의존하지 않는다.
- 동시성이 핵심인 시나리오(선착순, 재고 차감 등)는 동시 요청 테스트를 둔다. 가상 스레드 executor와 `CountDownLatch`로 요청을 한 번에 출발시키고, 결과 수와 인프라 상태를 함께 확인한다.
- 구현 방식이 여러 개면 모든 방식이 지켜야 할 규칙을 `common` 테스트 패키지에 추상 계약 테스트로 두고, 방식별 테스트가 상속한다. 방식마다 다른 점만 방식별 테스트에 추가한다. 흐름 테스트는 `@ParameterizedTest`로 모든 방식을 같은 시나리오로 돌린다.
- 테스트 이름은 한국어 백틱 함수명으로 동작을 적는다. 예: `` `없는 키를 지우면 false를 돌려준다` ``
- 테스트 실행: `./gradlew :<모듈>:test` (Docker 필요). 일부만: `--tests '*.<시나리오>.*'`, `--tests '*.service.*'`
