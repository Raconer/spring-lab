# spring-boot-lab

Redis 같은 기술을 Spring Boot에서 하나씩 테스트해 보는 실습 프로젝트다.
기능마다 모듈을 따로 두고, 각 모듈은 공통 모듈 `lab-core`를 가져다 쓴다. 인프라는 모듈별 Docker Compose로 띄운다.

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
├── build.gradle.kts     # 플러그인 버전, Java 25 toolchain, Spring Boot BOM, Kotlin 공통 설정
├── settings.gradle.kts  # 모듈 등록
├── lab-core             # 공통 Spring Boot 설정 (Java)
└── lab-redis            # Redis 실습 (Kotlin)
    ├── compose.yaml     # 이 모듈이 쓰는 인프라
    └── Dockerfile       # 앱을 컨테이너로 띄울 때 사용
```

- `lab-core`: 공통 의존성(`spring-boot-starter`, `spring-boot-starter-webmvc`, springdoc)을 `api`로 노출한다. 공통 테스트 의존성(Spring Boot Test, Testcontainers)은 `testFixtures`로 노출한다.
- `lab-redis`: `lab-core`를 import해서 Redis를 실습한다. 목적(시나리오)별로 패키지를 나누고, 각 시나리오는 쓰기(Command)와 읽기(Query)를 나눠서 구현한다.

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

# 앱까지 컨테이너로 띄우기 (local-test 프로필)
docker compose -f lab-redis/compose.yaml --profile local-test up --build
```

앱을 띄운 뒤 확인할 주소:

- `http://localhost:8080/redis/ping`
- `http://localhost:8080/swagger-ui.html`

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
       implementation(project(":lab-core"))
       // 테스트할 기술의 starter
       developmentOnly("org.springframework.boot:spring-boot-docker-compose")

       testImplementation(testFixtures(project(":lab-core")))
   }
   ```

3. 필요한 인프라를 `lab-kafka/compose.yaml`에 적는다.
4. 애플리케이션 클래스에 `@Import(LabCoreConfiguration::class)`를 붙인다.
5. `com.spring.lab.kafka.base` 패키지에 기본 명령 실습부터 만든다. 실전 상황은 그 뒤에 시나리오 패키지로 하나씩 추가한다.
