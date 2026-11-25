# HANDOVER — 일어나세요용사여 (교대근무자용 알람 앱)

> 마지막 업데이트: 2026-11-25
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
| `57eb4ba` | **2단계 시작: `core` + `app` 멀티 모듈 전환**, Android 앱 홈 화면(오늘 근무 + 다음 알람) — core 테스트 54개 통과, 디버그 APK 빌드 성공 |
| `78dce8d` | **M2 근무표 달력 화면** — core에 편집 함수(`ScheduleEdit.kt`, 테스트 10개), app에 ViewModel·홈·달력 화면 — core 테스트 64개 통과, APK 빌드 성공 |
| `17366eb` | **M3 Room 저장** — core `scheduleDiff()`(테스트 5개), app `data/`(Entity·DAO·DB·Repository), ViewModel이 저장소 사용 — core 테스트 69개 통과, 에뮬레이터에서 재시작 후 유지 확인 |
| `b39b5f4` | **M4 근무 유형 설정 화면** — core `ShiftTypeEdit.kt`(검사·새 id·시간 읽기·알람 미리보기, 테스트 12개), app 목록·편집 화면, 저장·삭제(사용 중이면 막기) — core 테스트 81개 통과, 에뮬레이터 확인 |
| `e76caf7` | **M5 실제 알람** — core `AlarmInfo.kt`(몇 번째 알람인지·알람 글, 테스트 5개), app `alarm/`(예약·리시버·알림·알람 화면), 권한 안내, 디버그 테스트 버튼 — core 테스트 86개 통과, 에뮬레이터에서 울림·끄기·다시 알림·재부팅 재예약 확인 |
| `948b4bd` | **실기기 테스트 1차 반영** — 무음·방해 금지·절전 모드에서 안 울림 → 소리·진동을 `AlarmService`(포그라운드 서비스)가 알람 볼륨으로 직접 재생, 알림 채널은 조용한 `alarm_v2` — 에뮬레이터 무음 모드에서 재생 확인 |
| `fdb66d2` | **M6-1 일반 알람 core** — `GeneralAlarm.kt`(한 번/요일 반복, 켜기·끄기, 근무+일반 합친 `upcomingAlarms`·`nextUpcomingAlarm`, 검사, 요일 글), `AlarmInfo`의 근무 알람 목록을 `workAlarmInfos`로 분리 — core 테스트 99개 통과 |
| `0a372a1` | **M6-2 일반 알람 앱** — DB version 2(general_alarms 표 + 마이그레이션 1→2), 일반 알람 목록(켜기·끄기)·편집(시각·이름·반복 요일·날짜·삭제) 화면, 홈에 오늘·내일 알람 목록, 예약을 `nextUpcomingAlarm`(근무+일반)으로 교체, core `parseDateText`(테스트 1개) — core 테스트 100개, 에뮬레이터에서 마이그레이션(근무표 유지)·추가·예약·울림·다음 예약·알림 [끄기]·"지남" 표시 확인 |
| `34f2a01` | **M6 코드 검토 반영** — 지난 시각 한 번 알람 저장 막기, 지난 한 번 알람을 다시 켜면 다음 날짜로 옮김(`enableGeneralAlarm`), 홈 "내일" 목록 경계(모레 00:00 제외), 편집 화면 입력값이 늦게 불러온 알람으로 다시 맞춰지게, 시스템 리시버 action 확인(lint), 다음 알람 없음 문구 — core 테스트 103개, lint 오류 0 |
| `087e292` | **실기기 테스트(일반 알람) 22개 통과 + 입력 편의** — 시각·날짜 칸에 숫자만 쳐도 ":"·"-" 자동 입력(core `InputFormat.kt`, 테스트 5개, app `FormattedField`), 일반 알람·근무 유형 화면 모두 적용 — core 테스트 108개 |
| `89cca36` | **A1 미션 알람** — core `MathMission.kt`(덧셈·곱셈 번갈아 3문제, 정답 확인, 테스트 7개), app `AppSettings`(SharedPreferences), 홈의 미션 스위치, 알람 화면의 `MathMissionSection`, 미션 중 알림 [끄기] 제거·뒤로가기 막기, 알람 화면 키보드 대응(imePadding+스크롤) — core 테스트 115개, 에뮬레이터에서 문제 풀이로 끄기 확인 |
| `677e045` | **A1 휴대폰 테스트 9개 통과** (스위치 유지·[끄기] 없음·뒤로가기 막힘·키보드 대응·오답 안내·정답 3개로 끄기·다시 알림 후 미션·미션 끄면 기존 동작) |
| `1c5e3db` | **A5 다크 모드** — 휴대폰 설정을 따라감: `Theme.kt`(WakeUpHeroTheme, 밝은/어두운 색), 창 테마 `values(-night)/themes.xml`(여는 순간 흰 화면 번쩍임 방지), `enableEdgeToEdge`(상단 아이콘 색 자동), 직접 쓴 색(달력 빈 칸·색 고르기 테두리)을 테마 색으로 — 에뮬레이터 다크/밝은 모드 화면 캡처로 확인 |
| `dead32b` | **A2 다시 알림 5분/10분** — core `Snooze.kt`(선택지 5·10분, 울릴 시각, 잘못된 값은 5분, "다시 알림 · " 겹침 방지, 버튼 글, 테스트 5개), 알람 화면에 [5분 뒤][10분 뒤] 나란히, 알림 버튼도 [끄기][5분 뒤][10분 뒤] (분은 `EXTRA_MINUTES`) — core 테스트 120개, 에뮬레이터에서 알림 [10분 뒤] → 10분 뒤 예약 확인 |
| `7fb5a76` | **A4 근무 유형별 알람음** — core `ShiftType.soundUri`·`withSound()`(휴무·빈 값은 null), `UpcomingAlarm.sound`(근무=유형 소리, 일반=기본, 테스트 6개), app DB **version 3**(`MIGRATION_2_3` = shift_types에 soundUri 열), 근무 유형 편집에 알람음 버튼(`AlarmSoundPicker.kt`, 휴대폰 기본 소리 선택 창), 예약→리시버→서비스→알림→알람 화면→다시 알림까지 `EXTRA_SOUND` 전달, 서비스는 고른 소리를 못 틀면 기본 알람음, 디버그 테스트 알람은 다음 근무 알람음 — core 테스트 126개, lint 오류 0, 에뮬레이터에서 마이그레이션(기존 유형 유지)·소리 선택·재시작 후 유지·실제 재생 확인 |
| `7bc2231` | **A5·A2·A4 휴대폰 테스트 통과** (다크 모드 화면·번쩍임 없음 / 다시 알림 5·10분 버튼·글 겹침 없음 / 알람음 선택·유지·재생·기본음·DB v3 업데이트 후 근무표 유지) |
| (이 커밋) | **회고 작성** — `RETROSPECTIVE.md` (기획 대비 완료 현황, 잘된 점, 어려웠던 점, 아쉬운 점, 다음 할 일) |

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

**현재 위치: 기획서 기능 중 S8만 남음. 회고 완료(`RETROSPECTIVE.md`) → 남은 것: (선택) S8 사진 입력·앱 아이콘, 미정 사항 결정**

### 휴대폰 테스트 (A5·A2·A4) ✅ 통과 (2026-11-25)
1. A5: 다크 모드 켜고 끄며 모든 화면 색·글자, 앱 열 때 흰 화면 번쩍임 없음
2. A2: 알람 화면 [5분 뒤]·[10분 뒤], 알림 버튼 [끄기][5분 뒤][10분 뒤], "다시 알림 ·" 한 번만
3. A4: 알람음 고르기·재시작 후 유지·테스트 알람/다시 알림 같은 소리·기본 알람음·DB v3 업데이트 후 근무표 유지

### 1단계: 콘솔 ✅ 전부 완료
- 콘솔 입출력, 패턴 자동 채우기, 이번만 끄기, 근무 유형 삭제 막기

### 2단계: Android (마일스톤 순서)
- [x] **M1** 멀티 모듈 전환 + 홈 화면 (오늘 근무, 다음 알람) — 예시 패턴 하드코딩
- [x] **M2** 근무표 달력 화면 (월 이동, 날짜 눌러 근무 바꾸기·지우기, 패턴으로 그 달 채우기, 이번만 끄기)
- [x] **M3** Room으로 근무표·근무 유형·알람 규칙 저장 (첫 실행 시 기본 유형·규칙 자동 입력, 근무표는 빈 상태로 시작)
- [x] **M4** 근무 유형 설정 화면 (목록, 추가·고치기, 휴무 스위치, 출근 시간 "07:00" 입력, 알람 규칙 3칸 + 미리보기, 색 7가지, 검사 메시지, 사용 중이면 삭제 막고 날짜 안내)
- [x] **M5** AlarmManager(setAlarmClock)로 다음 알람 1개 예약, 알람 알림(끌 때까지 반복·전체 화면) + 알람 화면, 알림의 [끄기][5분 뒤], 권한 안내(알림·정확한 알람·전체 화면), 재부팅·시간 변경 시 재예약
- [x] **실기기 테스트** (사용자 삼성 휴대폰) — 1차 4·5·6 실패 → 소리 재생을 포그라운드 서비스로 바꿔 2차 전부 정상
- [ ] **M6** 일반 알람(F1~F6)
  - [x] M6-1 core: GeneralAlarm 모델·울릴 시각·근무와 합친 다가오는 알람·검사 (테스트 13개)
  - [x] M6-2 app: Room에 general_alarms 표(DB version 2 + 마이그레이션), 알람 목록(F2·F3·F4)·편집 화면(F1·F6), 예약을 nextUpcomingAlarm으로 교체
  - [x] 추가 기능(A1·A2·A4·A5)·회고(`RETROSPECTIVE.md`)
- [ ] (추가) S8 근무표 사진 입력 — 설계안 비교 후

### 미정 사항
- 근무 유형/규칙 누락 시 처리 방식 (현재: 조용히 알람 없음)
- 앱 아이콘 (현재 기본값)

---

## 4. 핵심 파일 및 주의사항

| 파일 | 역할 |
|---|---|
| `alarm_app_plan.md` | 기획서 v0.3 (요구사항의 기준) |
| `RETROSPECTIVE.md` | 회고 (2026-11-25) |
| `CLAUDE.md` | 프로젝트 규칙 (Claude가 자동으로 읽음) |
| `settings.gradle.kts` | 모듈 목록(`:core`, `:app`), 저장소(google, mavenCentral) |
| `build.gradle.kts` | 루트: 플러그인 버전만 (`apply false`) |
| `core/build.gradle.kts` | core 빌드 (kotlin jvm + application, 실행 폴더 = 루트) |
| `app/build.gradle.kts` | app 빌드 (AGP, Compose, `implementation(project(":core"))`) |
| `core/src/main/kotlin/com/wakeuphero/core/` | `Models`(ShiftType.soundUri·withSound) `Snooze`(SNOOZE_OPTIONS·snoozeTime·snoozeMinutesOrDefault·snoozeText·snoozeLabel) `GeneralAlarm`(GeneralAlarm·generalAlarmTimes·UpcomingAlarm·upcomingAlarms·nextUpcomingAlarm·validateGeneralAlarm·defaultOneTimeDate·repeatDaysText) `AlarmInfo`(nextAlarmInfo·alarmTitle·alarmText, nextAlarm은 이걸 재사용) `ShiftTypeEdit`(validateShiftType·nextShiftTypeId·parseTimeText·alarmPreview) `ScheduleDiff` `ScheduleEdit`(setShift·removeShift·toggleSkip·applyPattern·parsePatternCodes·monthGrid) `Models` `AlarmCalculator` `DefaultShifts` `ScheduleParser` `PatternFill` `SkipAlarm` `DeleteShiftType` `ConsoleFormat` `Main`(콘솔) |
| `core/src/test/kotlin/com/wakeuphero/core/` | 테스트 126개 (알람 계산 18, 파일 읽기 7, 출력 4, 환경 1, 패턴 9, 이번만 끄기 9, 유형 삭제 6, 근무표 편집 10, 저장 차이 5, 근무 유형 편집 12, 알람 정보 5, 일반 알람 13, 입력 형식 5, 미션 7, 다시 알림 5, 알람음 6 등) |
| `app/src/main/kotlin/com/wakeuphero/app/MainActivity.kt` | `App()`: 화면 전환(enum Screen: HOME, CALENDAR, SHIFT_LIST, SHIFT_EDIT, GENERAL_LIST, GENERAL_EDIT), 뒤로가기 |
| `app/.../ScheduleViewModel.kt` | 저장소의 Flow → StateFlow, 편집 = core 함수로 새 근무표 계산 → 저장소에 저장 → DB 갱신으로 화면 자동 반영. `Factory`로 저장소 주입 |
| `app/.../HomeScreen.kt` | 오늘·내일 근무, 다음 알람(근무+일반), 오늘·내일 알람 목록(최대 8개), [근무표 달력][근무 유형 설정][일반 알람] |
| `app/.../CalendarScreen.kt` | 달력(monthGrid), 날짜 창(DayDialog), 패턴 창(PatternDialog), 근무 색 배경 |
| `app/.../SettingsScreen.kt` | `ShiftTypeListScreen`(목록), `ShiftTypeEditScreen`(편집·검사·삭제), 공용 `hexToColor()` |
| `app/.../GeneralAlarmScreen.kt` | `GeneralAlarmListScreen`(목록·스위치·지남 표시), `GeneralAlarmEditScreen`(시각·이름·요일 칩·날짜·오늘/내일 버튼·검사·삭제) |
| `app/.../AlarmPermissionSection.kt` | 홈 화면의 권한 안내(알림·정확한 알람·전체 화면) + 디버그용 "10초 뒤 테스트 알람" 버튼 |
| `app/.../alarm/AlarmScheduler.kt` | `setAlarmClock`으로 다음 알람 1개 예약(요청코드 0), 다시 알림·테스트(요청코드 1), `rescheduleFromDb()` |
| `app/.../alarm/AlarmReceiver.kt` | 알람 시각 → 알림 띄우고 다음 알람 예약 / 알림 버튼 [끄기][5분 뒤][10분 뒤] / `SystemEventReceiver`: 재부팅·시간 변경·앱 업데이트·권한 변경 시 재예약 |
| `app/.../alarm/AlarmNotifications.kt` | 알람 알림(조용한 채널 alarm_v2, 옛 alarm_v1 삭제), 전체 화면 인텐트, 버튼 최대 3개 — `build()`로 알림만 만들어 서비스에 넘김 |
| `app/.../alarm/AlarmService.kt` | 포그라운드 서비스(mediaPlayback): MediaPlayer(USAGE_ALARM, 반복) + 진동(USAGE_ALARM) + 웨이크락(최대 10분). `start()`/`stop()` |
| `app/.../alarm/AlarmActivity.kt` | 알람 울림 화면 (잠금 화면 위·화면 켜기, 끄기·다시 알림 [5분 뒤][10분 뒤], 미션이면 [끄기] 대신 수학 문제 + 뒤로가기 막기, 키보드 대응) |
| `app/.../alarm/MathMissionSection.kt` | 미션 문제 풀이 UI (씨앗·푼 개수 rememberSaveable, 키패드 [완료]로도 제출) |
| `app/.../AppSettings.kt` | SharedPreferences 설정 (math_mission) |
| `app/.../Theme.kt` | `WakeUpHeroTheme`: 모든 화면을 감싸는 테마 (새 화면도 색은 MaterialTheme.colorScheme에서) |
| `app/.../AlarmSoundPicker.kt` | `AlarmSoundRow`: 근무 유형 알람음 버튼 → 휴대폰 소리 선택 창(RingtoneManager), "기본값" 고르면 null 저장, 소리 이름 표시 |
| `app/.../data/Entities.kt` | DB 표 4개(shift_types, alarm_rules, work_days, general_alarms — 요일은 "1,3,5" 글자) + core ↔ DB 변환 (날짜·시각은 글자로 저장) |
| `app/.../data/AppDao.kt` | 조회는 Flow(자동 갱신), `applyScheduleChanges()`는 @Transaction으로 넣기·지우기 한 묶음 |
| `app/.../data/AppDatabase.kt` | Room DB (파일명 wakeuphero.db, **version 3**, `MIGRATION_1_2` = general_alarms 표, `MIGRATION_2_3` = shift_types.soundUri 열) |
| `app/.../data/ScheduleRepository.kt` | core 모양으로만 주고받는 저장소, `saveSchedule(before, after)` = scheduleDiff로 바뀐 부분만 저장 |
| `app/.../WakeUpHeroApp.kt` | Application: DB·저장소를 앱에 하나만 생성 |
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
- 분기점 기록(M2): 상태 관리 **ViewModel**(vs remember / 전역 객체), 화면 전환 **enum 상태**(vs Navigation 라이브러리 / Activity 여러 개)
- 에뮬레이터: `Medium_Phone_API_37.0` (창 없이 켜기: `emulator -avd Medium_Phone_API_37.0 -no-window`, 설치 `adb install -r app/build/outputs/apk/debug/app-debug.apk`)
- 분기점 기록(M3): 저장 **Room**(vs DataStore+JSON / schedule.txt 파일) — 기획서 기술 스택, DB 학습 목표
- 동시 편집 메모: 빠르게 연속으로 눌러 이전 상태(before)로 계산돼도, scheduleDiff는 "그 편집에서 바뀐 부분"만 저장하므로 앞선 편집을 되돌리지 않음
- 에뮬레이터 화면 조작 팁(Git Bash): `export MSYS_NO_PATHCONV=1` 후 `adb shell uiautomator dump /sdcard/ui.xml` 로 글자 위치 찾아 `adb shell input tap x y`
- Room 스키마를 바꾸면 `version`을 올리고 마이그레이션 필요 (개발 중엔 앱 삭제 후 재설치로 초기화 가능)
- 분기점 기록(M4): 편집 **전체 화면**(vs 팝업 / 목록에서 바로 수정), 출근 시간 **"07:00" 글자 입력**(vs 시계 선택기 / 시·분 따로)
- 제가 정한 것(M4): 약어는 대문자로 저장, 5글자까지, 띄어쓰기·PATTERN·SKIP 금지, 색은 7가지 중 선택, 휴무로 바꾸면 알람 규칙 삭제
- 시간 읽기는 `ResolverStyle.STRICT` (기본값은 "24:00"을 다음 날 00:00으로 너그럽게 받아들여서)
- adb 화면 테스트 시 한글은 `input text`로 입력 불가 → 영어로 시험. 글자 찾기 grep에서 `( ) +`는 특수문자라 이스케이프 필요
- 분기점 기록(M5): 예약 **setAlarmClock**(vs setExactAndAllowWhileIdle / WorkManager), 울림 **알람 채널 + FLAG_INSISTENT + 전체 화면 인텐트**(vs 포그라운드 서비스 / 화면에서만 재생)
- 제가 정한 것(M5): 다시 알림 5분, 알림에 [끄기][5분 뒤] 버튼, 디버그 앱에만 10초 테스트 버튼, 권한은 USE_EXACT_ALARM(알람 앱용, 13+ 자동 허용) + SCHEDULE_EXACT_ALARM(12 전용)
- 알람 동작 메모: 화면을 쓰는 중이면 Android가 전체 화면 대신 상단 알림으로 보여줌(정상). 잠금/화면 꺼짐이면 알람 화면이 바로 뜸
- 근무표·유형·규칙이 바뀌면 ViewModel이 combine으로 감지해 자동 재예약, 알람이 울리면 리시버가 다음 것 예약
- 재부팅 후 BOOT_COMPLETED는 늦게 올 수 있음(에뮬레이터 약 26초) → 확인은 넉넉히 기다린 뒤 `adb shell dumpsys alarm | grep wakeuphero`
- 알림 채널 소리·진동을 바꾸려면 CHANNEL_ID를 바꿔야 함 (한 번 만든 채널 설정은 앱이 못 바꿈)
- 분기점 기록(실기기 1차): 알람 소리 **포그라운드 서비스가 직접 재생**(vs 알람 화면에서만 재생 / 채널 방해 금지 무시 권한) — 알림 소리는 무음·방해 금지·절전에서 시스템이 막음. M5의 '알림 채널 소리' 방식을 대체
- 제가 정한 것: 웨이크락 최대 10분(소리는 끌 때까지 계속), 알람음 없으면 벨소리→알림음 순으로 대체
- 분기점 기록(M6-1): 근무·일반 알람 합치기 **core의 upcomingAlarms 하나로 함께 정렬**(vs 따로 계산해 앱에서 비교 / 일반 알람을 가짜 근무로 변환) — F2 목록과 예약이 같은 함수를 씀
- 제가 정한 것(M6-1): 반복 요일이 없으면 '한 번만'(날짜 필요), 이름은 비워도 됨(→ '알람')·20글자까지, 같은 시각이면 근무 알람 먼저, 지난 한 번 알람은 자동으로 끄지 않고 그냥 안 울림, 기본 날짜는 시각이 지났으면 내일
- 분기점 기록(M6-2): 화면 **홈 → 목록 → 편집 화면**(vs 홈에 바로 목록 / 아래 탭 메뉴), DB **version 2 + 마이그레이션**(vs 지우고 새로 만들기 / 별도 DB)
- 제가 정한 것(M6-2): 새 알람 기본 시각 07:00, 날짜는 '2026-12-01' 글자 입력 + [오늘][내일] 버튼, 일반 알람 id는 DB 자동 번호, 목록은 시각순, 홈의 오늘·내일 목록은 8개까지
- DB 바꿀 때: Entity 수정 → version 올리기 → Migration의 CREATE 문이 Entity와 정확히 같아야 함 (다르면 앱 시작 시 오류)
- 제가 정한 것(M6 검토): 지난 시각의 한 번 알람은 저장 불가(경고), 지난 한 번 알람을 스위치로 켜면 오늘/내일의 그 시각으로 옮김, 같은 시각 근무·일반 알람은 한 번만 울림(제목은 근무 알람)
- 분기점 기록(입력 편의): **치는 대로 기호 자동 삽입**(vs 읽을 때만 너그럽게 해석 / 시계·달력 선택기) — 숫자 키패드, 커서는 항상 맨 끝(중간 수정 대신 지우고 다시 입력)
- 분기점 기록(A1): 미션 단위 **앱 전체 스위치 하나**(vs 근무 유형·일반 알람마다 / 항상 켜짐), 설정 저장 **SharedPreferences**(vs DataStore / Room v3)
- 제가 정한 것(A1): 3문제(두 자리 덧셈 10~89, 곱셈 2~9, 번갈아), 틀리면 같은 문제 유지, 미션 중에도 [5분 뒤]는 허용, 미션이면 알림의 [끄기] 버튼 없음(예전 알림 버튼도 무시), 미션 중 뒤로가기 막음, 문제 푸는 동안에도 소리는 계속
- 알람 화면 주의: targetSdk 37은 화면 끝까지 그리기(edge-to-edge)라 키보드가 와도 화면이 저절로 안 줄어듦 → imePadding 필요
- 분기점 기록(A5): **휴대폰 다크 모드 설정 따라가기**(vs 앱 안에서 자동/밝게/어둡게 고르기 / 배경화면 색 따라가는 Material You) — 근무 색이 기종마다 달라지지 않게
- 다크 모드 주의: 색을 직접 쓰지 말 것(Color.Black 등). 근무 색 배경은 투명도 0.45라 어두운 바탕에서도 구분됨
- 분기점 기록(A2): **알람 화면·알림에 [5분 뒤][10분 뒤] 버튼 2개**(vs 홈에서 하나 미리 설정 / 근무 유형·일반 알람마다 설정 = DB v3) — 기획서 "5분 / 10분" 그대로, 울릴 때 바로 고름
- 제가 정한 것(A2): 선택지는 5·10분 두 개, 알림 버튼의 분 값이 없거나 이상하면 5분, 다시 알림 글 앞 표시는 한 번만, 미션 중에도 두 버튼 모두 허용, 알림 버튼 요청 코드 끄기 2·5분 3·10분 4 (Android 알림 버튼은 최대 3개라 선택지를 더 늘리려면 알림 쪽 구성을 바꿔야 함)
- 분기점 기록(A4): **휴대폰 소리 선택 창으로 고르고 소리 주소를 근무 유형에 저장(DB v3)**(vs 앱에 소리 파일 내장 / 목록·미리듣기 화면 직접 만들기) — 소리 파일 저작권 문제 없음, 미리듣기는 선택 창이 해 줌
- 제가 정한 것(A4): 알람음은 근무 유형에만(일반 알람은 기본 알람음), 선택 창에서 "기본값"을 고르면 null 저장(휴대폰 기본 알람음이 바뀌면 따라감), 무음은 고를 수 없음, 휴무 유형은 소리 없음, 고른 소리를 못 틀면(지워짐 등) 기본 알람음으로 대신, 같은 시각 근무·일반 알람이 겹치면 근무 알람음, 디버그 테스트 알람은 다음 근무 알람의 소리
- 알람음 메모: 에뮬레이터 선택 창이 준 주소는 `content://media/external_primary/audio/media/38?...` 형태(공용 저장소)였고 권한 없이 재생됨. 재생/실패는 logcat `AlarmService` 태그로 확인
