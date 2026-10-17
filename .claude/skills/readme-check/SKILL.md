---
name: readme-check
description: 시나리오·방식 README(src/main/kotlin/**/README.md)가 scenario-readme 스킬의 README 양식을 지키는지, 내용이 실제 코드·설정과 맞는지 점검하고 보고한다. mermaid 점검은 diagramReviewer 에이전트에 맡긴다. "README 점검", "readme-check", "README 양식 맞는지 봐줘" 같은 요청이나 README를 만들거나 고친 뒤에 쓴다.
---

# README 점검

기준은 `.claude/skills/scenario-readme/SKILL.md`의 `## README 양식`이다. 점검 전에 그 파일을 다시 읽는다. 점검만 하고 고치지 않는다. 고치라고 하면 그때 고친다.

## 대상

- 경로를 받으면 그 README만 본다.
- 경로가 없으면 `git status --porcelain`에서 바뀐 `src/main/kotlin/**/README.md`를 본다. 바뀐 것이 없으면 모든 시나리오 README를 본다.

## 1. 양식

- 제목: `# (시나리오 또는 방식) — (한 줄 제목)`, 바로 아래 한 줄 요약
- 섹션 순서와 제목이 양식과 같은지. 해당 없는 섹션을 뺀 것은 괜찮다
- 양식에 없는 섹션은 보고한다. 방식이 여러 개인 시나리오 README의 `## 방식 비교`는 scenario-readme 스킬이 시나리오 README에 맡긴 내용이라 허용한다
- `## 2. 흐름도`의 첫 문장이 양식 문구(서버별 색 설명)와 같은지. API가 여러 개면 `### (API 이름)`으로 나눴는지

## 2. 실제와 맞는지

각 항목을 실제 파일로 확인한다. 기억이나 README 다른 부분으로 확인하지 않는다.

| README 내용 | 확인할 곳 |
| --- | --- |
| 데이터 구조의 키·타입 | 시나리오의 키 규칙 객체 (예: `CouponRedisKeys.kt`) |
| 사용 기술의 명령 | repository·방식 구현체, Lua 스크립트 |
| 부하 테스트의 스크립트 경로, Parameter key·값·기본값 | `loadtest/scripts/(기술)/(시나리오).groovy`의 맨 위 주석과 `DEFAULTS` |
| 모니터링의 대시보드 이름, 메트릭 이름 | `monitoring/grafana/dashboards/(기술)/(시나리오).json`의 `title`, service의 `Timer.builder(...)` |
| 모니터링의 Pinpoint application 이름 | 모듈 `compose.pinpoint.yaml`의 `applicationName` |
| 검증하는 테스트의 클래스 | `src/test/kotlin/...`에 그 클래스가 있는지 |
| API 경로, 상태 코드 | controller의 매핑과 응답 |

## 3. 다이어그램

`Agent` 도구로 `diagramReviewer` 에이전트를 부르고 점검할 README 경로를 넘긴다. 결과는 그대로 합친다. flowchart가 꺾인선(`"curve": "step"`)이 아니면 무조건 HIGH다.

## 보고

```
FILE: <README 경로>
- [HIGH|MEDIUM|LOW] <문제> | EVIDENCE: <파일:줄> | FIX: <바꿀 내용>
```

문제가 없으면 `문제 없음`. 끝에 파일별 결과를 한 줄씩 요약한다.
