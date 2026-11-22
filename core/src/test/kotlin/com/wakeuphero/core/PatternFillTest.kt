package com.wakeuphero.core

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PatternFillTest {

    private fun date(d: Int) = LocalDate.of(2026, 12, d)

    // 기본 근무 유형 id: D=1, E=2, N=3, OFF=4
    private val D = 1
    private val E = 2
    private val N = 3
    private val OFF = 4

    // ── 1. fillPattern: 패턴 펼치기 ───────────────────────────
    @Test
    fun `패턴을 끝날까지 반복해서 채운다`() {
        val result = fillPattern(date(1), date(5), listOf(D, D, OFF))
        assertEquals(
            listOf(
                WorkDay(date(1), D), WorkDay(date(2), D), WorkDay(date(3), OFF),
                WorkDay(date(4), D), WorkDay(date(5), D)
            ),
            result
        )
    }

    @Test
    fun `시작일과 끝날이 같으면 하루만 채운다`() {
        assertEquals(listOf(WorkDay(date(1), E)), fillPattern(date(1), date(1), listOf(E, N)))
    }

    @Test
    fun `달이 바뀌어도 이어서 채운다`() {
        val result = fillPattern(date(31), LocalDate.of(2027, 1, 2), listOf(D, N))
        assertEquals(
            listOf(
                WorkDay(date(31), D),
                WorkDay(LocalDate.of(2027, 1, 1), N),
                WorkDay(LocalDate.of(2027, 1, 2), D)
            ),
            result
        )
    }

    @Test
    fun `시작일이 끝날보다 늦거나 패턴이 비어 있으면 빈 목록`() {
        assertTrue(fillPattern(date(5), date(1), listOf(D)).isEmpty())
        assertTrue(fillPattern(date(1), date(5), emptyList()).isEmpty())
    }

    // ── 2. schedule.txt의 PATTERN 줄 ──────────────────────────
    @Test
    fun `PATTERN 줄로 기간 전체를 채운다`() {
        val result = parseSchedule(listOf("PATTERN 2026-12-01 2026-12-04 D E N OFF"), defaultShiftTypes)
        assertEquals(
            listOf(WorkDay(date(1), D), WorkDay(date(2), E), WorkDay(date(3), N), WorkDay(date(4), OFF)),
            result.workDays
        )
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `날짜 줄이 패턴보다 우선한다 - 순서와 상관없이`() {
        // 날짜 줄이 패턴 줄보다 위에 있어도 날짜 줄이 이긴다
        val result = parseSchedule(
            listOf("2026-12-02 OFF", "PATTERN 2026-12-01 2026-12-03 D"),
            defaultShiftTypes
        )
        assertEquals(
            listOf(WorkDay(date(1), D), WorkDay(date(2), OFF), WorkDay(date(3), D)),
            result.workDays
        )
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `패턴끼리 겹치면 아래 줄 패턴이 우선한다`() {
        val result = parseSchedule(
            listOf("PATTERN 2026-12-01 2026-12-03 D", "pattern 2026-12-03 2026-12-04 n"),
            defaultShiftTypes
        )
        assertEquals(
            listOf(WorkDay(date(1), D), WorkDay(date(2), D), WorkDay(date(3), N), WorkDay(date(4), N)),
            result.workDays
        )
    }

    @Test
    fun `PATTERN 줄에 모르는 근무가 있으면 그 줄 전체를 오류로 건너뛴다`() {
        val result = parseSchedule(listOf("PATTERN 2026-12-01 2026-12-03 D X"), defaultShiftTypes)
        assertTrue(result.workDays.isEmpty())
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0].startsWith("1번째 줄"))
    }

    @Test
    fun `PATTERN 줄 형식이 틀리면 오류`() {
        val result = parseSchedule(
            listOf(
                "PATTERN 2026-12-01 2026-12-03",      // 근무가 없음
                "PATTERN 2026-12-05 2026-12-01 D",    // 시작일이 끝날보다 늦음
                "PATTERN 12월1일 2026-12-03 D"        // 날짜 형식 틀림
            ),
            defaultShiftTypes
        )
        assertTrue(result.workDays.isEmpty())
        assertEquals(3, result.errors.size)
        assertTrue(result.errors[0].startsWith("1번째 줄"))
        assertTrue(result.errors[1].startsWith("2번째 줄"))
        assertTrue(result.errors[2].startsWith("3번째 줄"))
    }
}
