---
name: scenario-readme
description: 시나리오·방식 README.md를 새로 쓰거나 고칠 때 쓴다. README 양식, mermaid 규칙, 템플릿 A(물리 구성도)·B(흐름도)·C(시퀀스), 팔레트가 여기 있다. "README 써줘", "흐름도 그려줘", "다이어그램 추가", "README 고쳐" 같은 요청이나, 시나리오 코드의 동작을 바꿔서 README도 고쳐야 할 때 쓴다.
---

# 시나리오 README 작성

시나리오 패키지마다 `README.md`를 둔다 (예: `src/main/kotlin/com/spring/lab/(기술)/(시나리오)/README.md`). 구현 방식이 여러 개면 방식 패키지에도 하나씩 두고, 시나리오 README는 방식 비교와 공통 흐름을 맡는다.

README는 무조건 아래 양식으로 만든다. 섹션 순서, 제목, mermaid 설정(init 줄, 색, 모양)을 바꾸지 않는다. 해당 없는 섹션만 뺀다.

## 작성 순서

1. 대상 코드(controller, service, repository, 방식 구현체, 스크립트), 모듈 `compose.yaml`, 부하 스크립트, 대시보드를 먼저 읽는다. 노드 이름, 명령, 키, 이미지, 포트는 실제 값만 쓴다.
2. 아래 양식을 복사해서 채운다. 다이어그램은 템플릿 A, B, C를 복사하고 init 줄은 그대로 둔다.
3. 다 쓰면 `readme-check` 스킬로 점검한다.

## README 양식

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

## mermaid 공통 규칙

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

## 1. 물리 구성도 (템플릿 A, `flowchart LR`)

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

## 2. 흐름도 (템플릿 B, `flowchart TD`)

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

## 3. 옵션 (템플릿 C, `sequenceDiagram`)

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

## 팔레트

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
