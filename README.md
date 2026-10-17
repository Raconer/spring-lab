# spring-boot-lab

Redis 같은 기술을 Spring Boot에서 하나씩 테스트해 보는 실습 프로젝트다.
기능마다 모듈을 따로 두고, 각 모듈은 공통 모듈 `core`를 가져다 쓴다. 인프라는 모듈별 Docker Compose로 띄운다.

## 버전

| 항목 | 버전 |
| --- | --- |
| Spring Boot | 4.1.1 |
| Java | 25 (Temurin) |
| Kotlin | 2.3.21 |
| Gradle | 9.8.1 (Kotlin DSL) |

Kotlin과 Gradle 버전은 Spring Initializr 기본값을 따른다.

## 모듈 구조

```
spring-boot-lab
├── build.gradle.kts         # 플러그인 버전, Java 25 toolchain, Spring Boot BOM, Kotlin 공통 설정
├── settings.gradle.kts      # 모듈 등록
├── Dockerfile               # 모든 모듈 공통 이미지 빌드 (build args MODULE로 모듈 선택)
├── core                 # 공통 Spring Boot 설정 (Java)
├── lab-redis                # Redis 실습 (Kotlin)
│   ├── compose.yaml         # lab-redis 그룹: 앱, redis, redis-exporter
│   └── compose.pinpoint.yaml # 앱에 Pinpoint 에이전트를 붙이는 설정
├── loadtest                 # 부하 테스트 (nGrinder, 공용. Gradle 모듈 아님)
│   ├── compose.yaml         # loadtest 그룹: nGrinder controller, agent
│   └── scripts/             # nGrinder 스크립트 (기술/시나리오.groovy)
├── monitoring               # 지표·추적 모니터링 스택 (공용)
│   ├── compose.yaml         # monitoring 그룹: Prometheus, Grafana
│   ├── prometheus/          # 수집 대상
│   ├── grafana/             # 데이터소스, 대시보드
│   └── pinpoint
│       ├── compose.yaml     # pinpoint 그룹: Pinpoint 서버
│       └── agent.yaml       # Pinpoint 에이전트 (모듈의 compose.pinpoint.yaml이 extends로 가져감)
└── .run                     # IntelliJ 실행 설정 (그룹별 + 전부 띄우는 lab-redis-all)
```

- `core`: 공통 의존성(`spring-boot-starter`, `spring-boot-starter-webmvc`, springdoc, actuator)을 `api`로 노출한다. Prometheus 레지스트리를 넣어 모든 모듈이 `/actuator/prometheus`를 연다. 공통 테스트 의존성(Spring Boot Test, Testcontainers)은 `testFixtures`로 노출한다.
- `lab-redis`: `core`를 import해서 Redis를 실습한다. 목적(시나리오)별로 패키지를 나누고, 각 시나리오는 쓰기(Command)와 읽기(Query)를 나눠서 구현한다.
- `loadtest`: nGrinder controller와 agent를 띄우고, 시나리오별 Groovy 스크립트로 대상 모듈의 시나리오 API에 부하를 준다. 부하 후 검증(예: 초과 발급 여부)은 스크립트가 로그로 남긴다.
- `monitoring`: Prometheus + Grafana (지표 추이)와 Pinpoint (요청 단위 추적). 모듈과 별도 그룹으로 띄운다.

### 기능 모듈 패키지 구조

```
com.spring.lab.redis
├── LabRedisApplication.kt
├── base             # 기본 명령 실습: String 저장·조회·삭제, TTL, PING
└── couponfirstcome  # 시나리오: 쿠폰 선착순 발급
    ├── common       # 방식 공통: API, 등록·조회, 키 규칙
    ├── naive        # 방식: 확인 후 발급 (원자성 없음, 비교 기준선)
    ├── lua          # 방식: Lua 스크립트로 원자적 실행
    ├── incr         # 방식: INCR 순번 + 초과 시 되돌리기
    └── lock         # 방식: Redisson 분산 락
```

각 시나리오 안은 `controller`, `service`, `repository`, `domain`, `dto`로 나눈다. 자세한 규칙은 `CLAUDE.md`를 따른다.

### lab-redis API

`base` — 기본 명령

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `PUT` | `/redis/values/{key}` | 값 저장. 본문 `{"value": "...", "ttlSeconds": 60}`, `ttlSeconds`는 생략 가능 |
| `DELETE` | `/redis/values/{key}` | 값 삭제. 없는 키면 404 |
| `GET` | `/redis/values/{key}` | 값과 남은 TTL 조회. 없는 키면 404 |
| `GET` | `/redis/ping` | Redis에 PING을 보내 연결 확인 |

`couponfirstcome` — 쿠폰 선착순 발급. 같은 API에서 `method` 파라미터로 발급 방식을 골라 비교한다.

| method | 원리 | 동시 요청 |
| --- | --- | --- |
| `naive` | GET·SISMEMBER·SCARD로 확인 후 SADD | 초과 발급된다 (비교 기준선) |
| `lua` (기본값) | 확인과 발급을 Lua 스크립트(`redis/couponfirstcome/lua/issue.lua`) 하나로 실행 | 수량을 지킨다 |
| `incr` | SADD로 기록 → INCR 순번이 수량을 넘으면 SREM으로 되돌림 | 수량을 지킨다. 되돌리기 전 잠깐 Set이 수량보다 커 보인다 |
| `lock` | Redisson 분산 락 안에서 naive와 같은 로직 실행 | 수량을 지킨다. 요청이 줄을 서서 가장 느리다 |

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `PUT` | `/redis/coupons/{couponId}` | 쿠폰 등록. 본문 `{"quantity": 100}`. 다시 등록하면 발급 기록을 지우고 처음부터 발급한다 |
| `POST` | `/redis/coupons/{couponId}/issues?method=lua` | 발급 요청. 본문 `{"userId": "..."}`. 성공 201, 중복·소진 409, 없는 쿠폰 404, 모르는 method 400 |
| `GET` | `/redis/coupons/{couponId}` | 수량, 발급 수, 남은 수량 조회 |
| `GET` | `/redis/coupons/{couponId}/issues/{userId}` | 사용자의 발급 여부 조회 |

## 언어 선택

모듈마다 Java나 Kotlin을 고를 수 있다.

- Kotlin 모듈: `build.gradle.kts`에 `kotlin("jvm")`을 적용한다. 루트 빌드가 `plugin.spring`, `kotlin-reflect`, `jackson-module-kotlin`, 컴파일러 옵션을 자동으로 얹는다.
- Java 모듈: `kotlin("jvm")`을 빼면 된다.

## 실행

사전 준비: Docker Desktop을 켜 둔다. 터미널에서 Gradle을 돌릴 때는 `JAVA_HOME`을 JDK 17 이상으로 잡는다. 모듈 빌드에 쓰는 Java 25는 toolchain이 찾거나 내려받는다.

```bash
# 테스트 (Testcontainers가 Redis 컨테이너를 직접 띄운다)
./gradlew :lab-redis:test

# 앱 실행 (spring-boot-docker-compose가 lab-redis/compose.yaml의 redis를 같이 띄운다)
./gradlew :lab-redis:bootRun

# 인프라만 띄우기
docker compose -f lab-redis/compose.yaml up -d redis
```

Docker로 띄울 때는 그룹마다 따로 띄운다. 원하는 그룹만 실행하면 된다. 그룹끼리는 `spring-boot-lab` 네트워크로 통신한다.

| 그룹 | IntelliJ 실행 설정 | 컨테이너 | 터미널 |
| --- | --- | --- | --- |
| `lab-redis` | `lab-redis` | `redis.app`, `redis.server`, `redis.exporter`, `redis.pinpoint-agent` | `docker compose -f lab-redis/compose.yaml -f lab-redis/compose.pinpoint.yaml --profile local-test up --build -d` |
| `monitoring` | `monitoring` | `monitoring.prometheus`, `monitoring.grafana` | `docker compose -f monitoring/compose.yaml up -d` |
| `pinpoint` | `pinpoint` | `pinpoint.web`, `pinpoint.collector`, `pinpoint.hbase`, `pinpoint.redis`, `pinpoint.zookeeper` | `docker compose -f monitoring/pinpoint/compose.yaml up -d` |
| `loadtest` | `loadtest` | `loadtest.controller`, `loadtest.agent` | `docker compose -f loadtest/compose.yaml up -d` |
| `lab-redis` + 모니터링 | `lab-redis-all` | `lab-redis`, `monitoring`, `pinpoint`의 11개 | 위 명령 3개 |

- IntelliJ 실행 설정은 실행 목록의 `docker` 폴더에 있다 (`.run/`).
- 종료는 같은 명령의 `up ...`을 `down`으로 바꾼다.
- `lab-redis` 그룹은 Pinpoint 에이전트를 항상 붙인다. `pinpoint`가 꺼져 있어도 앱은 정상으로 뜨고, 추적 데이터만 쌓이지 않는다.
- `lab-redis/compose.yaml`을 프로필 없이 실행하면 `redis.server`만 뜬다. `bootRun`도 이것만 띄운다.

확인할 주소:

| 주소 | 내용 |
| --- | --- |
| `http://localhost:8080/swagger-ui.html` | `lab-redis` API |
| `http://localhost:8090` | nGrinder controller (`admin` / `admin`) |
| `http://localhost:9090` | Prometheus |
| `http://localhost:3000` | Grafana (로그인 없음). 대시보드 `lab 공통`, `redis / couponfirstcome` |
| `http://localhost:8079` | Pinpoint web |

## 부하 테스트

nGrinder로 부하를 준다. `loadtest` 그룹을 띄운 뒤 `http://localhost:8090`(`admin` / `admin`)에서 진행한다.

1. `Script` → `Create` → `Upload`로 `loadtest/scripts/(기술)/(시나리오).groovy`를 올린다. 예: `loadtest/scripts/redis/couponfirstcome.groovy`
2. `Performance Test` → `Create Test`에서 올린 스크립트를 고르고, agent 1, 가상 사용자 수(프로세스 1, 스레드 = 가상 사용자 수), 실행 횟수를 넣는다.
3. 옵션(예: 발급 방식)은 `상세 설정 보기`의 Parameter에 `key.value`를 쉼표로 이어 넣고(예: `method.lua,quantity.20`) `Save and Start`를 누른다.
4. 결과(TPS, 응답 시간, 에러 수)는 테스트 상세 화면에서 보고, 검증 결과는 테스트 상세의 로그에서 `[검증` 줄로 본다.

nGrinder 이미지는 amd64뿐이라 Apple Silicon에서는 에뮬레이션으로 돈다. agent가 낼 수 있는 최대 TPS가 실제 장비보다 낮다. 시나리오별 옵션과 검증 기준은 그 시나리오 README의 `부하 테스트` 섹션에 있다.

## 모니터링

| 도구 | 보는 것 | 수집 방식 |
| --- | --- | --- |
| Prometheus + Grafana | 지표 추이: HTTP 처리량·응답 시간, JVM, 시나리오 메트릭, 인프라 지표 | 앱의 `/actuator/prometheus`, 인프라 exporter를 5초마다 수집 |
| Pinpoint | 요청 하나의 호출 경로와 구간별 시간 (Controller → Redis 명령까지), 서버맵 | 앱 JVM에 붙인 Pinpoint 에이전트가 collector로 전송 |

Pinpoint는 컨테이너가 많아 메모리를 많이 쓴다. Docker Desktop 메모리를 넉넉히 잡고 필요할 때만 띄운다.

## 새 기능 모듈 추가

`lab-kafka`를 추가하는 예시다.

1. `settings.gradle.kts`의 `include(...)`에 `"lab-kafka"`를 추가한다.
2. `lab-kafka/build.gradle.kts`를 만든다.

   ```kotlin
   plugins {
       kotlin("jvm") // Java 모듈이면 이 줄을 뺀다
       id("org.springframework.boot")
   }

   dependencies {
       implementation(project(":core"))
       // 테스트할 기술의 starter
       developmentOnly("org.springframework.boot:spring-boot-docker-compose")

       testImplementation(testFixtures(project(":core")))
   }
   ```

3. `lab-kafka/compose.yaml`을 만든다. `lab-redis/compose.yaml`처럼 `name: lab-kafka`, `spring-boot-lab` 네트워크, 인프라, exporter와 앱 컨테이너(`local-test` 프로필, 루트 `Dockerfile`에 `MODULE: lab-kafka`)를 적는다. 컨테이너 이름은 `kafka.(역할)`로 한다. Pinpoint 에이전트는 `compose.pinpoint.yaml`에서 `monitoring/pinpoint/agent.yaml`을 `extends`로 가져온다. `.run/lab-kafka.run.xml`과 공용 그룹까지 묶은 `.run/lab-kafka-all.run.xml`을 만든다.
4. 애플리케이션 클래스에 `@Import(LabCoreConfiguration::class)`를 붙인다.
5. `com.spring.lab.kafka.base` 패키지에 기본 명령 실습부터 만든다. 실전 상황은 그 뒤에 시나리오 패키지로 하나씩 추가한다.
6. 모니터링과 부하 테스트를 연결한다.
   - `monitoring/prometheus/prometheus.yml`에 앱과 exporter 수집 대상을 추가한다.
   - 시나리오마다 `monitoring/grafana/dashboards/kafka/(시나리오).json` 대시보드를 만든다.
   - 시나리오마다 `loadtest/scripts/kafka/(시나리오).groovy` nGrinder 스크립트를 만든다. 규칙은 `loadtest/CLAUDE.md`를 따른다.
