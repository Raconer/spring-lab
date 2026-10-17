---
name: new-module
description: 새 기능 모듈(lab-kafka, lab-mysql 같은 lab-(기술))을 추가할 때 쓴다. Gradle 설정, compose, Pinpoint, IntelliJ 실행 설정, Prometheus, 문서, base 시나리오까지 만든다. "모듈 추가", "lab-xxx 만들어", "(기술) 실습 모듈 시작" 같은 요청에 쓴다.
---

# 기능 모듈 추가

규칙은 루트 `CLAUDE.md`, 절차의 뼈대는 루트 `README.md`의 `새 기능 모듈 추가`에 있다. 이 스킬은 빠뜨리기 쉬운 파일까지 순서대로 정한다. 기준 예시는 `lab-redis`다.

## 0. 먼저 정할 것

요청에 없으면 한 번에 묻는다.

- 기술 이름 (모듈 `lab-(기술)`, 패키지 `com.spring.lab.(기술)`, URL `/(기술)/...`, 컨테이너 `(기술).(역할)`에 같이 쓴다)
- 언어 (Kotlin 또는 Java)
- 인프라 이미지와 버전, 호스트 포트. `.claude/rules/compose.md`의 "지금 쓰는 포트"와 겹치지 않는지 확인한다
- exporter 이미지와 버전 (있으면)

## 1. 빌드

- `settings.gradle.kts`의 `include(...)`에 추가
- `lab-(기술)/build.gradle.kts`: README 예시대로. BOM에 없는 의존성만 버전과 이유 주석
- `src/main/resources/application.properties`: `spring.application.name=lab-(기술)`
- 애플리케이션 클래스 `Lab(기술)Application` + `@Import(LabCoreConfiguration::class)`

## 2. 컨테이너와 실행 설정

`lab-redis`의 같은 파일을 복사해서 이름만 바꾼다.

- `lab-(기술)/compose.yaml`: `name: lab-(기술)`, 인프라(프로필 없음), 앱·exporter(`profiles: ['local-test']`), `networks.default.name: spring-boot-lab`
- `lab-(기술)/compose.pinpoint.yaml`: `agent.yaml` extends, agentId·applicationName은 `lab-(기술)`
- `.run/lab-(기술).run.xml`, `.run/lab-(기술)-all.run.xml`: `--build`, `upDetach`, `upForceRecreate`, `upRemoveOrphans`, `local-test`, `secondarySourceFiles`. all은 Before launch에 `monitoring`, `pinpoint`
- `monitoring/prometheus/prometheus.yml`: job `lab-(기술)`, 앱(`role: app`)과 exporter(`role: exporter`)

## 3. 테스트 기반

- `src/test/.../(기술)TestContainerConfig`: `compose.yaml`과 같은 이미지, `@ServiceConnection`

## 4. base 시나리오

`new-scenario` 스킬로 `base` 시나리오(그 기술의 기본 명령)를 만든다. 부하 스크립트와 시나리오 대시보드는 base에서는 만들지 않아도 된다.

## 5. 문서

- `lab-(기술)/CLAUDE.md`: `lab-redis/CLAUDE.md`와 같은 섹션(시나리오, 계층 나누기, 기술 규칙, 부하 테스트와 모니터링, 테스트)
- 루트 `README.md`: 모듈 구조 트리, 패키지 구조, API 표, 실행 그룹 표, 확인할 주소
- `.claude/rules/compose.md`: 새 호스트 포트를 "지금 쓰는 포트"에 추가
- `.claude/skills/scenario-readme/SKILL.md`: 팔레트에 없는 서버면 `classDef`를 팔레트 표에 추가
- `progress.md`: 모듈 행, 이력

## 6. 확인

1. `./gradlew :lab-(기술):test`
2. `docker compose -f lab-(기술)/compose.yaml -f lab-(기술)/compose.pinpoint.yaml --profile local-test config -q`
3. `.claude/rules/compose.md`의 포트 목록과 새 compose의 `ports`가 겹치지 않는지

끝나면 만든·고친 파일 목록과 확인 결과를 보고한다. 커밋은 하지 않는다.
