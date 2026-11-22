import java.time.LocalDateTime

// 알람 규칙이 올바른지 확인
// - 모든 값은 1 이상
// - 마지막 알람이 출근 시간보다 "먼저" 울려야 함 (정각도 안 됨)
//   마지막 알람 = 출근 - minutesBefore + interval × (repeatCount - 1)
//   → interval × (repeatCount - 1) < minutesBefore 이면 OK
fun AlarmRule.isValid(): Boolean {
    if (minutesBefore < 1 || intervalMinutes < 1 || repeatCount < 1) return false
    return intervalMinutes * (repeatCount - 1) < minutesBefore
}

// 근무 하루에 대한 알람 시각 목록 (휴무·이번만 끄기면 빈 목록)
fun alarmTimesFor(workDay: WorkDay, shiftType: ShiftType, rule: AlarmRule): List<LocalDateTime> {
    val startTime = shiftType.startTime ?: return emptyList() // 출근 시간이 없으면 휴무
    if (workDay.alarmSkipped) return emptyList()

    // 날짜 + 시각을 합쳐서 계산해야 자정을 넘어 전날로 가는 경우도 맞게 나온다
    val workStart = workDay.date.atTime(startTime)
    val firstAlarm = workStart.minusMinutes(rule.minutesBefore.toLong())

    // n번째 알람 = 첫 알람 + 간격 × (n - 1)
    return (0 until rule.repeatCount).map { n ->
        firstAlarm.plusMinutes(rule.intervalMinutes.toLong() * n)
    }
}

// 현재 시각(now) "이후" ~ 한 달 뒤까지 중 가장 빠른 알람 1개 (없으면 null)
fun nextAlarm(
    now: LocalDateTime,
    schedule: List<WorkDay>,
    shiftTypes: List<ShiftType>,
    rules: List<AlarmRule>
): LocalDateTime? {
    val limit = now.plusMonths(1)

    return schedule
        .flatMap { workDay ->
            // 근무 유형이나 알람 규칙을 못 찾으면 그날은 알람 없음
            val shiftType = shiftTypes.find { it.id == workDay.shiftTypeId } ?: return@flatMap emptyList()
            val rule = rules.find { it.shiftTypeId == workDay.shiftTypeId } ?: return@flatMap emptyList()
            alarmTimesFor(workDay, shiftType, rule)
        }
        .filter { it > now && it <= limit } // 방금 울린 알람(now와 같음)은 제외
        .minOrNull()
}
