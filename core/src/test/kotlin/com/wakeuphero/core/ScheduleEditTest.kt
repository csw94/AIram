package com.wakeuphero.core

import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScheduleEditTest {

    private fun date(d: Int) = LocalDate.of(2026, 12, d)

    // 기본 근무 유형 id: D=1, E=2, N=3, OFF=4
    private val D = 1
    private val E = 2
    private val N = 3
    private val OFF = 4

    // ── setShift / removeShift ───────────────────────────────
    @Test
    fun `근무가 없는 날에 근무를 넣으면 날짜순으로 들어간다`() {
        val schedule = listOf(WorkDay(date(1), D), WorkDay(date(3), D))
        assertEquals(
            listOf(WorkDay(date(1), D), WorkDay(date(2), E), WorkDay(date(3), D)),
            setShift(schedule, date(2), E)
        )
    }

    @Test
    fun `근무를 바꾸면 이번만 끄기는 해제된다`() {
        val schedule = listOf(WorkDay(date(1), D, alarmSkipped = true))
        assertEquals(listOf(WorkDay(date(1), N)), setShift(schedule, date(1), N))
    }

    @Test
    fun `근무를 지우면 그 날짜만 빠진다`() {
        val schedule = listOf(WorkDay(date(1), D), WorkDay(date(2), E))
        assertEquals(listOf(WorkDay(date(2), E)), removeShift(schedule, date(1)))
        assertEquals(schedule, removeShift(schedule, date(9))) // 없는 날짜는 그대로
    }

    // ── toggleSkip ────────────────────────────────────────────
    @Test
    fun `이번만 끄기를 두 번 누르면 원래대로 돌아온다`() {
        val schedule = listOf(WorkDay(date(1), D))
        val once = toggleSkip(schedule, date(1))
        assertEquals(listOf(WorkDay(date(1), D, alarmSkipped = true)), once)
        assertEquals(schedule, toggleSkip(once, date(1)))
    }

    @Test
    fun `근무가 없는 날의 이번만 끄기는 아무것도 바꾸지 않는다`() {
        val schedule = listOf(WorkDay(date(1), D))
        assertEquals(schedule, toggleSkip(schedule, date(2)))
    }

    // ── applyPattern ──────────────────────────────────────────
    @Test
    fun `기간 안은 패턴으로 다시 채우고 기간 밖은 그대로 둔다`() {
        val schedule = listOf(
            WorkDay(date(1), N),
            WorkDay(date(2), N, alarmSkipped = true),
            WorkDay(date(5), N)
        )
        assertEquals(
            listOf(
                WorkDay(date(1), N),
                WorkDay(date(2), D), WorkDay(date(3), OFF), WorkDay(date(4), D),
                WorkDay(date(5), N)
            ),
            applyPattern(schedule, date(2), date(4), listOf(D, OFF))
        )
    }

    // ── parsePatternCodes ─────────────────────────────────────
    @Test
    fun `패턴 글자를 근무 유형 id로 바꾼다 - 소문자와 여러 칸 띄어쓰기 허용`() {
        assertEquals(
            listOf(D, D, E, E, N, N, OFF, OFF),
            parsePatternCodes("  D d E  e N n off OFF ", defaultShiftTypes)
        )
    }

    @Test
    fun `모르는 약어가 있거나 비어 있으면 null`() {
        assertNull(parsePatternCodes("D X", defaultShiftTypes))
        assertNull(parsePatternCodes("   ", defaultShiftTypes))
    }

    // ── monthGrid ─────────────────────────────────────────────
    @Test
    fun `달력 칸은 일요일 시작으로 앞뒤 빈칸을 null로 채운다`() {
        // 2026년 12월: 1일 화요일, 31일 목요일 → 5주
        val grid = monthGrid(YearMonth.of(2026, 12))
        assertEquals(5, grid.size)
        assertEquals(listOf(null, null, date(1), date(2), date(3), date(4), date(5)), grid[0])
        assertEquals(listOf(date(27), date(28), date(29), date(30), date(31), null, null), grid[4])
        grid.forEach { week -> assertEquals(7, week.size) }
    }

    @Test
    fun `1일이 일요일이면 앞 빈칸이 없다`() {
        // 2026년 11월 1일은 일요일
        val grid = monthGrid(YearMonth.of(2026, 11))
        assertEquals(LocalDate.of(2026, 11, 1), grid[0][0])
    }
}
