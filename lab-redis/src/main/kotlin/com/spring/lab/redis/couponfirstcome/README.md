# couponfirstcome — 쿠폰 선착순 발급

정해진 수량의 쿠폰을 먼저 요청한 사용자에게만, 한 사람당 한 장씩 발급한다. 같은 요구사항을 네 가지 방식으로 구현해서 동시 요청에서 어떻게 다르게 동작하는지 비교한다.

## 지켜야 할 규칙

1. 발급 수는 등록한 수량을 넘지 않는다.
2. 한 사용자는 한 장만 받는다.
3. 동시에 요청이 몰려도 1, 2가 지켜진다.

## 방식 비교

| 방식 | 핵심 기술 | 규칙 3 (동시 요청) | 자세히 |
| --- | --- | --- | --- |
| `naive` | 명령 여러 개를 따로 실행 | 깨진다 (초과 발급) | [naive/README.md](naive/README.md) |
| `lua` | Lua 스크립트 (`EVALSHA`) | 지킨다 | [lua/README.md](lua/README.md) |
| `incr` | 원자 명령 `SADD`, `INCR` + 되돌리기 | 지킨다 (잠깐 Set이 커 보임) | [incr/README.md](incr/README.md) |
| `lock` | Redisson 분산 락 | 지킨다 (가장 느림) | [lock/README.md](lock/README.md) |

## 데이터 구조

모든 방식이 같은 키를 쓴다. 키 규칙은 `common/repository/CouponRedisKeys.kt` 한 곳에 있다.

| 키 | 타입 | 값 | 쓰는 방식 |
| --- | --- | --- | --- |
| `coupon:{couponId}:quantity` | String | 총 수량 | 전부 |
| `coupon:{couponId}:issued-users` | Set | 발급받은 사용자 ID | 전부 (조회 기준) |
| `coupon:{couponId}:issue-count` | String (정수) | 발급 순번 카운터 | `incr` |
| `coupon:{couponId}:issue-lock` | Hash (Redisson 락) | 락을 가진 클라이언트·스레드 | `lock` |

- `{couponId}`는 해시 태그다. 중괄호 안 문자열로 슬롯을 정하므로 같은 쿠폰의 키는 Redis Cluster에서도 한 노드에 모인다. 여러 키를 쓰는 Lua 스크립트를 실행하려면 이 조건이 필요하다.

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

등록, 발급, 조회의 공통 흐름이다. 노드는 실행되는 서버별로 묶었다: Client(회색), lab-redis 컨테이너(초록), redis 컨테이너(빨강). 발급의 방식별 Redis 명령은 각 방식 README의 흐름도에 있다.

### 쿠폰 등록

```mermaid
%%{init: {"flowchart": {"curve": "step"}, "themeVariables": {"background": "#FFFFFF", "mainBkg": "#FFFFFF", "edgeLabelBackground": "transparent", "lineColor": "#212121", "primaryTextColor": "#000000", "secondaryTextColor": "#000000", "tertiaryTextColor": "#000000", "textColor": "#000000", "nodeTextColor": "#000000", "titleColor": "#000000", "actorTextColor": "#000000", "signalTextColor": "#000000", "noteTextColor": "#000000", "labelTextColor": "#000000", "loopTextColor": "#000000"}, "themeCSS": "& { background-color: #FFFFFF !important; } .edgeLabel, .edgeLabel p, .edgeLabel span, .labelBkg { background-color: transparent !important; } .edgeLabel rect { fill: transparent !important; opacity: 0 !important; } text, tspan { fill: #000000 !important; font-weight: bold !important; } .nodeLabel, .edgeLabel, .cluster-label, .label, span, p { color: #000000 !important; font-weight: bold !important; }"}}%%
flowchart TD
    subgraph client["Client"]
        C1(["PUT /redis/coupons/welcome<br/>quantity = 100"])
    end
    subgraph app["lab-redis 컨테이너"]
        C2["CouponCommandController<br/>→ CouponCommandRepository.create"]
        C5(["204 No Content"])
    end
    subgraph redis["redis 컨테이너"]
        C3["DEL coupon:{welcome}:issued-users<br/>coupon:{welcome}:issue-count"]
        C4["SET coupon:{welcome}:quantity 100"]
    end

    C1 --> C2 --> C3 --> C4 --> C5

    classDef default fill:#FFFFFF,stroke:#212121,stroke-width:2px,color:#000000,font-weight:bold
    style client fill:#ECEFF1,stroke:#263238,stroke-width:2px,color:#000000,font-weight:bold
    style app fill:#D8EFC8,stroke:#1E3D0C,stroke-width:2px,color:#000000,font-weight:bold
    style redis fill:#F9D2CF,stroke:#5C120D,stroke-width:2px,color:#000000,font-weight:bold
```

- 다시 등록하면 발급 기록과 순번을 지우고 처음부터 발급한다.

### 발급

```mermaid
%%{init: {"flowchart": {"curve": "step"}, "themeVariables": {"background": "#FFFFFF", "mainBkg": "#FFFFFF", "edgeLabelBackground": "transparent", "lineColor": "#212121", "primaryTextColor": "#000000", "secondaryTextColor": "#000000", "tertiaryTextColor": "#000000", "textColor": "#000000", "nodeTextColor": "#000000", "titleColor": "#000000", "actorTextColor": "#000000", "signalTextColor": "#000000", "noteTextColor": "#000000", "labelTextColor": "#000000", "loopTextColor": "#000000"}, "themeCSS": "& { background-color: #FFFFFF !important; } .edgeLabel, .edgeLabel p, .edgeLabel span, .labelBkg { background-color: transparent !important; } .edgeLabel rect { fill: transparent !important; opacity: 0 !important; } text, tspan { fill: #000000 !important; font-weight: bold !important; } .nodeLabel, .edgeLabel, .cluster-label, .label, span, p { color: #000000 !important; font-weight: bold !important; }"}}%%
flowchart TD
    subgraph client["Client"]
        I1(["POST /redis/coupons/welcome/issues?method=...<br/>userId = user-1"])
    end
    subgraph app["lab-redis 컨테이너"]
        I2{"CouponIssueMethod.from(method)"}
        I3(["400 + ProblemDetail"])
        I4["CouponCommandService<br/>issuers 맵에서 구현체 선택"]
        I5["방식별 CouponIssuer.issue()<br/>naive / lua / incr / lock"]
        I7{"CouponIssueResult"}
        I8(["201 Created"])
        I9(["409 + ProblemDetail"])
        I10(["404 + ProblemDetail"])
    end
    subgraph redis["redis 컨테이너"]
        I6[["방식별 명령 실행<br/>(각 방식 README 참고)"]]
    end

    I1 --> I2
    I2 -->|모르는 값| I3
    I2 -->|"NAIVE, LUA, INCR, LOCK"| I4 --> I5 --> I6 --> I7
    I7 -->|ISSUED| I8
    I7 -->|"ALREADY_ISSUED, SOLD_OUT"| I9
    I7 -->|NOT_FOUND| I10

    classDef default fill:#FFFFFF,stroke:#212121,stroke-width:2px,color:#000000,font-weight:bold
    style client fill:#ECEFF1,stroke:#263238,stroke-width:2px,color:#000000,font-weight:bold
    style app fill:#D8EFC8,stroke:#1E3D0C,stroke-width:2px,color:#000000,font-weight:bold
    style redis fill:#F9D2CF,stroke:#5C120D,stroke-width:2px,color:#000000,font-weight:bold
```

### 조회

```mermaid
%%{init: {"flowchart": {"curve": "step"}, "themeVariables": {"background": "#FFFFFF", "mainBkg": "#FFFFFF", "edgeLabelBackground": "transparent", "lineColor": "#212121", "primaryTextColor": "#000000", "secondaryTextColor": "#000000", "tertiaryTextColor": "#000000", "textColor": "#000000", "nodeTextColor": "#000000", "titleColor": "#000000", "actorTextColor": "#000000", "signalTextColor": "#000000", "noteTextColor": "#000000", "labelTextColor": "#000000", "loopTextColor": "#000000"}, "themeCSS": "& { background-color: #FFFFFF !important; } .edgeLabel, .edgeLabel p, .edgeLabel span, .labelBkg { background-color: transparent !important; } .edgeLabel rect { fill: transparent !important; opacity: 0 !important; } text, tspan { fill: #000000 !important; font-weight: bold !important; } .nodeLabel, .edgeLabel, .cluster-label, .label, span, p { color: #000000 !important; font-weight: bold !important; }"}}%%
flowchart TD
    subgraph client["Client"]
        Q1(["GET /redis/coupons/welcome"])
    end
    subgraph app["lab-redis 컨테이너"]
        Q2["CouponQueryController<br/>→ CouponQueryService.find"]
        Q4{"값이 있는가"}
        Q6["remaining = max(quantity - issuedCount, 0)"]
        Q7(["200 quantity, issuedCount, remaining"])
        Q8(["404 Not Found"])
    end
    subgraph redis["redis 컨테이너"]
        Q3["GET coupon:{welcome}:quantity"]
        Q5["SCARD coupon:{welcome}:issued-users"]
    end

    Q1 --> Q2 --> Q3 --> Q4
    Q4 -->|없음| Q8
    Q4 -->|있음| Q5 --> Q6 --> Q7

    classDef default fill:#FFFFFF,stroke:#212121,stroke-width:2px,color:#000000,font-weight:bold
    style client fill:#ECEFF1,stroke:#263238,stroke-width:2px,color:#000000,font-weight:bold
    style app fill:#D8EFC8,stroke:#1E3D0C,stroke-width:2px,color:#000000,font-weight:bold
    style redis fill:#F9D2CF,stroke:#5C120D,stroke-width:2px,color:#000000,font-weight:bold
```

- 사용자별 발급 여부(`GET .../issues/{userId}`)는 `SISMEMBER coupon:{welcome}:issued-users user-1` 한 번으로 확인한다.

## 부하 테스트

nGrinder 스크립트 `loadtest/scripts/redis/couponfirstcome.groovy`로 부하를 준다. 실행마다 새 쿠폰(`load-(시작 시각)`)을 등록하고 발급 요청을 보낸 뒤, 쿠폰 상태를 조회해서 검증한다.

| 항목 | 값 |
| --- | --- |
| Parameter (`key.value`를 쉼표로) | `method`: `naive`, `lua`, `incr`, `lock` (기본 `naive`) / `quantity`: 쿠폰 수량 (기본 10) / `sameUser`: `true`면 모든 요청이 `user-1` (기본 `false`: 요청마다 다른 사용자). 예: `method.lua`, `method.lock,quantity.20` |
| 실행 조건 | agent 1, 프로세스 1, 스레드 100 (가상 사용자 100), 실행 횟수 1 |
| 검증 | 발급 수가 수량을 넘지 않는다 / 201 응답 수와 실제 발급 수가 같다 / 받을 수 있는 만큼(수량과 사용자 수 중 작은 값) 모두 발급된다. `@AfterProcess`가 `[검증 통과]`, `[검증 실패]` 로그로 남긴다 |

1. `http://localhost:8090`(nGrinder, `admin` / `admin`)의 `Script`에 `couponfirstcome.groovy`를 올린다.
2. `Performance Test` → `Create Test`에서 스크립트를 고르고, 위 실행 조건과 `상세 설정 보기`의 Parameter(예: `method.lua`)를 넣고 시작한다.

| 방식 | 예상 결과 |
| --- | --- |
| `naive` | 검증 실패. 초과 발급되고, 201 응답 수가 실제 발급 수보다 많다 |
| `lua` | 모두 통과 |
| `incr` | 모두 통과 |
| `lock` | 모두 통과. 응답 시간이 가장 길다 |

## 모니터링

| 도구 | 보는 곳 | 볼 것 |
| --- | --- | --- |
| Grafana | `redis / couponfirstcome` 대시보드 | 선택한 시간 범위의 발급 결과(방식·결과별)와 응답 상태 수: 시간 범위를 테스트 구간에 맞추면 그 테스트 결과만 나온다 / `coupon_issue_seconds`: 방식·결과별 처리량과 p95 / Redis 명령 처리량·연결 수 / 발급 API 요청 처리량과 p95 (서버 기준) |
| nGrinder | 테스트 상세 | TPS, 평균 응답 시간, 에러 수 (클라이언트 기준), 검증 로그 |
| Pinpoint | application `lab-redis` | 요청 하나의 구간: `CouponCommandController` → `CouponCommandService` → 방식 구현체 → Redis 명령 (방식마다 다른 명령은 각 방식 README) |

## 검증하는 테스트

| 테스트 | 확인하는 것 |
| --- | --- |
| `common/repository/CouponIssuerContractTest` | 규칙 1, 2를 순차 요청으로 (모든 방식이 상속) |
| `common/repository/ConcurrentCouponIssuerContractTest` | 규칙 3: 100명 동시 → 10장, 같은 사용자 50회 동시 → 1장 (`lua`, `incr`, `lock`이 상속) |
| `CouponFirstComeFlowTest` | 등록 → 발급 → 중복·소진 거절 → 조회 흐름을 네 방식 모두로 |
