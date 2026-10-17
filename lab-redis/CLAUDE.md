# lab-redis

Redis를 실습하는 기능 모듈이다. Kotlin으로 작성한다. 루트 `CLAUDE.md`의 공통 규칙을 함께 따른다.

## 시나리오

| 패키지 | 내용 | URL |
| --- | --- | --- |
| `base` | String 저장·조회·삭제, TTL, PING | `/redis/values/{key}`, `/redis/ping` |
| `couponfirstcome` | 쿠폰 선착순 발급. 방식 `naive`, `lua`, `incr`, `lock`을 비교한다 | `/redis/coupons/...` |

시나리오를 추가하거나 없애면 이 표와 루트 README의 API 표를 같이 고친다. 시나리오마다 동작 설명은 그 시나리오 폴더의 `README.md`에 있다 (예: `couponfirstcome/README.md`, `couponfirstcome/lua/README.md`).

## 계층 나누기: Command / Query

controller, service, repository를 쓰기(Command)와 읽기(Query)로 나눈다. 이름은 `<대상>CommandController`, `<대상>QueryService`처럼 짓는다.

- Command: 저장·수정·삭제만 한다. 조회 결과를 응답으로 돌려주지 않는다. 성공은 상태 코드만 응답한다.
- Query: 조회만 한다. 상태를 바꾸지 않는다.
- Command 쪽에서 조회가 필요해도 Query repository를 가져다 쓰지 않고, 필요한 확인은 Command repository 안에서 한다.
- 방식이 여러 개인 시나리오에서 방식 구현체(예: `CouponIssuer`)는 Command repository로 본다.

## Redis 규칙

- Redis 접근은 `StringRedisTemplate`으로 한다.
- 키 규칙은 시나리오의 `repository`에 `<시나리오>RedisKeys` 객체로 한 곳에 두고, Command/Query repository와 방식 구현체가 같이 쓴다.
- 한 시나리오의 키는 `{<id>}` 해시 태그로 묶는다 (예: `coupon:{couponId}:quantity`). Redis Cluster에서도 여러 키를 쓰는 Lua 스크립트를 실행할 수 있게 하기 위해서다.
- Lua 스크립트는 `src/main/resources/redis/<시나리오>/<방식>/`에 두고 `RedisScript.of(ClassPathResource(...), Long::class.javaObjectType)`로 읽는다. 반환 코드와 의미는 스크립트 주석에 적는다.
- Redisson은 `couponfirstcome.lock`에서만 쓴다. `RedissonClient`는 `DataRedisConnectionDetails`로 만들어서 Spring Data Redis와 같은 접속 정보를 따른다.

## 부하 테스트와 모니터링

| 시나리오 | 메트릭 (service에서 기록) | 부하 스크립트 (nGrinder) | 대시보드 |
| --- | --- | --- | --- |
| `couponfirstcome` | `coupon.issue` Timer, 태그 `method`, `result` (`CouponCommandService`) | `loadtest/scripts/redis/couponfirstcome.groovy` | `monitoring/grafana/dashboards/redis/couponfirstcome.json` |
| `base` | 없음 (HTTP 공통 지표만) | 없음 | 공통 대시보드 |

- Redis 서버 지표는 `compose.yaml`의 `redis-exporter`(`local-test` 프로필, 컨테이너 `redis.exporter`)가 내보낸다.
- Pinpoint 연결은 `compose.pinpoint.yaml`에 있다. 에이전트(`redis.pinpoint-agent`)는 `monitoring/pinpoint/agent.yaml`을 `extends`로 가져와 lab-redis 그룹 안에서 띄운다. agentId와 applicationName은 `lab-redis`다.
- 시나리오를 추가하면 이 표를 채우고, 메트릭·부하 스크립트·대시보드를 같이 만든다.

## 테스트

- 컨테이너 설정: `src/test/kotlin/com/spring/lab/redis/RedisTestContainerConfig.kt` (`redis:latest`, `compose.yaml`과 같은 이미지)
- repository와 방식 구현체 테스트는 `@DataRedisTest` + `@Import(RedisTestContainerConfig::class, 대상::class)`로 작성한다. 대상이 쓰는 설정(예: `RedissonConfig`)도 같이 `@Import`한다.
- `@BeforeEach`에서 `redisTemplate.requiredConnectionFactory.connection.use { it.serverCommands().flushDb() }`로 비운다.
- Command 테스트는 결과를 `StringRedisTemplate`으로 직접 확인하고, Query 테스트는 데이터를 `StringRedisTemplate`으로 직접 넣고 시작한다.
- `couponfirstcome`의 계약 테스트 구조:
  - `CouponIssuerContractTest`(순차 규칙) ← `ConcurrentCouponIssuerContractTest`(동시 규칙) ← `LuaCouponIssuerTest`, `IncrCouponIssuerTest`, `LockCouponIssuerTest`
  - `NaiveCouponIssuerTest`는 순차 규칙만 상속하고, 동시 요청에서 초과 발급되는 것을 보여주는 테스트를 따로 둔다.
- 실행 예:
  - 모듈 전체: `./gradlew :lab-redis:test`
  - 시나리오 하나: `./gradlew :lab-redis:test --tests '*.couponfirstcome.*'`
  - 방식 하나: `./gradlew :lab-redis:test --tests '*.couponfirstcome.lock.*'`
  - Docker 없이 되는 것만: `./gradlew :lab-redis:test --tests '*.service.*' --tests '*.controller.*'`
