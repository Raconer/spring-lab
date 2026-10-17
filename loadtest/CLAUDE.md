# loadtest

nGrinder로 대상 모듈의 시나리오 API에 부하를 준다. Gradle 모듈이 아니다. 루트 `CLAUDE.md`의 공통 규칙을 함께 따른다.

## 구조

```
loadtest
├── compose.yaml                   # loadtest 그룹: nGrinder controller(loadtest.controller), agent(loadtest.agent)
└── scripts
    └── (기술)
        └── (시나리오).groovy       # 대상 모듈·시나리오와 같은 이름. 예: redis/couponfirstcome.groovy
```

- 이미지는 `ngrinder/controller`, `ngrinder/agent`를 같은 버전으로 고정한다. amd64 이미지만 있어서 `platform: linux/amd64`로 띄운다.
- controller 웹 화면은 `http://localhost:8090`이다 (기본 계정 `admin` / `admin`). agent는 네트워크 별칭 `controller`로 controller를 찾는다.
- 스크립트의 원본은 이 폴더다. controller 화면의 `Script`에 같은 경로(`(기술)/(시나리오).groovy`)로 올려서 쓴다. controller에서 고치면 이 파일에도 반영한다.

## 스크립트 규칙

- nGrinder Groovy(JUnit) 스크립트로 쓴다. `GTest` 이름은 `(기술).(시나리오)`로 한다.
- 대상 주소는 스크립트의 `TARGET`에 compose 서비스 이름으로 적는다 (예: `http://lab-redis:8080`).
- 방식과 값은 테스트 설정의 `Parameter`(상세 설정 보기 안)에 `key.value`를 쉼표로 이어 받는다 (예: `method.lua,quantity.20`). nGrinder는 영문·숫자·`.` `,` `|` `_`만 50자까지 허용해서 `=` 대신 `.`을 쓴다. 읽을 때는 `System.getProperty("param", "")`를 쓴다.
- 없는 key는 스크립트의 `DEFAULTS` 값을 쓴다. 모르는 key나 값은 `@BeforeProcess`에서 `assert`로 막아 테스트가 시작되지 않게 한다.
- `@BeforeProcess`: 실행마다 새 테스트 데이터를 만든다. 이름에 시작 시각을 넣어 이전 실행과 섞이지 않게 한다 (예: `load-(시각)`).
- `@Test`: 요청 하나를 보낸다. 시나리오에서 정상인 응답(예: 201, 409)만 통과시키고 나머지는 `assert`로 실패시킨다. nGrinder는 실패를 에러 수로 센다.
- `@AfterProcess`: 부하 후 대상 상태를 조회해서 시나리오 규칙을 확인하고 `[검증 통과]`, `[검증 실패]` 로그로 남긴다.
- 등록·검증이 프로세스마다 돌기 때문에 agent 1개, 프로세스 1개로 실행한다. 가상 사용자 수는 스레드 수로 정한다.
- 스크립트 맨 위 주석에 Parameter key, 고를 수 있는 값과 기본값, 실행 조건을 적는다. 대상 시나리오 README의 `부하 테스트` 섹션에도 같은 내용을 적는다.

## 결과 보기

- nGrinder 테스트 결과(TPS, 평균 응답 시간, 에러 수)는 controller 화면의 테스트 상세에서 본다. 검증 로그는 테스트 상세의 로그에서 본다.
- 서버 쪽 지표는 Grafana 시나리오 대시보드에서 본다. nGrinder는 Prometheus로 지표를 내보내지 않으므로, 대시보드에는 대상 앱의 `http_server_requests_seconds_*`(시나리오 API의 `uri`)를 넣는다.

## 확인

- 테스트 코드는 없다. 대상 모듈 그룹(예: `lab-redis`)과 `loadtest` 그룹을 띄우고 nGrinder에서 스크립트 검증(`Validate`)과 테스트를 돌려서 확인한다.
