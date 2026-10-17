# lua — Lua 스크립트로 원자적 발급

수량 확인, 중복 확인, 발급을 Lua 스크립트 하나로 Redis 안에서 실행한다. Redis는 스크립트를 실행하는 동안 다른 명령을 처리하지 않으므로, 확인과 발급 사이에 다른 요청이 끼어들 수 없다.

## 사용 기술

| 기술 | 역할 |
| --- | --- |
| Redis Lua 스크립트 (`EVAL`, `EVALSHA`) | 명령 여러 개를 원자적으로 실행 |
| 스크립트 캐시 (SHA1) | 스크립트 본문 대신 해시만 보내 네트워크 비용을 줄임 |
| Spring Data Redis `RedisScript` | 스크립트 로드, SHA1 계산, `EVALSHA` → `EVAL` 대체 실행 |

- 스크립트: `src/main/resources/redis/couponfirstcome/lua/issue.lua`
- 호출: `LuaCouponIssuer.issue()` → `StringRedisTemplate.execute(issueScript, keys, userId)`

## 데이터 구조

| 키 | 타입 | 값 |
| --- | --- | --- |
| `coupon:{couponId}:quantity` | String | 총 수량 |
| `coupon:{couponId}:issued-users` | Set | 발급받은 사용자 ID |

스크립트 입력과 반환:

| 구분 | 값 |
| --- | --- |
| `KEYS[1]` | `coupon:{couponId}:quantity` |
| `KEYS[2]` | `coupon:{couponId}:issued-users` |
| `ARGV[1]` | 사용자 ID |
| 반환 0 | ISSUED |
| 반환 1 | ALREADY_ISSUED |
| 반환 2 | SOLD_OUT |
| 반환 3 | NOT_FOUND |

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

```mermaid
%%{init: {"flowchart": {"curve": "step"}, "themeVariables": {"background": "#FFFFFF", "mainBkg": "#FFFFFF", "edgeLabelBackground": "transparent", "lineColor": "#212121", "primaryTextColor": "#000000", "secondaryTextColor": "#000000", "tertiaryTextColor": "#000000", "textColor": "#000000", "nodeTextColor": "#000000", "titleColor": "#000000", "actorTextColor": "#000000", "signalTextColor": "#000000", "noteTextColor": "#000000", "labelTextColor": "#000000", "loopTextColor": "#000000"}, "themeCSS": "& { background-color: #FFFFFF !important; } .edgeLabel, .edgeLabel p, .edgeLabel span, .labelBkg { background-color: transparent !important; } .edgeLabel rect { fill: transparent !important; opacity: 0 !important; } text, tspan { fill: #000000 !important; font-weight: bold !important; } .nodeLabel, .edgeLabel, .cluster-label, .label, span, p { color: #000000 !important; font-weight: bold !important; }"}}%%
flowchart TD
    subgraph client["Client"]
        Start(["POST /redis/coupons/welcome/issues?method=lua<br/>userId = user-1"])
    end

    subgraph app["lab-redis 컨테이너"]
        Parse{"CouponIssueMethod.from(method)"}
        Pick["CouponCommandService<br/>issuers 맵에서 LuaCouponIssuer 선택"]
        Sha["EVALSHA (sha1) 2<br/>coupon:{welcome}:quantity<br/>coupon:{welcome}:issued-users user-1 전송"]
        Eval["NOSCRIPT 오류를 받고<br/>EVAL (issue.lua 본문) 재전송"]
        Map["LuaCouponIssuer.toResult(code)"]
        Bad(["400 지원하지 않는 발급 방식"])
        Created(["201 Created"])
        Conflict(["409 + ProblemDetail<br/>이미 발급 / 수량 소진"])
        NotFound(["404 + ProblemDetail<br/>등록되지 않은 쿠폰"])
    end

    subgraph redis["redis 컨테이너"]
        Cached{"스크립트 캐시에<br/>sha1이 있는가"}
        subgraph atomic["메인 스레드가 끊지 않고 실행 (다른 명령은 대기)"]
            Get["GET coupon:{welcome}:quantity"]
            HasQty{"값이 있는가"}
            IsMember{"SISMEMBER issued-users user-1<br/>== 1"}
            Count{"SCARD issued-users<br/>>= quantity"}
            Add["SADD issued-users user-1"]
            R3["return 3"]
            R1["return 1"]
            R2["return 2"]
            R0["return 0"]
        end
    end

    Start --> Parse
    Parse -->|모르는 값| Bad
    Parse -->|LUA| Pick --> Sha --> Cached
    Cached -->|있음| Get
    Cached -->|"없음 (NOSCRIPT)"| Eval --> Get
    Get --> HasQty
    HasQty -->|없음| R3
    HasQty -->|있음| IsMember
    IsMember -->|예| R1
    IsMember -->|아니오| Count
    Count -->|예| R2
    Count -->|아니오| Add --> R0
    R0 & R1 & R2 & R3 --> Map
    Map -->|ISSUED| Created
    Map -->|"ALREADY_ISSUED, SOLD_OUT"| Conflict
    Map -->|NOT_FOUND| NotFound

    classDef default fill:#FFFFFF,stroke:#212121,stroke-width:2px,color:#000000,font-weight:bold
    style client fill:#ECEFF1,stroke:#263238,stroke-width:2px,color:#000000,font-weight:bold
    style app fill:#D8EFC8,stroke:#1E3D0C,stroke-width:2px,color:#000000,font-weight:bold
    style redis fill:#F9D2CF,stroke:#5C120D,stroke-width:2px,color:#000000,font-weight:bold
    style atomic fill:#FFFFFF,stroke:#5C120D,stroke-dasharray:5 5,stroke-width:2px,color:#000000,font-weight:bold
```

- 요청 하나는 Redis와 한 번만 왕복한다 (`EVALSHA`, 캐시에 없을 때만 `EVAL` 한 번 더).
- 앱 서버를 여러 대로 늘려도 스크립트는 같은 Redis에서 하나씩 실행되므로 규칙이 지켜진다.

## 3. 옵션: 동시 요청에서 지켜지는 이유

수량 10, 이미 9명 발급된 상태에서 두 사용자가 동시에 요청한 경우다. 두 요청이 겹치는 시간 순서는 흐름도로 보기 어려워 시퀀스로 그린다.

```mermaid
%%{init: {"themeVariables": {"background": "#FFFFFF", "mainBkg": "#FFFFFF", "edgeLabelBackground": "transparent", "actorBkg": "transparent", "signalColor": "#000000", "sequenceNumberColor": "#000000", "actorBorder": "#212121", "noteBkgColor": "transparent", "noteBorderColor": "#212121", "labelBoxBkgColor": "transparent", "labelBoxBorderColor": "#212121", "activationBkgColor": "transparent", "activationBorderColor": "#212121", "lineColor": "#212121", "primaryTextColor": "#000000", "secondaryTextColor": "#000000", "tertiaryTextColor": "#000000", "textColor": "#000000", "nodeTextColor": "#000000", "titleColor": "#000000", "actorTextColor": "#000000", "signalTextColor": "#000000", "noteTextColor": "#000000", "labelTextColor": "#000000", "loopTextColor": "#000000"}, "themeCSS": "& { background-color: #FFFFFF !important; } .edgeLabel, .edgeLabel p, .edgeLabel span, .labelBkg { background-color: transparent !important; } .edgeLabel rect { fill: transparent !important; opacity: 0 !important; } rect.actor, rect.note, .labelBox, rect[class^=activation] { fill: transparent !important; stroke: #212121 !important; } .messageLine0, .messageLine1 { stroke: #000000 !important; } marker circle { fill: transparent !important; stroke: #000000 !important; } marker path, .arrowheadPath { fill: #000000 !important; stroke: #000000 !important; } text, tspan { fill: #000000 !important; font-weight: bold !important; } .nodeLabel, .edgeLabel, .cluster-label, .label, span, p { color: #000000 !important; font-weight: bold !important; }"}}%%
sequenceDiagram
    autonumber
    participant A as 요청 A (user-10)
    participant R as Redis (메인 스레드 1개)
    participant B as 요청 B (user-11)

    Note over R: quantity = 10, issued-users = 9명
    par 거의 동시에 전송
        A->>R: EVALSHA ... user-10
    and
        B->>R: EVALSHA ... user-11
    end
    Note over R: 먼저 도착한 스크립트를 끝까지 실행한 뒤 다음 것을 실행한다
    R->>R: [A] GET → SISMEMBER → SCARD = 9 → SADD (10명)
    R-->>A: 0 (ISSUED)
    R->>R: [B] GET → SISMEMBER → SCARD = 10 → 수량 초과
    R-->>B: 2 (SOLD_OUT)
```

- 확인과 발급이 한 덩어리로 실행되므로 naive의 check-then-act 경쟁 상태가 생기지 않는다.

## 부하 테스트

nGrinder 테스트 Parameter `method.lua`로 실행한다. 실행 조건과 검증 기준은 [시나리오 README](../README.md)의 `부하 테스트` 섹션을 따른다.

예상 결과: 모두 통과.

## 모니터링

| 도구 | 보는 곳 | 볼 것 |
| --- | --- | --- |
| Grafana | `redis / couponfirstcome` 대시보드 | `method="lua"` 선의 결과별 처리량과 p95 |
| Pinpoint | application `lab-redis` | Redis 명령이 `EVALSHA` 하나로 보인다 |

## 한계와 주의할 점

- 스크립트가 오래 걸리면 그동안 Redis 전체가 멈춘다. 스크립트에는 짧은 명령만 넣는다.
- 스크립트가 쓰는 키는 모두 `KEYS`로 넘긴다. 그래야 Redis Cluster가 키의 슬롯을 확인할 수 있다. 이 시나리오는 `{couponId}` 해시 태그로 두 키를 같은 슬롯에 둔다.
- 스크립트 중간에 오류가 나면 그 전에 실행한 쓰기는 되돌려지지 않는다. 이 스크립트는 마지막에 쓰기(`SADD`) 하나만 하므로 문제가 없다.

## 검증하는 테스트

`lua/repository/LuaCouponIssuerTest`: `ConcurrentCouponIssuerContractTest`를 상속한다 (순차 규칙 + 100명 동시 → 10장 + 같은 사용자 50회 동시 → 1장).
