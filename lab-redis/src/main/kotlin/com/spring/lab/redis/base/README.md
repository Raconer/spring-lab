# base — Redis 기본 명령

Redis String 값의 저장·조회·삭제와 TTL, 연결 확인(PING)을 실습한다.

## 사용 기술

| 기술 | 쓰는 곳 |
| --- | --- |
| Redis String (`SET`, `GET`, `DEL`) | 값 저장·조회·삭제 |
| Redis 만료 (`SET ... EX`, `TTL`) | 만료 시간 설정과 남은 시간 조회 |
| `PING` | 연결 확인 |
| Spring Data Redis `StringRedisTemplate` + Lettuce | 애플리케이션에서 Redis 명령 실행 |

## 키 구조

| 키 | 타입 | 값 |
| --- | --- | --- |
| 요청 경로의 `{key}` 그대로 | String | 요청 본문의 `value` |

## 1. 물리 구성도

```mermaid
%%{init: {"flowchart": {"curve": "step"}, "themeVariables": {"background": "#FFFFFF", "mainBkg": "#FFFFFF", "edgeLabelBackground": "transparent", "lineColor": "#212121", "primaryTextColor": "#000000", "secondaryTextColor": "#000000", "tertiaryTextColor": "#000000", "textColor": "#000000", "nodeTextColor": "#000000", "titleColor": "#000000", "actorTextColor": "#000000", "signalTextColor": "#000000", "noteTextColor": "#000000", "labelTextColor": "#000000", "loopTextColor": "#000000"}, "themeCSS": "& { background-color: #FFFFFF !important; } .edgeLabel, .edgeLabel p, .edgeLabel span, .labelBkg { background-color: transparent !important; } .edgeLabel rect { fill: transparent !important; opacity: 0 !important; } text, tspan { fill: #000000 !important; font-weight: bold !important; } .nodeLabel, .edgeLabel, .cluster-label, .label, span, p { color: #000000 !important; font-weight: bold !important; }"}}%%
flowchart LR
    Client(["Client"])
    subgraph docker["Docker 호스트 (docker compose)"]
        direction LR
        App("lab-redis 컨테이너<br/>eclipse-temurin:25-jre :8080")
        Redis[("redis 컨테이너<br/>redis:latest :6379")]
    end

    Client -->|"HTTP :8080"| App
    App -->|"TCP redis:6379"| Redis

    classDef client fill:#ECEFF1,stroke:#263238,stroke-width:2px,color:#000000,font-weight:bold
    classDef spring fill:#D8EFC8,stroke:#1E3D0C,stroke-width:2px,color:#000000,font-weight:bold
    classDef redis fill:#F9D2CF,stroke:#5C120D,stroke-width:2px,color:#000000,font-weight:bold
    class Client client
    class App spring
    class Redis redis
    style docker fill:#E3F2FD,stroke:#0B3D6B,stroke-width:2px,color:#000000,font-weight:bold
```

## 2. 흐름도

노드는 실행되는 서버별로 묶었다. Client(회색), lab-redis 컨테이너(초록), redis 컨테이너(빨강).

### 값 저장

```mermaid
%%{init: {"flowchart": {"curve": "step"}, "themeVariables": {"background": "#FFFFFF", "mainBkg": "#FFFFFF", "edgeLabelBackground": "transparent", "lineColor": "#212121", "primaryTextColor": "#000000", "secondaryTextColor": "#000000", "tertiaryTextColor": "#000000", "textColor": "#000000", "nodeTextColor": "#000000", "titleColor": "#000000", "actorTextColor": "#000000", "signalTextColor": "#000000", "noteTextColor": "#000000", "labelTextColor": "#000000", "loopTextColor": "#000000"}, "themeCSS": "& { background-color: #FFFFFF !important; } .edgeLabel, .edgeLabel p, .edgeLabel span, .labelBkg { background-color: transparent !important; } .edgeLabel rect { fill: transparent !important; opacity: 0 !important; } text, tspan { fill: #000000 !important; font-weight: bold !important; } .nodeLabel, .edgeLabel, .cluster-label, .label, span, p { color: #000000 !important; font-weight: bold !important; }"}}%%
flowchart TD
    subgraph client["Client"]
        S1(["PUT /redis/values/greeting<br/>value = hello, ttlSeconds = 60"])
    end
    subgraph app["lab-redis 컨테이너"]
        S2["RedisCommandController<br/>→ RedisCommandService.save"]
        S3{"ttlSeconds가 있는가"}
        S6(["204 No Content"])
    end
    subgraph redis["redis 컨테이너"]
        S4["SET greeting hello EX 60"]
        S5["SET greeting hello"]
    end

    S1 --> S2 --> S3
    S3 -->|있음| S4 --> S6
    S3 -->|없음| S5 --> S6

    classDef default fill:#FFFFFF,stroke:#212121,stroke-width:2px,color:#000000,font-weight:bold
    style client fill:#ECEFF1,stroke:#263238,stroke-width:2px,color:#000000,font-weight:bold
    style app fill:#D8EFC8,stroke:#1E3D0C,stroke-width:2px,color:#000000,font-weight:bold
    style redis fill:#F9D2CF,stroke:#5C120D,stroke-width:2px,color:#000000,font-weight:bold
```

### 값 조회

```mermaid
%%{init: {"flowchart": {"curve": "step"}, "themeVariables": {"background": "#FFFFFF", "mainBkg": "#FFFFFF", "edgeLabelBackground": "transparent", "lineColor": "#212121", "primaryTextColor": "#000000", "secondaryTextColor": "#000000", "tertiaryTextColor": "#000000", "textColor": "#000000", "nodeTextColor": "#000000", "titleColor": "#000000", "actorTextColor": "#000000", "signalTextColor": "#000000", "noteTextColor": "#000000", "labelTextColor": "#000000", "loopTextColor": "#000000"}, "themeCSS": "& { background-color: #FFFFFF !important; } .edgeLabel, .edgeLabel p, .edgeLabel span, .labelBkg { background-color: transparent !important; } .edgeLabel rect { fill: transparent !important; opacity: 0 !important; } text, tspan { fill: #000000 !important; font-weight: bold !important; } .nodeLabel, .edgeLabel, .cluster-label, .label, span, p { color: #000000 !important; font-weight: bold !important; }"}}%%
flowchart TD
    subgraph client["Client"]
        F1(["GET /redis/values/greeting"])
    end
    subgraph app["lab-redis 컨테이너"]
        F2["RedisQueryController<br/>→ RedisQueryService.find"]
        F4{"값이 있는가"}
        F6["0 미만이면 ttlSeconds = null"]
        F7(["200 key, value, ttlSeconds"])
        F8(["404 Not Found"])
    end
    subgraph redis["redis 컨테이너"]
        F3["GET greeting"]
        F5["TTL greeting<br/>(남은 초, 만료 없음 -1, 키 없음 -2)"]
    end

    F1 --> F2 --> F3 --> F4
    F4 -->|없음| F8
    F4 -->|있음| F5 --> F6 --> F7

    classDef default fill:#FFFFFF,stroke:#212121,stroke-width:2px,color:#000000,font-weight:bold
    style client fill:#ECEFF1,stroke:#263238,stroke-width:2px,color:#000000,font-weight:bold
    style app fill:#D8EFC8,stroke:#1E3D0C,stroke-width:2px,color:#000000,font-weight:bold
    style redis fill:#F9D2CF,stroke:#5C120D,stroke-width:2px,color:#000000,font-weight:bold
```

- `GET`과 `TTL`은 서로 다른 명령이라 그 사이에 키가 만료될 수 있다. 그러면 `TTL`이 -2를 돌려주고 응답의 `ttlSeconds`는 null이 된다.

### 값 삭제

```mermaid
%%{init: {"flowchart": {"curve": "step"}, "themeVariables": {"background": "#FFFFFF", "mainBkg": "#FFFFFF", "edgeLabelBackground": "transparent", "lineColor": "#212121", "primaryTextColor": "#000000", "secondaryTextColor": "#000000", "tertiaryTextColor": "#000000", "textColor": "#000000", "nodeTextColor": "#000000", "titleColor": "#000000", "actorTextColor": "#000000", "signalTextColor": "#000000", "noteTextColor": "#000000", "labelTextColor": "#000000", "loopTextColor": "#000000"}, "themeCSS": "& { background-color: #FFFFFF !important; } .edgeLabel, .edgeLabel p, .edgeLabel span, .labelBkg { background-color: transparent !important; } .edgeLabel rect { fill: transparent !important; opacity: 0 !important; } text, tspan { fill: #000000 !important; font-weight: bold !important; } .nodeLabel, .edgeLabel, .cluster-label, .label, span, p { color: #000000 !important; font-weight: bold !important; }"}}%%
flowchart TD
    subgraph client["Client"]
        D1(["DELETE /redis/values/greeting"])
    end
    subgraph app["lab-redis 컨테이너"]
        D2["RedisCommandController<br/>→ RedisCommandService.delete"]
        D4{"지운 키 수"}
        D5(["204 No Content"])
        D6(["404 Not Found"])
    end
    subgraph redis["redis 컨테이너"]
        D3["DEL greeting"]
    end

    D1 --> D2 --> D3 --> D4
    D4 -->|1| D5
    D4 -->|0| D6

    classDef default fill:#FFFFFF,stroke:#212121,stroke-width:2px,color:#000000,font-weight:bold
    style client fill:#ECEFF1,stroke:#263238,stroke-width:2px,color:#000000,font-weight:bold
    style app fill:#D8EFC8,stroke:#1E3D0C,stroke-width:2px,color:#000000,font-weight:bold
    style redis fill:#F9D2CF,stroke:#5C120D,stroke-width:2px,color:#000000,font-weight:bold
```

### 연결 확인

```mermaid
%%{init: {"flowchart": {"curve": "step"}, "themeVariables": {"background": "#FFFFFF", "mainBkg": "#FFFFFF", "edgeLabelBackground": "transparent", "lineColor": "#212121", "primaryTextColor": "#000000", "secondaryTextColor": "#000000", "tertiaryTextColor": "#000000", "textColor": "#000000", "nodeTextColor": "#000000", "titleColor": "#000000", "actorTextColor": "#000000", "signalTextColor": "#000000", "noteTextColor": "#000000", "labelTextColor": "#000000", "loopTextColor": "#000000"}, "themeCSS": "& { background-color: #FFFFFF !important; } .edgeLabel, .edgeLabel p, .edgeLabel span, .labelBkg { background-color: transparent !important; } .edgeLabel rect { fill: transparent !important; opacity: 0 !important; } text, tspan { fill: #000000 !important; font-weight: bold !important; } .nodeLabel, .edgeLabel, .cluster-label, .label, span, p { color: #000000 !important; font-weight: bold !important; }"}}%%
flowchart TD
    subgraph client["Client"]
        P1(["GET /redis/ping"])
    end
    subgraph app["lab-redis 컨테이너"]
        P2["RedisQueryController<br/>→ RedisQueryService.ping"]
        P4{"응답"}
        P5(["200 redis = PONG"])
        P6(["500 (연결 예외)"])
    end
    subgraph redis["redis 컨테이너"]
        P3["PING"]
    end

    P1 --> P2 --> P3 --> P4
    P4 -->|PONG| P5
    P4 -->|연결 실패| P6

    classDef default fill:#FFFFFF,stroke:#212121,stroke-width:2px,color:#000000,font-weight:bold
    style client fill:#ECEFF1,stroke:#263238,stroke-width:2px,color:#000000,font-weight:bold
    style app fill:#D8EFC8,stroke:#1E3D0C,stroke-width:2px,color:#000000,font-weight:bold
    style redis fill:#F9D2CF,stroke:#5C120D,stroke-width:2px,color:#000000,font-weight:bold
```

- Redis는 명령을 하나씩 실행한다. 명령 하나는 원자적이지만, 명령 여러 개를 묶어 원자적으로 실행하지는 않는다.

## 검증하는 테스트

| 테스트 | 확인하는 것 |
| --- | --- |
| `base/repository/RedisCommandRepositoryTest` | `SET`(EX 포함), 덮어쓰기, `DEL` 결과 |
| `base/repository/RedisQueryRepositoryTest` | `GET`, `TTL`(-1, -2), `PING` |
| `base/RedisBaseFlowTest` | 저장 → 조회 → 삭제 → 404 흐름 |
