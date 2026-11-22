# HANDOVER — 일어나세요용사여 (교대근무자용 알람 앱)

> 마지막 업데이트: 2026-11-22
> 새 대화를 시작하면 이 파일과 `alarm_app_plan.md`를 먼저 읽고 이어서 진행한다.

---

## 1. 완료된 작업

| 커밋 | 내용 |
|---|---|
| `af5f758` | IntelliJ 기본 프로젝트 → **Gradle(Kotlin DSL)** 전환, kotlin.test 환경, 기획서 v0.3, 데이터 모델, 알람 계산 테스트 작성 (TDD) |
| `e6efc61` | 알람 계산 기능 구현 — 테스트 19개 통과 |
| `a05a872` | HANDOVER.md 추가 |
| `cc175ce` | **콘솔 근무표 입력·출력**: `schedule.txt` 읽기 → 달력 출력 → 다음 알람 — 테스트 30개 통과 |
| `5ec51c8` | **패턴 자동 채우기** (`PATTERN` 줄) + 프로젝트 `CLAUDE.md` — 테스트 39개 통과 |
| `afa87a8` | **이번만 끄기(S7)** — `SKIP 날짜` 줄, 달력에 `*` 표시 — 테스트 48개 통과 |
| `283edff` | **사용 중 근무 유형 삭제 막기** — `deleteShiftType()` + sealed class 결과, 진행 방식 규칙 — 테스트 54개 통과 |
| (이 커밋) | **2단계 시작: `core` + `app` 멀티 모듈 전환**, Android 앱 홈 화면(오늘 근무 + 다음 알람) — core 테스트 54개 통과, 디버그 APK 빌드 성공 |

- 콘솔 입출력 설계: 파일 입력 + 달력형 출력
- 기획서에 **S8 근무표 사진으로 입력** 추가 (Android 단계에서 설계안 비교 후 진행)
- 기획서 v0.3 반영 내용: 알람 규칙 검증, "다음 알람 하나만 예약"(도미노 방식), 한 달 기준 탐색, `isOff` 계산 속성화, 사용 중 근무 유형 삭제 불가
- 앱 가제: **일어나세요용사여** (구 "깨워줘"), 패키지: `com.wakeuphero`

---

## 2. 확정된 설계 및 기술 규칙

### 프로젝트 구조 (멀티 모듈)
```
AIram/
├─ core/   순수 Kotlin (Android 몰라도 됨): 알람 계산·근무표 로직 + 콘솔 프로그램 + 테스트
│          패키지 com.wakeuphero.core
└─ app/    Android 앱 (Jetpack Compose), core를 가져다 씀
           패키지 com.wakeuphero.app
```
- 분기점 기록: A 멀티 모듈 / B 새 Android 프로젝트에 복사 / C 프로젝트를 Android 전용으로 변환 → **A 선택** (로직·테스트 재사용, 코드 중복 없음, 콘솔도 계속 사용 가능)
- **원칙: 계산·규칙은 core에 TDD로, app은 화면·저장·알람 예약만**

### 기술 환경
| 항목 | 값 |
|---|---|
| 빌드 | Gradle 9.9.2 (래퍼 `gradlew`) |
| Kotlin | 2.4.21 |
| Android Gradle Plugin | 9.5.2 (AGP 9부터 Kotlin 지원 기본 포함 → `kotlin-android` 플러그인 불필요) |
| Compose | BOM 2026.11.01, Material3, activity-compose 1.13.0 |
| SDK | compileSdk **37.1** (`release(37) { minorApiLevel = 1 }`, Compose가 요구), targetSdk 37, minSdk 26 |
| JDK | 빌드는 설치된 JDK 26, **결과물은 Java 17 기준** (core·app 모두) |
| 테스트 | kotlin.test + JUnit 5 (core) |
| Android SDK | `C:/Users/dongb/AppData/Local/Android/Sdk` (`local.properties`, git 제외) |

### 핵심 설계 규칙
- **휴무 판단**: `ShiftType.isOff = startTime == null` (따로 저장하지 않음)
- **알람 규칙 검증**: 모든 값 ≥ 1, 그리고 `intervalMinutes × (repeatCount - 1) < minutesBefore`
  → 마지막 알람이 출근 시간 **정각이어도 실패**
- **하루치 알람**: `날짜.atTime(출근시간) - minutesBefore` 부터 `interval` 간격으로 `repeatCount`번
  → 날짜+시각을 합쳐 계산하므로 자정 넘어 **전날로 가는 경우 자동 처리**
- **다음 알람**: `now < 알람 ≤ now.plusMonths(1)` 중 가장 빠른 것 1개, 없으면 `null`
  - "한 달" = 달력 기준 (`plusMonths(1)`), `now`와 같은 시각은 제외
- **근무 유형/규칙을 못 찾으면** 그날은 알람 없음 (에러 X)
- **알람 예약 방식(Android)**: 다음 알람 1개만 AlarmManager에 예약, 울리면 다음 것 예약
- **근무 유형 삭제**: 근무표에서 한 번이라도 쓰였으면(지난 날짜·알람 끈 날 포함) 삭제 불가 → `InUse(쓰는 날짜들)`. 삭제되면 알람 규칙도 함께 삭제
- **근무표 파일 문법** (`schedule.txt`): `날짜 근무` / `PATTERN 시작일 끝날 근무...` / `SKIP 날짜` / `#` 주석
  - 우선순위: 날짜 줄 > 패턴(겹치면 아래 줄), SKIP은 마지막에 적용, 오류는 줄 번호 순

### 작업 규칙 (자세한 건 프로젝트 `CLAUDE.md`)
- **TDD**: 테스트 먼저 → 실패 확인 → 구현 → 통과까지 반복
- **단계가 끝나면 묻지 않고 커밋 + push** (git 작성자: 일어나세요용사여, 원격 origin/main = github.com/csw94/AIram)
- **분기점은 설계안 3개 비교 후 추천안으로 알아서 진행**, 보고 때 이유를 남김
- 문제·되돌리기 어려운 작업·새 프로그램 설치·계정 로그인이 필요할 때만 멈추고 질문
- 답변은 한국어로, 어려운 개념은 쉽게

---

## 3. 현재 진행 상태 및 남은 작업

**현재 위치: 2단계(Android) 1번째 마일스톤 완료 — 앱이 빌드되고 홈 화면에 core 계산 결과가 나옴 (아직 예시 패턴, 저장·알람 없음)**

### 1단계: 콘솔 ✅ 전부 완료
- 콘솔 입출력, 패턴 자동 채우기, 이번만 끄기, 근무 유형 삭제 막기

### 2단계: Android (마일스톤 순서)
- [x] **M1** 멀티 모듈 전환 + 홈 화면 (오늘 근무, 다음 알람) — 예시 패턴 하드코딩
- [ ] **M2** 근무표 달력 화면 (월 이동, 날짜 눌러 근무 바꾸기, 패턴 채우기, 이번만 끄기)
- [ ] **M3** Room으로 근무표·근무 유형·알람 규칙 저장
- [ ] **M4** 근무 유형 설정 화면 (+ 알람 규칙, 미리보기, 검증, 삭제 막기)
- [ ] **M5** AlarmManager로 다음 알람 예약 + 알람 울림 화면(소리·진동) + 정확한 알람 권한 + 재부팅 재예약
- [ ] **M6** 일반 알람(F1~F6), 추가 기능, 실기기 테스트, 회고
- [ ] (추가) S8 근무표 사진 입력 — 설계안 비교 후

### 미정 사항
- 근무 유형/규칙 누락 시 처리 방식 (현재: 조용히 알람 없음)
- 앱 아이콘 (현재 기본값)

---

## 4. 핵심 파일 및 주의사항

| 파일 | 역할 |
|---|---|
| `alarm_app_plan.md` | 기획서 v0.3 (요구사항의 기준) |
| `CLAUDE.md` | 프로젝트 규칙 (Claude가 자동으로 읽음) |
| `settings.gradle.kts` | 모듈 목록(`:core`, `:app`), 저장소(google, mavenCentral) |
| `build.gradle.kts` | 루트: 플러그인 버전만 (`apply false`) |
| `core/build.gradle.kts` | core 빌드 (kotlin jvm + application, 실행 폴더 = 루트) |
| `app/build.gradle.kts` | app 빌드 (AGP, Compose, `implementation(project(":core"))`) |
| `core/src/main/kotlin/com/wakeuphero/core/` | `Models` `AlarmCalculator` `DefaultShifts` `ScheduleParser` `PatternFill` `SkipAlarm` `DeleteShiftType` `ConsoleFormat` `Main`(콘솔) |
| `core/src/test/kotlin/com/wakeuphero/core/` | 테스트 54개 (알람 계산 18, 파일 읽기 7, 출력 4, 환경 1, 패턴 9, 이번만 끄기 9, 유형 삭제 6) |
| `app/src/main/kotlin/com/wakeuphero/app/MainActivity.kt` | 홈 화면 `HomeScreen` (+ Preview) |
| `app/src/main/AndroidManifest.xml`, `res/values/strings.xml` | 앱 이름 "일어나세요용사여" |
| `schedule.txt` | 콘솔용 예시 근무표 |

### 명령어
- core 테스트: `./gradlew :core:test`
- 콘솔 실행: `./gradlew :core:run` (다른 파일: `./gradlew :core:run --args="파일.txt"`)
- 앱 빌드: `./gradlew :app:assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`
- 앱 실행: Android Studio에서 프로젝트 열기 → 에뮬레이터/폰 선택 → ▶ Run

### 주의사항
- `local.properties`의 SDK 경로는 **슬래시(/)** 로 쓸 것 (역슬래시는 이스케이프 문제로 빌드 실패했음)
- 윈도우 콘솔 한글 깨짐 방지로 `Main.kt`에서 출력을 UTF-8로 설정함
- 테스트 이름은 한글 백틱 함수명 사용 — JVM 제약상 `. ; [ ] / < > :` 문자는 쓰면 안 됨
- PC에 JDK 17은 없음 (JDK 25, 26 + Android Studio 내장 JBR) → `jvmToolchain(17)` 쓰면 빌드 실패하니 현재 방식 유지
- PowerShell/Bash에서 `python` 명령은 Windows 스토어 스텁이라 멈출 수 있음 — 쓰지 말 것
- Android Studio에서 열 때는 **AIram 폴더 전체**를 열 것 (Gradle 멀티 모듈로 인식)
