# core

모든 기능 모듈이 가져다 쓰는 공통 Spring Boot 모듈이다. Java로 작성한다. 루트 `CLAUDE.md`의 공통 규칙을 함께 따른다.

## 무엇을 두는가

- 모든 기능 모듈에 공통인 의존성과 설정만 둔다. 특정 기술(Redis, MQ 등)에만 쓰는 것은 두지 않는다.
- 기능 모듈 두 곳 이상에서 같은 코드가 필요해지면 여기로 옮기는 것을 검토한다. 한 모듈에서만 쓰면 그 모듈에 둔다.

## 의존성 노출

- 런타임 의존성은 `api`로 노출한다. 기능 모듈이 따로 선언하지 않아도 쓸 수 있게 하기 위해서다.
- 실행 시에만 필요한 의존성은 `runtimeOnly`로 둔다 (예: `micrometer-registry-prometheus`). 기능 모듈의 실행 클래스패스에는 들어간다.
- 공통 테스트 의존성(Spring Boot Test, actuator-test, webmvc-test, Testcontainers)은 `testFixturesApi`로 노출한다.
- 공통 테스트 도구 클래스가 생기면 `src/testFixtures/java`에 둔다.
- springdoc은 Boot BOM에 없어서 버전을 직접 지정한다. Boot 버전을 올리면 호환 버전도 같이 확인한다.

## 공통 설정

- 모든 모듈의 기본 설정은 `src/main/resources/lab-core.properties`에 둔다. `LabCoreConfiguration`의 `@PropertySource`로 읽어서, 모듈의 `application.properties`가 같은 키를 덮어쓸 수 있다.
- 지금 들어 있는 것: actuator 노출 범위(`health`, `info`, `prometheus`), 모든 메트릭의 `application` 태그, HTTP 응답 시간 히스토그램.
- `application.properties`는 core에 두지 않는다. 모듈의 `application.properties`와 충돌하기 때문이다.

## 패키지

- `com.spring.lab.core.config`: 공통 `@Configuration`. 기능 모듈은 `@Import(LabCoreConfiguration::class)`로 가져간다.
- `com.spring.lab.core.support`: 공통 예외, 응답 포맷 같은 공통 코드 (필요해지면 만든다).
