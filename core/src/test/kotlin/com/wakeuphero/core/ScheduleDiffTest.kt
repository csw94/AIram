package com.wakeuphero.core

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScheduleDiffTest {

    private fun date(d: Int) = LocalDate.of(2026, 12, d)

    @Test
    fun `바뀐 것이 없으면 할 일도 없다`() {
        val schedule = listOf(WorkDay(date(1), 1), WorkDay(date(2), 2))
        val diff = scheduleDiff(schedule, schedule)
        assertTrue(diff.upserts.isEmpty())
        assertTrue(diff.deletions.isEmpty())
    }

    @Test
    fun `새로 생긴 근무와 바뀐 근무는 upserts에 들어간다`() {
        val before = listOf(WorkDay(date(1), 1), WorkDay(date(2), 2))
        val after = listOf(WorkDay(date(1), 1), WorkDay(date(2), 3), WorkDay(date(3), 1))
        val diff = scheduleDiff(before, after)
        assertEquals(listOf(WorkDay(date(2), 3), WorkDay(date(3), 1)), diff.upserts)
        assertTrue(diff.deletions.isEmpty())
    }

    @Test
    fun `이번만 끄기만 바뀌어도 upserts에 들어간다`() {
        val before = listOf(WorkDay(date(1), 1))
        val after = listOf(WorkDay(date(1), 1, alarmSkipped = true))
        assertEquals(after, scheduleDiff(before, after).upserts)
    }

    @Test
    fun `없어진 날짜는 deletions에 들어간다`() {
        val before = listOf(WorkDay(date(1), 1), WorkDay(date(2), 2), WorkDay(date(3), 3))
        val after = listOf(WorkDay(date(2), 2))
        val diff = scheduleDiff(before, after)
        assertTrue(diff.upserts.isEmpty())
        assertEquals(listOf(date(1), date(3)), diff.deletions)
    }

    @Test
    fun `편집 함수 결과를 그대로 넣어도 맞게 계산된다`() {
        val before = fillPattern(date(1), date(4), listOf(1, 4))       // D OFF D OFF
        val after = applyPattern(before, date(3), date(5), listOf(2))  // 3~5일을 E로
        val diff = scheduleDiff(before, after)
        assertEquals(listOf(WorkDay(date(3), 2), WorkDay(date(4), 2), WorkDay(date(5), 2)), diff.upserts)
        assertTrue(diff.deletions.isEmpty())
    }
}
