# HANDOVER — 일어나세요용사여 (교대근무자용 알람 앱)

> 마지막 업데이트: 2026-11-22
> 새 대화를 시작하면 이 파일과 `alarm_app_plan.md`를 먼저 읽고 이어서 진행한다.

---

## 1. 완료된 작업

| 커밋 | 내용 |
|---|---|
| `af5f758` | IntelliJ 기본 프로젝트 → **Gradle(Kotlin DSL)** 전환, kotlin.test 환경, 기획서 v0.3, 데이터 모델, 알람 계산 테스트 작성 (TDD) |
| `e6efc61` | 알람 계산 기능 구현 — **테스트 19개 전부 통과** |

- 기획서 v0.3 반영 내용: 알람 규칙 검증, "다음 알람 하나만 예약"(도미노 방식), 한 달 기준 탐색, `isOff` 계산 속성화, 사용 중 근무 유형 삭제 불가
- 앱 가제: **일어나세요용사여** (구 "깨워줘")

---

## 2. 확정된 설계 및 기술 규칙

### 기술 환경
| 항목 | 값 |
|---|---|
| 빌드 | Gradle 9.9.2 (래퍼 `gradlew` 사용, 별도 설치 불필요) |
| Kotlin | 2.4.21 (`kotlin("jvm")`), `application` 플러그인, mainClass = `MainKt` |
| JDK | 실행은 설치된 JDK 26, **결과물은 Java 17 기준** (Android 이전 대비) |
| 테스트 | kotlin.test + JUnit 5 (`./gradlew test`) |
| 패키지 | 아직 없음 (기본 패키지). `group = "com.wakeme"`은 예전 이름 그대로 |

### 핵심 설계 규칙
- **휴무 판단**: `ShiftType.isOff = startTime == null` (따로 저장하지 않음)
- **알람 규칙 검증**: 모든 값 ≥ 1, 그리고 `intervalMinutes × (repeatCount - 1) < minutesBefore`
  → 마지막 알람이 출근 시간 **정각이어도 실패**
- **하루치 알람**: `날짜.atTime(출근시간) - minutesBefore` 부터 `interval` 간격으로 `repeatCount`번
  → 날짜+시각을 합쳐 계산하므로 자정 넘어 **전날로 가는 경우 자동 처리**
- **다음 알람**: `now < 알람 ≤ now.plusMonths(1)` 중 가장 빠른 것 1개, 없으면 `null`
  - "한 달" = 달력 기준 (`plusMonths(1)`), 30일 아님
  - `now`와 같은 시각은 제외 (방금 울린 알람 다음 것을 예약하기 위해)
- **근무 유형/규칙을 못 찾으면** 그날은 알람 없음 (에러 X) — 기획서엔 없던 규칙, 바뀔 수 있음
- **알람 예약 방식(안드로이드 단계)**: 다음 알람 1개만 AlarmManager에 예약, 울리면 다음 것 예약

### 작업 규칙 (CLAUDE.md 요약)
- 모호하면 먼저 질문, 새 기능은 설계안 3개 비교 후 사람이 선택
- **TDD**: 테스트 먼저 → 실패 확인 → 구현 → 통과까지 반복
- 기능 하나 완성마다 사용자 확인 후 커밋 (git 작성자: 일어나세요용사여, 저장소 로컬 설정)
- 답변은 한국어로, 어려운 개념은 쉽게

---

## 3. 현재 진행 상태 및 남은 작업

**현재 위치: 개발 일정 1단계(콘솔) — 3~4주차 핵심 계산 로직 완료**

### 남은 작업 (1단계: 콘솔)
- [ ] **3주차 남은 것**: 콘솔에서 근무표 입력·출력 (`Main.kt` — 지금은 IntelliJ 기본 Hello 예제 그대로)
- [ ] **5주차**: 패턴 자동 채우기 (`D D E E N N OFF OFF` 반복 + 시작 날짜) — 테스트 먼저
- [ ] (선택) "이번만 끄기"(S7) 토글 함수
- [ ] (선택) 사용 중인 근무 유형 삭제 불가 검사 함수

### 이후 (2단계: Android Studio, 6~10주차)
- Jetpack Compose 화면, Room 저장, AlarmManager(정확한 알람 권한), 재부팅 대응

### 미정 사항
- `group = "com.wakeme"` → 영어 앱 이름 정해지면 변경
- 근무 유형/규칙 누락 시 처리 방식 (현재: 조용히 알람 없음)

---

## 4. 핵심 파일 및 주의사항

| 파일 | 역할 |
|---|---|
| `alarm_app_plan.md` | 기획서 v0.3 (요구사항의 기준) |
| `src/main/kotlin/Models.kt` | `ShiftType`, `AlarmRule`, `WorkDay` |
| `src/main/kotlin/AlarmCalculator.kt` | `isValid()`, `alarmTimesFor()`, `nextAlarm()` |
| `src/main/kotlin/Main.kt` | 콘솔 진입점 (아직 예제 코드) |
| `src/test/kotlin/AlarmCalculatorTest.kt` | 알람 계산 테스트 18개 |
| `src/test/kotlin/SetupTest.kt` | 환경 확인용 테스트 (지워도 됨) |
| `build.gradle.kts` | 빌드 설정 |

### 주의사항
- **실행 명령**: 테스트 `./gradlew test`, 실행 `./gradlew run`
- 테스트 이름은 한글 백틱 함수명 사용 — JVM 제약상 `. ; [ ] / < > :` 문자는 쓰면 안 됨
- IntelliJ에서 Gradle 프로젝트로 다시 불러와야 함 ("Load Gradle Project" / `build.gradle.kts` 우클릭 → Link Gradle Project)
- 예전 IntelliJ 설정 파일(`AIram.iml` 등)은 프로젝트에서 제거됨 (임시 폴더로 옮겨 둠, 재부팅 시 사라질 수 있음 — 필요 없음)
- PC에 JDK 17은 없음 (JDK 25, 26 설치됨) → `jvmToolchain(17)` 쓰면 빌드 실패하니 현재 방식 유지
- PowerShell/Bash에서 `python` 명령은 Windows 스토어 스텁이라 멈출 수 있음 — 쓰지 말 것
