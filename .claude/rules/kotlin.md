---
paths:
  - "**/*.kt"
---

# Kotlin 코드 작성 규칙

`lab-redis`의 지금 코드에서 뽑은 작성 방식이다. 계층·패키지 규칙은 루트 `CLAUDE.md`와 모듈 `CLAUDE.md`를 따른다.

## main

- 의존성은 생성자 주입(`private val`)으로 받는다. 파라미터가 여러 줄이면 끝에 쉼표를 둔다.
- controller, repository, 방식 구현체, enum, 키 규칙 객체 위에는 역할과 짝이 되는 클래스를 한국어 주석으로 적는다 (예: `// 쓰기 API. 조회 API는 CouponQueryController에 둔다`). 문장은 `~다`로 끝낸다.
- 주석은 이유와 규칙만 적는다. 코드를 그대로 읽어 주는 주석은 쓰지 않는다.
- 요청·응답 dto는 `data class`, 결과·방식은 `enum class`로 둔다. 문자열 파라미터를 enum으로 바꾸는 함수는 `companion object`의 `from(value): X?`로 둔다.
- "없음"은 예외 대신 `null`이나 결과 enum으로 돌려주고, controller가 상태 코드로 바꾼다 (404, 409 등).
- 실패 응답 본문은 `ProblemDetail.forStatusAndDetail(status, detail)`로 만든다. `detail`은 한국어 한 문장이다.
- 성공한 Command는 본문 없이 상태 코드만 응답한다 (`204`, `201`).
- 일어나면 안 되는 상황(구현체 없음, 알 수 없는 스크립트 반환 코드)은 `IllegalStateException`에 한국어 메시지로 던진다.
- 상수는 `companion object`의 `private const val`로 둔다.
- 저장소 접근 클래스와 방식 구현체는 `@Repository`, service는 `@Service`를 붙인다.

## test

- 단언은 `kotlin.test`(`assertEquals`, `assertTrue`, `assertFalse`)와 JUnit `assertThrows`를 쓴다.
- mock은 `Mockito.mock(X::class.java)`, 동작 지정은 `BDDMockito.given(...).willReturn(...)`, 호출 확인은 `Mockito.verify`를 쓴다. mockito-kotlin은 쓰지 않는다.
- controller·흐름 테스트는 MockMvc Kotlin DSL(`mockMvc.get(...) { }.andExpect { status { ... }; jsonPath(...) { value(...) } }`)을 쓴다.
- Spring 빈은 생성자에 `@Autowired private val`로 받는다. 상속용 추상 테스트의 `@Autowired protected lateinit var`와 `@MockitoBean private lateinit var` 필드만 예외다.
- service 테스트의 `MeterRegistry`는 `SimpleMeterRegistry()`로 넣고 메트릭 이름·태그·count를 확인한다.
- 테스트 클래스 위에 무엇을 띄우고 무엇을 검증하는지 한국어 주석 한 줄을 적는다.
