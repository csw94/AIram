package com.wakeuphero.core

import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RosterImportTest {

    private val month = YearMonth.of(2026, 12)
    private fun date(d: Int) = LocalDate.of(2026, 12, d)

    // 기본 근무 유형 id: D=1, E=2, N=3, OFF=4
    private val D = 1
    private val E = 2
    private val N = 3
    private val OFF = 4

    private fun parse(text: String, name: String = "홍길동") =
        parseRosterText(text, name, month, defaultShiftTypes)

    private fun found(text: String, name: String = "홍길동"): RosterParseResult.Found =
        assertIs<RosterParseResult.Found>(parse(text, name))

    // ── 이름 줄 찾기 ─────────────────────────────────────────
    @Test
    fun `이름이 있는 줄의 약어를 1일부터 차례로 날짜에 맞춘다`() {
        val text = """
            이름 1 2 3 4
            김철수 N N OFF D
            홍길동 D E N OFF
        """.trimIndent()
        assertEquals(
            listOf(
                RosterCell(date(1), "D", D),
                RosterCell(date(2), "E", E),
                RosterCell(date(3), "N", N),
                RosterCell(date(4), "OFF", OFF)
            ),
            found(text).cells
        )
    }

    @Test
    fun `이름 중간에 띄어쓰기가 있어도 찾는다`() {
        assertEquals(listOf(D, E), found("1 홍 길동 D E").cells.map { it.shiftTypeId })
        assertEquals(listOf(D), found("홍길동 D", name = "홍 길동").cells.map { it.shiftTypeId })
    }

    @Test
    fun `이름이 있는 줄이 여러 개면 첫 줄을 쓴다`() {
        assertEquals(listOf(N), found("홍길동 N\n홍길동 D").cells.map { it.shiftTypeId })
    }

    @Test
    fun `이름을 못 찾으면 NameNotFound`() {
        assertEquals(RosterParseResult.NameNotFound, parse("김철수 D E N"))
    }

    @Test
    fun `이름을 비우면 모든 줄을 이어서 내 근무표로 본다`() {
        val cells = found("D E\nN OFF", name = " ").cells
        assertEquals(listOf(date(1), date(2), date(3), date(4)), cells.map { it.date })
        assertEquals(listOf(D, E, N, OFF), cells.map { it.shiftTypeId })
    }

    @Test
    fun `약어가 하나도 없으면 NoCodes`() {
        assertEquals(RosterParseResult.NoCodes, parse("홍길동"))
        assertEquals(RosterParseResult.NoCodes, parse("  \n ", name = ""))
    }

    // ── 약어 알아보기 ────────────────────────────────────────
    @Test
    fun `소문자·근무 이름·쉼표와 세로줄 구분도 알아본다`() {
        assertEquals(listOf(D, D, E, N), found("홍길동 d,주간|e 야간").cells.map { it.shiftTypeId })
    }

    @Test
    fun `휴무는 O·0FF·휴·슬래시로 적혀 있어도 알아본다`() {
        assertEquals(
            listOf(OFF, OFF, OFF, OFF, OFF),
            found("홍길동 O 0FF 휴 / off").cells.map { it.shiftTypeId }
        )
    }

    @Test
    fun `모르는 글자는 근무 없이 남겨 사용자가 고치게 한다`() {
        val cells = found("홍길동 D X? N").cells
        assertEquals(RosterCell(date(2), "X?", null), cells[1])
        assertEquals(listOf(D, null, N), cells.map { it.shiftTypeId })
    }

    @Test
    fun `말일보다 많은 칸은 잘라내고 개수를 알려준다`() {
        val result = found("홍길동 " + List(33) { "D" }.joinToString(" "))
        assertEquals(31, result.cells.size)
        assertEquals(date(31), result.cells.last().date)
        assertEquals(2, result.extraCount)
        assertEquals(0, found("홍길동 D").extraCount)
    }

    // ── 근무표에 반영 ────────────────────────────────────────
    @Test
    fun `알아본 칸만 근무표에 넣고 모르는 칸과 다른 날은 그대로 둔다`() {
        val schedule = listOf(
            WorkDay(date(2), N, alarmSkipped = true), // 모르는 칸 → 그대로
            WorkDay(date(3), D, alarmSkipped = true), // 새 근무로 바뀜 → 이번만 끄기 해제
            WorkDay(date(20), E)                      // 근무표에 없는 날 → 그대로
        )
        val cells = listOf(
            RosterCell(date(1), "D", D),
            RosterCell(date(2), "??", null),
            RosterCell(date(3), "OFF", OFF)
        )
        assertEquals(
            listOf(
                WorkDay(date(1), D),
                WorkDay(date(2), N, alarmSkipped = true),
                WorkDay(date(3), OFF),
                WorkDay(date(20), E)
            ),
            applyRoster(schedule, cells)
        )
    }
}
