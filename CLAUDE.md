# spring-boot-lab

Spring Boot에서 기술을 하나씩 실습하는 Gradle 멀티모듈 프로젝트다. 모듈 구조, 실행 방법, 새 모듈 추가 절차는 README를 따른다.

@README.md

## 빌드 규칙

- 플러그인 버전은 루트 `build.gradle.kts`에만 선언한다. 모듈은 `kotlin("jvm")`, `id("org.springframework.boot")`처럼 버전 없이 적용한다.
- 의존성 버전은 Spring Boot BOM에 맡긴다. BOM에 없는 라이브러리만 버전을 직접 적고, 이유를 주석으로 남긴다 (예: `lab-core`의 springdoc).
- Java toolchain은 Java 25(Temurin)로 고정한다. 모듈에서 따로 바꾸지 않는다.
- Spring Boot 4 기준 starter 이름을 쓴다. 예: `spring-boot-starter-webmvc`, `spring-boot-starter-data-redis-test`.
- Testcontainers는 2.x 아티팩트 이름을 쓴다. 예: `org.testcontainers:testcontainers-junit-jupiter`.

## 모듈 규칙

- 모든 모듈에 공통인 의존성과 설정만 `lab-core`에 둔다. 특정 기술에만 쓰는 것은 그 기능 모듈에 둔다.
- `lab-core`는 런타임 의존성을 `api`로, 테스트 의존성을 `testFixturesApi`로 노출한다. 기능 모듈은 `implementation(project(":lab-core"))`와 `testImplementation(testFixtures(project(":lab-core")))`로 가져간다.
- 기능 모듈 이름은 `lab-<기술>`, 패키지는 `com.spring.lab.<기술>`로 한다.
- 기능 모듈의 애플리케이션 클래스는 `@Import(LabCoreConfiguration::class)`로 공통 설정을 가져온다.
- 인프라는 그 모듈 디렉터리의 `compose.yaml`에 둔다. 앱 컨테이너 서비스는 `profiles: ['local-test']`로 분리해서 `bootRun`이 같이 띄우지 않게 한다.
- 모듈을 추가하거나 없애면 `settings.gradle.kts`와 README의 모듈 구조를 같이 고친다.

## 테스트

- 인프라 연결 테스트는 Testcontainers와 `@ServiceConnection`으로 작성한다. 로컬에 띄운 compose 컨테이너에 의존하지 않는다.
- 컨테이너 이미지는 그 모듈의 `compose.yaml`과 같은 이미지를 쓴다.
- 테스트 실행: `./gradlew :<모듈>:test` (Docker 필요)
