# Spring AI Tool Calling 실습

- `starter`: 수강생용 시작 코드. TODO 1~3 구현 대상
- `solution`: 강사용 예시 풀이. 동일한 API·설정·시간 변환 로직 사용

Java 21 · Spring Boot 3.5.16 · Spring AI 1.1.8 · Gemini 사용. `127.0.0.1`에서 실행하는 로컬 실습용 API.

## 설정 및 실행

1. 루트의 `application-local.yaml.example`을 `application-local.yaml`로 복사하고 Gemini API 키 입력. 실제 키 파일은 Git·JAR 제외 대상
2. 시작 코드는 TODO 구현 후 실행. 두 모듈은 같은 포트를 사용하므로 하나씩 실행

```bash
./gradlew :starter:bootRun
```

풀이 확인 시 `./gradlew :solution:bootRun` 사용. IntelliJ에서는 실행할 모듈의 `ChatApplication`을 선택하고 Working directory를 프로젝트 루트로 설정.

시작 코드는 미완성 상태에서도 컴파일할 수 있지만, TODO 1의 ChatClient Bean을 구현하기 전에는 서버가 시작되지 않음. `ChatService.answer()`도 TODO 3 구현 필요.

## 요청 및 확인

```bash
curl -sS http://127.0.0.1:8080/api/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"2026년 7월 15일 서울 오후 3시는 뉴욕 몇 시야?"}'
```

- 정상 변환: 뉴욕의 **2026년 7월 15일 오전 2시** 응답. `[TOOL CALL]`의 Arguments와 `[TOOL RESULT]` 확인
- “안녕!”: 일반 응답, Tool 실행 없음
- “오후 3시는 뉴욕 몇 시야?”: 누락한 출발 지역·날짜 확인, Tool 실행 없음

요청 예시: [requests.http](requests.http). 모델 응답의 문구·도구 선택은 달라질 수 있음. 빈 질문과 2,000자를 넘는 질문은 HTTP 400으로 거절.

## 설정과 검증

각 모듈의 `src/main/resources/application.yaml`에서 모델·system prompt·로그 설정. API 키는 로컬 설정 파일 또는 `GEMINI_API_KEY`로 전달.

기본 INFO에서 HTTP 요청 → 도구 호출 → 도구 결과 → HTTP 응답 출력. 전체 대화 메시지·헤더는 출력하지 않음. `chat.trace-model: false`이면 도구 로그 비활성화.

```bash
./gradlew build
```

테스트는 `solution`에만 포함. 시간 변환·입력 검증·로그·도구 실행 흐름을 검증하며, 모델 연동 테스트는 외부 API 키 없이 로컬 모의 Gemini를 사용. 실제 모델의 도구 선택 판단은 API로 별도 확인 필요.

[교재·실습 안내](https://app.notion.com/p/padoling/Function-Calling-Tool-Calling-41f81b1e2cc480bebf1ec8d74b652c4c)
