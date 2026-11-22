# HANDOVER — 일어나세요용사여 (교대근무자용 알람 앱)

> 마지막 업데이트: 2026-11-22
> 새 대화를 시작하면 이 파일과 `alarm_app_plan.md`를 먼저 읽고 이어서 진행한다.

---

## 1. 완료된 작업

| 커밋 | 내용 |
|---|---|
| `af5f758` | IntelliJ 기본 프로젝트 → **Gradle(Kotlin DSL)** 전환, kotlin.test 환경, 기획서 v0.3, 데이터 모델, 알람 계산 테스트 작성 (TDD) |
| `e6efc61` | 알람 계산 기능 구현 — **테스트 19개 전부 통과** |
| `a05a872` | HANDOVER.md 추가 |
| `cc175ce` | **콘솔 근무표 입력·출력**: `schedule.txt` 읽기 → 달력 출력 → 다음 알람 — **테스트 30개 전부 통과** |
| `5ec51c8` | **패턴 자동 채우기** (`PATTERN` 줄) + 프로젝트 `CLAUDE.md`(커밋 후 바로 push 규칙) — **테스트 39개 전부 통과** |
| `afa87a8` | **이번만 끄기(S7)** — `SKIP 날짜` 줄, 달력에 `*` 표시 — **테스트 48개 전부 통과** |
| (이 커밋) | **사용 중 근무 유형 삭제 막기** — `deleteShiftType()` + sealed class 결과, 진행 방식 규칙(자동 커밋·푸시, 분기점은 추천안) — **테스트 54개 전부 통과** |

- 콘솔 입출력 설계: 설계안 3개(한 줄 입력 / 메뉴형 / 파일) 중 **파일 입력 + 달력형 출력** 선택
- 기획서에 **S8 근무표 사진으로 입력** 추가 (Android 단계에서 설계안 비교 후 진행)

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
- **근무 유형 삭제**: 근무표에서 한 번이라도 쓰였으면(지난 날짜·알람 끈 날 포함) 삭제 불가 → `InUse(쓰는 날짜들)`. 삭제되면 그 유형의 알람 규칙도 함께 삭제

### 작업 규칙 (자세한 건 프로젝트 `CLAUDE.md`)
- **TDD**: 테스트 먼저 → 실패 확인 → 구현 → 통과까지 반복
- **단계가 끝나면 묻지 않고 커밋 + push** (git 작성자: 일어나세요용사여, 저장소 로컬 설정, 원격 origin/main = github.com/csw94/AIram)
- **분기점은 설계안 3개 비교 후 추천안으로 알아서 진행**, 보고 때 이유를 남김
- 문제·되돌리기 어려운 작업·새 프로그램 설치·계정 로그인이 필요할 때만 멈추고 질문
- 답변은 한국어로, 어려운 개념은 쉽게

---

## 3. 현재 진행 상태 및 남은 작업

**현재 위치: 1단계(콘솔) 필수·선택 작업 모두 완료 → 다음은 2단계(Android Studio)**

- 2단계 시작 전 필요: **Android Studio 설치**(사용자 직접), 새 Android 프로젝트로 옮길지 / 이 저장소에 모듈로 추가할지 결정

### 남은 작업 (1단계: 콘솔)
- [x] **3주차**: 콘솔 근무표 입력(`schedule.txt`)·달력 출력·다음 알람 표시 완료
- [x] **5주차**: 패턴 자동 채우기 — `PATTERN 시작일 끝날 근무...` 줄, 날짜 줄이 패턴보다 우선, 패턴끼리 겹치면 아래 줄 우선
- [x] **이번만 끄기(S7)**: `SKIP 날짜` 줄 (근무는 그대로, 알람만 끔), 근무 없는 날·OFF는 오류, 달력에 `E*` + 설명 줄
- [x] **사용 중 근무 유형 삭제 막기**: `deleteShiftType()` → `Deleted` / `InUse(날짜들)` / `NotFound` (콘솔엔 삭제 UI가 없어 함수+테스트만, Android 설정 화면에서 사용 예정)

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
| `CLAUDE.md` | 프로젝트 규칙 (Claude가 자동으로 읽음): 시작 시 HANDOVER·기획서 읽기, 단계 끝나면 자동 커밋·push, 분기점은 추천안으로 진행 |
| `src/main/kotlin/Models.kt` | `ShiftType`, `AlarmRule`, `WorkDay` |
| `src/main/kotlin/AlarmCalculator.kt` | `isValid()`, `alarmTimesFor()`, `nextAlarm()` |
| `src/main/kotlin/Main.kt` | 콘솔 진입점: 파일 읽기 → 달력 출력 → 다음 알람 |
| `src/main/kotlin/DefaultShifts.kt` | 기본 근무 유형(D 07:00, E 15:00, N 23:00, OFF) + 기본 알람 규칙(90분 전·10분 간격·3번) |
| `src/main/kotlin/ScheduleParser.kt` | `parseSchedule()`: 날짜 줄 + PATTERN 줄 + SKIP 줄 → WorkDay(날짜순), 잘못된 줄은 "n번째 줄: 이유" |
| `src/main/kotlin/PatternFill.kt` | `fillPattern()`: 패턴을 시작일~끝날까지 반복해서 펼치기 |
| `src/main/kotlin/SkipAlarm.kt` | `skipAlarm()`: 특정 날짜 알람만 끈 근무표 복사본 (Android "이번만 끄기" 버튼에서 재사용 예정) |
| `src/main/kotlin/DeleteShiftType.kt` | `deleteShiftType()` + `DeleteShiftResult`(sealed class: Deleted / InUse / NotFound) |
| `src/main/kotlin/ConsoleFormat.kt` | `formatCalendar()`(일요일 시작, 한 칸 4글자, 근무 없는 날 "-"), `nextAlarmMessage()` |
| `schedule.txt` | 예시 근무표 (12월 전체 PATTERN + 12/25 OFF + SKIP 12/01) |
| `src/test/kotlin/*Test.kt` | 테스트: 알람 계산 18 + 파일 읽기 7 + 출력 4 + 환경 1 + 패턴 9 + 이번만 끄기 9 + 유형 삭제 6 = 54개 |
| `src/test/kotlin/SetupTest.kt` | 환경 확인용 테스트 (지워도 됨) |
| `build.gradle.kts` | 빌드 설정 |

### 주의사항
- **실행 명령**: 테스트 `./gradlew test`, 실행 `./gradlew run` (다른 파일: `./gradlew run --args="파일.txt"`)
- 윈도우 콘솔 한글 깨짐 방지로 `Main.kt`에서 출력을 UTF-8로 설정함
- 테스트 이름은 한글 백틱 함수명 사용 — JVM 제약상 `. ; [ ] / < > :` 문자는 쓰면 안 됨
- IntelliJ에서 Gradle 프로젝트로 다시 불러와야 함 ("Load Gradle Project" / `build.gradle.kts` 우클릭 → Link Gradle Project)
- 예전 IntelliJ 설정 파일(`AIram.iml` 등)은 프로젝트에서 제거됨 (임시 폴더로 옮겨 둠, 재부팅 시 사라질 수 있음 — 필요 없음)
- PC에 JDK 17은 없음 (JDK 25, 26 설치됨) → `jvmToolchain(17)` 쓰면 빌드 실패하니 현재 방식 유지
- PowerShell/Bash에서 `python` 명령은 Windows 스토어 스텁이라 멈출 수 있음 — 쓰지 말 것
