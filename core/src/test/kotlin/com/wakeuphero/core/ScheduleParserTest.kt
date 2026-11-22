package com.wakeuphero.core

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScheduleParserTest {

    private fun date(d: Int) = LocalDate.of(2026, 12, d)

    // 기본 근무 유형 id: D=1, E=2, N=3, OFF=4
    @Test
    fun `날짜와 근무 약어를 한 줄씩 읽는다`() {
        val result = parseSchedule(
            listOf("2026-12-01 D", "2026-12-02 E", "2026-12-03 OFF"),
            defaultShiftTypes
        )
        assertEquals(
            listOf(WorkDay(date(1), 1), WorkDay(date(2), 2), WorkDay(date(3), 4)),
            result.workDays
        )
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `빈 줄과 샵으로 시작하는 주석은 무시한다`() {
        val result = parseSchedule(
            listOf("# 12월 근무표", "", "   ", "2026-12-01 D"),
            defaultShiftTypes
        )
        assertEquals(listOf(WorkDay(date(1), 1)), result.workDays)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `소문자와 여러 칸 띄어쓰기도 받아준다`() {
        val result = parseSchedule(listOf("  2026-12-01    off  "), defaultShiftTypes)
        assertEquals(listOf(WorkDay(date(1), 4)), result.workDays)
    }

    @Test
    fun `모르는 근무 약어는 몇 번째 줄인지 알려준다`() {
        val result = parseSchedule(listOf("2026-12-01 D", "2026-12-02 X"), defaultShiftTypes)
        assertEquals(listOf(WorkDay(date(1), 1)), result.workDays) // 맞는 줄은 그대로 읽음
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0].startsWith("2번째 줄"))
    }

    @Test
    fun `날짜 형식이 틀리면 오류`() {
        val result = parseSchedule(listOf("2026-13-01 D", "12월1일 D"), defaultShiftTypes)
        assertTrue(result.workDays.isEmpty())
        assertEquals(2, result.errors.size)
        assertTrue(result.errors[0].startsWith("1번째 줄"))
        assertTrue(result.errors[1].startsWith("2번째 줄"))
    }

    @Test
    fun `날짜와 근무 두 칸이 아니면 오류`() {
        val result = parseSchedule(listOf("2026-12-01", "2026-12-02 D E"), defaultShiftTypes)
        assertTrue(result.workDays.isEmpty())
        assertEquals(2, result.errors.size)
    }

    @Test
    fun `같은 날짜가 두 번 나오면 두 번째 줄은 오류`() {
        val result = parseSchedule(listOf("2026-12-01 D", "2026-12-01 E"), defaultShiftTypes)
        assertEquals(listOf(WorkDay(date(1), 1)), result.workDays)
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0].startsWith("2번째 줄"))
    }
}
