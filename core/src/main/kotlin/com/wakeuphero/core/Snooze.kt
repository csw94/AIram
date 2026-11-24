package com.wakeuphero.core

import java.time.LocalDateTime

// 다시 알림(스누즈): 알람 화면·알림에서 [5분 뒤] [10분 뒤] 중 골라 누른다

val SNOOZE_OPTIONS: List<Long> = listOf(5L, 10L)

private const val SNOOZE_PREFIX = "다시 알림 · "

fun snoozeTime(now: LocalDateTime, minutes: Long): LocalDateTime = now.plusMinutes(minutes)

// 알림 버튼 등에서 넘어온 값이 선택지에 없거나 비었으면 첫 번째(5분)로
fun snoozeMinutesOrDefault(minutes: Long?): Long =
    if (minutes != null && minutes in SNOOZE_OPTIONS) minutes else SNOOZE_OPTIONS.first()

// 여러 번 다시 알림을 눌러도 "다시 알림 · 다시 알림 · ..." 처럼 겹치지 않게
fun snoozeText(text: String): String {
    var original = text
    while (original.startsWith(SNOOZE_PREFIX)) original = original.removePrefix(SNOOZE_PREFIX)
    return SNOOZE_PREFIX + original
}

fun snoozeLabel(minutes: Long): String = "${minutes}분 뒤"
