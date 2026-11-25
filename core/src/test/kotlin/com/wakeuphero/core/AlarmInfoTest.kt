package com.wakeuphero.core

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AlarmInfoTest {

    private fun date(d: Int) = LocalDate.of(2026, 12, d)
    private fun at(d: Int, h: Int, m: Int) = LocalDateTime.of(2026, 12, d, h, m)

    // 기본 근무 유형 id: D=1(07:00), E=2(15:00), N=3(23:00), OFF=4 / 기본 규칙: 90분 전·10분 간격·3번
    private fun info(now: LocalDateTime, schedule: List<WorkDay>) =
        nextAlarmInfo(now, schedule, defaultShiftTypes, defaultAlarmRules)

    @Test
    fun `다음 알람이 몇 번째 알람인지 알려준다`() {
        val schedule = listOf(WorkDay(date(1), 1))
        val first = info(at(1, 5, 0), schedule)!!
        assertEquals(at(1, 5, 30), first.time)
        assertEquals(1, first.order)
        assertEquals(3, first.total)
        assertEquals("D", first.shiftType.shortName)

        val second = info(at(1, 5, 30), schedule)!! // 첫 알람이 울린 직후
        assertEquals(at(1, 5, 40), second.time)
        assertEquals(2, second.order)
    }

    @Test
    fun `자정 전에 울리는 알람은 다음 날 근무 정보를 담는다`() {
        val midnight = ShiftType(9, "새벽", "M", java.time.LocalTime.of(0, 30), "#7E57C2")
        val rule = AlarmRule(9, minutesBefore = 120, intervalMinutes = 10, repeatCount = 1)
        val result = nextAlarmInfo(at(1, 20, 0), listOf(WorkDay(date(2), 9)), listOf(midnight), listOf(rule))!!
        assertEquals(at(1, 22, 30), result.time)
        assertEquals(date(2), result.workDay.date)
    }

    @Test
    fun `nextAlarm과 같은 시각을 고른다`() {
        val schedule = listOf(WorkDay(date(1), 4), WorkDay(date(2), 3, alarmSkipped = true), WorkDay(date(3), 2))
        val now = at(1, 12, 0)
        assertEquals(nextAlarm(now, schedule, defaultShiftTypes, defaultAlarmRules), info(now, schedule)?.time)
    }

    @Test
    fun `알람이 없으면 null`() {
        assertNull(info(at(1, 6, 0), listOf(WorkDay(date(1), 1))))
    }

    @Test
    fun `알람 제목과 내용 글`() {
        val result = info(at(1, 5, 0), listOf(WorkDay(date(1), 1)))!!
        assertEquals("주간 근무", alarmTitle(result))
        assertEquals("12/01(화) 07:00 출근 · 1/3번째 알람", alarmText(result))
    }

    @Test
    fun `알람 규칙이 없는 근무 유형은 기본 규칙으로 울린다`() {
        val noRules = defaultAlarmRules.filter { it.shiftTypeId != 1 } // D 규칙만 빠짐
        val result = nextAlarmInfo(at(1, 0, 0), listOf(WorkDay(date(1), 1)), defaultShiftTypes, noRules)!!
        assertEquals(at(1, 5, 30), result.time) // 07:00 - 90분
        assertEquals(3, result.total)
    }

    @Test
    fun `근무 유형을 못 찾으면 출근 시간을 몰라 알람이 없다`() {
        assertNull(info(at(1, 0, 0), listOf(WorkDay(date(1), 99))))
    }

    @Test
    fun `규칙이 비어 있어도 휴무에는 알람이 없다`() {
        assertNull(nextAlarmInfo(at(1, 0, 0), listOf(WorkDay(date(1), 4)), defaultShiftTypes, emptyList()))
    }

    @Test
    fun `규칙 찾기는 없으면 기본 규칙을 그 유형 id로 돌려준다`() {
        assertEquals(AlarmRule(7, 90, 10, 3), ruleOrDefault(7, emptyList()))
        val custom = AlarmRule(7, 30, 5, 2)
        assertEquals(custom, ruleOrDefault(7, listOf(custom)))
    }
}
