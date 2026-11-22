import java.time.LocalDateTime

// ⚠️ 아직 구현 전입니다. (TDD: 테스트를 먼저 작성하고, 테스트가 실패하는 걸 확인한 뒤 구현)

// 알람 규칙이 올바른지 확인 (마지막 알람이 출근 시간보다 먼저 울려야 함)
fun AlarmRule.isValid(): Boolean = TODO("구현 예정")

// 근무 하루에 대한 알람 시각 목록 (휴무·이번만 끄기면 빈 목록)
fun alarmTimesFor(workDay: WorkDay, shiftType: ShiftType, rule: AlarmRule): List<LocalDateTime> =
    TODO("구현 예정")

// 현재 시각(now) "이후" ~ 한 달 뒤까지 중 가장 빠른 알람 1개 (없으면 null)
fun nextAlarm(
    now: LocalDateTime,
    schedule: List<WorkDay>,
    shiftTypes: List<ShiftType>,
    rules: List<AlarmRule>
): LocalDateTime? = TODO("구현 예정")
