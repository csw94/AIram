import java.time.LocalTime

// 콘솔 단계에서 쓰는 기본 근무 유형 (나중에 사용자가 직접 바꿀 수 있게 할 예정)
val defaultShiftTypes = listOf(
    ShiftType(1, "주간", "D", LocalTime.of(7, 0), "#FFD54F"),
    ShiftType(2, "오후", "E", LocalTime.of(15, 0), "#FFB74D"),
    ShiftType(3, "야간", "N", LocalTime.of(23, 0), "#BA68C8"),
    ShiftType(4, "휴무", "OFF", null, "#BDBDBD")
)

// 기본 알람 규칙: 모든 근무 출근 90분 전부터, 10분 간격, 3번
val defaultAlarmRules = defaultShiftTypes
    .filter { !it.isOff }
    .map { AlarmRule(shiftTypeId = it.id, minutesBefore = 90, intervalMinutes = 10, repeatCount = 3) }
