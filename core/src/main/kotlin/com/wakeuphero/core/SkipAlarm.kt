package com.wakeuphero.core

import java.time.LocalDate

// 근무표에서 date 날의 알람만 "이번만 끄기" 한 새 근무표를 돌려준다 (근무는 그대로)
// data class의 copy() → 나머지 값은 그대로 두고 alarmSkipped만 바꾼 복사본을 만든다
fun skipAlarm(schedule: List<WorkDay>, date: LocalDate): List<WorkDay> =
    schedule.map { if (it.date == date) it.copy(alarmSkipped = true) else it }
