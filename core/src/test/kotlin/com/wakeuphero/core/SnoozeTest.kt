package com.wakeuphero.core

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class SnoozeTest {
    @Test
    fun `다시 알림 선택지는 5분과 10분`() {
        assertEquals(listOf(5L, 10L), SNOOZE_OPTIONS)
    }

    @Test
    fun `다시 알림 시각은 지금부터 고른 분 뒤 - 자정도 넘어감`() {
        val now = LocalDateTime.of(2026, 11, 30, 23, 55)
        assertEquals(LocalDateTime.of(2026, 12, 1, 0, 5), snoozeTime(now, 10))
        assertEquals(LocalDateTime.of(2026, 12, 1, 0, 0), snoozeTime(now, 5))
    }

    @Test
    fun `선택지에 없는 분이나 값이 없으면 5분으로`() {
        assertEquals(10L, snoozeMinutesOrDefault(10))
        assertEquals(5L, snoozeMinutesOrDefault(5))
        assertEquals(5L, snoozeMinutesOrDefault(0))
        assertEquals(5L, snoozeMinutesOrDefault(-3))
        assertEquals(5L, snoozeMinutesOrDefault(7))
        assertEquals(5L, snoozeMinutesOrDefault(null))
    }

    @Test
    fun `다시 알림 글은 앞에 표시를 붙이고 여러 번 눌러도 겹치지 않음`() {
        val once = snoozeText("주간 출근 1번째 알람")
        assertEquals("다시 알림 · 주간 출근 1번째 알람", once)
        assertEquals(once, snoozeText(once))
        assertEquals(once, snoozeText(snoozeText(once)))
    }

    @Test
    fun `버튼 글`() {
        assertEquals("5분 뒤", snoozeLabel(5))
        assertEquals("10분 뒤", snoozeLabel(10))
    }
}
