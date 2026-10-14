# spring-boot-lab

Spring Boot에서 기술을 하나씩 실습하는 Gradle 멀티모듈 프로젝트다. 모듈 구조, 실행 방법, 새 모듈 추가 절차는 README를 따른다.

@README.md

이 파일에는 모든 모듈에 공통인 규칙만 둔다. 모듈마다 다른 규칙은 그 모듈의 `CLAUDE.md`(예: `lab-redis/CLAUDE.md`)에 두고, 모듈에서 작업할 때는 두 파일을 함께 따른다. 둘이 충돌하면 모듈 `CLAUDE.md`를 따른다.

## 빌드 규칙

- 플러그인 버전은 루트 `build.gradle.kts`에만 선언한다. 모듈은 `kotlin("jvm")`, `id("org.springframework.boot")`처럼 버전 없이 적용한다.
- 의존성 버전은 Spring Boot BOM에 맡긴다. BOM에 없는 라이브러리만 버전을 직접 적고, 이유를 주석으로 남긴다.
- Java toolchain은 Java 25(Temurin)로 고정한다. 모듈에서 따로 바꾸지 않는다.
- Spring Boot 4 기준 starter 이름을 쓴다. 예: `spring-boot-starter-webmvc`, `spring-boot-starter-<기술>-test`.
- Testcontainers는 2.x 아티팩트 이름을 쓴다. 예: `org.testcontainers:testcontainers-junit-jupiter`.

## 모듈 규칙

- 모든 모듈에 공통인 의존성과 설정만 `lab-core`에 둔다. 특정 기술에만 쓰는 것은 그 기능 모듈에 둔다.
- 기능 모듈은 `implementation(project(":lab-core"))`와 `testImplementation(testFixtures(project(":lab-core")))`로 공통 모듈을 가져간다.
- 기능 모듈 이름은 `lab-<기술>`, 패키지는 `com.spring.lab.<기술>`로 한다.
- 기능 모듈의 애플리케이션 클래스는 `@Import(LabCoreConfiguration::class)`로 공통 설정을 가져온다.
- 인프라는 그 모듈 디렉터리의 `compose.yaml`에 둔다. 앱 컨테이너 서비스는 `profiles: ['local-test']`로 분리해서 `bootRun`이 같이 띄우지 않게 한다.
- 모듈을 추가하면 `settings.gradle.kts`, README의 모듈 구조, 그 모듈의 `CLAUDE.md`를 같이 만든다. 모듈을 없애면 셋 다 정리한다.
- 모듈 `CLAUDE.md`에는 그 모듈의 시나리오 목록, 계층을 나누는 기준, 기술 전용 규칙(키·토픽 이름, 테스트 도구 등)을 적는다.

## 패키지 구조

기능 모듈은 먼저 목적(시나리오)별로 패키지를 나누고, 그 안을 MVC 계층으로 나눈다. 계층 패키지는 필요한 것만 만든다.

```
com.spring.lab.<기술>
├── Lab<기술>Application   # 애플리케이션 클래스는 루트 패키지에 둔다
├── config                 # 모듈 전체가 쓰는 @Configuration (필요할 때만)
├── base                   # 모듈을 만들 때 항상 만든다. 그 기술의 기본 명령 실습
│   ├── controller         # @RestController. 요청을 받아 service를 호출한다
│   ├── service            # @Service. 비즈니스 로직, 트랜잭션 경계
│   ├── repository         # 저장소·외부 기술 접근 (MQ 모듈이면 producer/consumer 등 모듈 CLAUDE.md에서 정한다)
│   ├── domain             # 엔티티, 저장 모델, 결과 enum 같은 도메인 타입
│   ├── dto                # 요청/응답 객체
│   └── config             # 이 시나리오 전용 @Configuration
└── <시나리오>              # 실전 상황 하나당 패키지 하나
    └── (base와 같은 계층 구조)
```

- 시나리오는 목적 하나만 다룬다. 다른 목적이 섞이면 시나리오 패키지를 새로 만든다.
- 시나리오끼리 서로의 클래스를 가져다 쓰지 않는다. 같은 코드가 필요하면 각자 둔다. 모듈 전체에 필요한 설정만 루트 `config`에 둔다.
- 계층 안은 역할로 나눈다 (예: 저장소 모듈은 쓰기/읽기, MQ 모듈은 발행/구독). 구체적인 기준은 모듈 `CLAUDE.md`에 정한다.
- controller는 저장소나 외부 기술 API를 직접 쓰지 않고 service를 거친다.
- controller는 `domain` 객체를 그대로 반환하지 않고 `dto`로 바꿔서 반환한다.
- 실패 이유는 `ProblemDetail`(RFC 9457) 본문으로 알려준다.
- URL은 `/<기술>/...`로 시작한다. `base`는 `/<기술>/<리소스>`, 시나리오는 `/<기술>/<시나리오 복수형>/...`로 한다.
- 리소스(스크립트, 스키마 등)는 `src/main/resources/<기술>/<시나리오>/`에 둔다.

### 구현 방식이 여러 개인 시나리오

한 시나리오를 여러 방식으로 구현해 비교할 때는 한 단계 더 나눈다. 방식마다 다른 부분만 방식 패키지에 두고, 나머지는 `common`에 한 번만 둔다.

```
com.spring.lab.<기술>.<시나리오>
├── common        # 모든 방식이 같이 쓰는 API·service·조회·dto·domain, 방식 인터페이스
├── <방식1>        # 그 방식의 구현체와 전용 config만
└── <방식2>
```

- 방식 패키지 이름은 방식을 한 단어로 적는다. 같은 값을 `common/domain`의 방식 enum과 API 파라미터(`?method=<방식>`)에 쓴다.
- 방식 구현체는 `common`의 인터페이스를 구현한 빈으로 두고, `common`의 service가 enum으로 골라 쓴다.
- 방식 패키지끼리는 서로 가져다 쓰지 않는다. `common`만 의존한다.
- 방식 전용 설정과 리소스는 그 방식 패키지의 `config`, `src/main/resources/<기술>/<시나리오>/<방식>/`에 둔다.
- 올바르지 않은 방식도 비교 기준선으로 둘 수 있다. 이름과 주석에 기준선임을 적는다.

## 테스트

테스트는 main과 같은 패키지 구조를 따른다. 시나리오마다, 계층마다, 역할마다 클래스를 나누고, 대상 클래스 하나에 테스트 클래스 하나를 둔다. 전체 흐름 테스트는 시나리오 패키지 바로 아래에 `<시나리오>FlowTest`로 하나 둔다.

| 대상 | 방식 | 인프라 | 검증할 것 |
| --- | --- | --- | --- |
| 인프라 접근 계층 (repository 등) | 기술별 슬라이스 테스트 + `@Import(컨테이너 설정, 대상 클래스)` | Testcontainers | 실제 인프라에 대한 동작 |
| service | 순수 단위 테스트. 의존 대상은 Mockito `mock()` | 없음 | 변환·분기 로직 |
| controller | `@WebMvcTest(대상::class)` + service는 `@MockitoBean` | 없음 | 요청 매핑, JSON 변환, 상태 코드 |
| 전체 흐름 | `@SpringBootTest` + `@AutoConfigureMockMvc` | Testcontainers | 시나리오의 API 흐름이 실제 인프라로 끝까지 이어지는지 |

- 인프라 접근 테스트는 데이터 준비와 결과 확인을 대상 클래스가 아니라 기술의 기본 API(템플릿, 클라이언트)로 직접 한다. 대상 클래스끼리 서로를 검증 도구로 쓰지 않게 하기 위해서다.
- Testcontainers 테스트는 `@BeforeEach`에서 데이터를 비운다.
- 컨테이너는 모듈의 `<기술>TestContainerConfig`(`@TestConfiguration` + `@Bean @ServiceConnection`)에 한 번만 정의하고 `@Import`로 쓴다.
- 컨테이너 이미지는 그 모듈의 `compose.yaml`과 같은 이미지를 쓴다. 로컬에 띄운 compose 컨테이너에 의존하지 않는다.
- 동시성이 핵심인 시나리오(선착순, 재고 차감 등)는 동시 요청 테스트를 둔다. 가상 스레드 executor와 `CountDownLatch`로 요청을 한 번에 출발시키고, 결과 수와 인프라 상태를 함께 확인한다.
- 구현 방식이 여러 개면 모든 방식이 지켜야 할 규칙을 `common` 테스트 패키지에 추상 계약 테스트로 두고, 방식별 테스트가 상속한다. 방식마다 다른 점만 방식별 테스트에 추가한다. 흐름 테스트는 `@ParameterizedTest`로 모든 방식을 같은 시나리오로 돌린다.
- 테스트 이름은 한국어 백틱 함수명으로 동작을 적는다. 예: `` `없는 키를 지우면 false를 돌려준다` ``
- 테스트 실행: `./gradlew :<모듈>:test` (Docker 필요). 일부만: `--tests '*.<시나리오>.*'`, `--tests '*.service.*'`
