package com.wakeuphero.core

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
    .map { ruleOrDefault(it.id, emptyList()) }

// 근무 유형의 알람 규칙 찾기 — 없으면 기본 규칙(90분 전·10분 간격·3번)으로 대신한다
// (알람 앱에서 출근날 알람이 조용히 빠지는 게 가장 위험하므로)
fun ruleOrDefault(shiftTypeId: Int, rules: List<AlarmRule>): AlarmRule =
    rules.find { it.shiftTypeId == shiftTypeId }
        ?: AlarmRule(shiftTypeId = shiftTypeId, minutesBefore = 90, intervalMinutes = 10, repeatCount = 3)
