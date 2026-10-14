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
- `lab-redis`: `lab-core`를 import해서 Redis 연결을 확인한다. `GET /redis/ping`으로 Redis에 PING을 보낸다.

## 언어 선택

모듈마다 Java나 Kotlin을 고를 수 있다.

- Kotlin 모듈: `build.gradle.kts`에 `kotlin("jvm")`을 적용한다. 루트 빌드가 `plugin.spring`, `kotlin-reflect`, 컴파일러 옵션을 자동으로 얹는다.
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
