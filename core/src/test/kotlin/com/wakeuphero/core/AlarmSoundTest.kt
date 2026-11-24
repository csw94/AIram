package com.wakeuphero.core

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AlarmSoundTest {
    private val dec1 = LocalDate.of(2026, 12, 1)
    private val ring = "content://media/internal/audio/media/42"

    private val dayShift = ShiftType(1, "주간", "D", LocalTime.of(7, 0), "#FFD54F", soundUri = ring)
    private val nightShift = ShiftType(2, "야간", "N", LocalTime.of(19, 0), "#7986CB")
    private val rules = listOf(AlarmRule(1, 60, 10, 1), AlarmRule(2, 60, 10, 1))

    @Test
    fun `근무 유형의 알람음은 기본값이 없음(휴대폰 기본 알람음)`() {
        assertNull(nightShift.soundUri)
    }

    @Test
    fun `근무 알람은 그 근무 유형의 알람음을 가진다`() {
        val next = nextUpcomingAlarm(
            LocalDateTime.of(2026, 11, 30, 12, 0),
            listOf(WorkDay(dec1, 1)), listOf(dayShift, nightShift), rules, emptyList()
        )
        assertEquals(ring, next?.sound)
    }

    @Test
    fun `알람음을 안 고른 근무는 기본 알람음`() {
        val next = nextUpcomingAlarm(
            LocalDateTime.of(2026, 11, 30, 12, 0),
            listOf(WorkDay(dec1, 2)), listOf(dayShift, nightShift), rules, emptyList()
        )
        assertNull(next?.sound)
    }

    @Test
    fun `일반 알람은 기본 알람음`() {
        val general = GeneralAlarm(1, "운동", LocalTime.of(9, 0), date = dec1)
        val next = nextUpcomingAlarm(
            LocalDateTime.of(2026, 11, 30, 12, 0), emptyList(), listOf(dayShift), rules, listOf(general)
        )
        assertNull(next?.sound)
    }

    @Test
    fun `같은 시각 근무와 일반 알람이 겹치면 근무 알람음`() {
        val general = GeneralAlarm(1, "운동", LocalTime.of(6, 0), date = dec1) // 주간 알람도 06:00
        val next = nextUpcomingAlarm(
            LocalDateTime.of(2026, 11, 30, 12, 0),
            listOf(WorkDay(dec1, 1)), listOf(dayShift), rules, listOf(general)
        )
        assertEquals(ring, next?.sound)
    }

    @Test
    fun `저장할 알람음 정리 - 휴무이거나 빈 값이면 없음`() {
        assertEquals(ring, dayShift.withSound(ring).soundUri)
        assertNull(dayShift.withSound("").soundUri)
        assertNull(dayShift.withSound(null).soundUri)
        val off = ShiftType(3, "휴무", "OFF", null, "#BDBDBD")
        assertNull(off.withSound(ring).soundUri)
    }
}
