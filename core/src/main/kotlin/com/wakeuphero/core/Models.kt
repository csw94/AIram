package com.wakeuphero.core

import java.time.LocalDate
import java.time.LocalTime

// 근무 유형 (예: 주간, 야간, 휴무)
data class ShiftType(
    val id: Int,
    val name: String,          // "주간"
    val shortName: String,     // "D"
    val startTime: LocalTime?, // 07:00 (휴무는 null)
    val color: String,         // "#FFD54F"
    val soundUri: String? = null // 알람음 (휴대폰 소리 주소), null = 휴대폰 기본 알람음
) {
    // 출근 시간이 없으면 휴무
    val isOff: Boolean get() = startTime == null

    // 저장할 알람음 정리: 휴무는 알람이 없으니 소리도 없음, 빈 값은 기본 알람음
    fun withSound(uri: String?): ShiftType = copy(soundUri = if (isOff) null else uri?.takeIf { it.isNotBlank() })
}

// 알람 규칙 (근무 유형마다 하나씩)
data class AlarmRule(
    val shiftTypeId: Int,
    val minutesBefore: Int,    // 90 → 출근 1시간 30분 전부터
    val intervalMinutes: Int,  // 10 → 10분 간격
    val repeatCount: Int       // 3 → 3번 울림
)

// 근무표 (날짜별 근무)
data class WorkDay(
    val date: LocalDate,              // 2026-12-01
    val shiftTypeId: Int,             // 주간
    val alarmSkipped: Boolean = false // "이번만 끄기"
)
