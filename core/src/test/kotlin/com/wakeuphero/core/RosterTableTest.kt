package com.wakeuphero.core

import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

// 사진 글자 상자 → 날짜 칸 위치로 근무 맞추기 (S8-2)
class RosterTableTest {

    private val month = YearMonth.of(2026, 11) // 30일까지
    private fun date(d: Int) = LocalDate.of(2026, 11, d)

    // 기본 근무 유형 id: D=1, E=2, N=3, OFF=4
    private val D = 1
    private val E = 2
    private val N = 3
    private val OFF = 4

    // 날짜 d 칸의 가운데 x = 100 + 70 × d, 칸 너비 60, 글자 높이 20
    private fun colX(d: Int) = 100 + 70 * d
    private fun box(text: String, cx: Int, y: Int, width: Int = 30) =
        TextBox(text, cx - width / 2, y, cx + width / 2, y + 20)
    private fun header(vararg days: Int) = days.map { box("$it", colX(it), 0) }
    private fun name(text: String = "홍길동", y: Int = 100) = box(text, 30, y, width = 50)
    private fun cell(text: String, d: Int, y: Int = 100) = box(text, colX(d), y)

    private fun parse(boxes: List<TextBox>, myName: String = "홍길동") =
        parseRosterBoxes(boxes, myName, month, defaultShiftTypes)

    private fun ids(boxes: List<TextBox>) = assertIs<RosterParseResult.Found>(parse(boxes)).cells.map { it.shiftTypeId }

    @Test
    fun `근무 글자를 바로 위 날짜 칸에 맞추고, 빠진 칸은 그날만 비워 둔다`() {
        val boxes = header(1, 2, 3, 4) + name() + cell("D", 1) + cell("N", 3) + cell("OFF", 4)
        val cells = assertIs<RosterParseResult.Found>(parse(boxes)).cells
        assertEquals(
            listOf(
                RosterCell(date(1), "D", D),
                RosterCell(date(2), "", null), // 사진에서 못 읽은 칸 → 사용자가 고름
                RosterCell(date(3), "N", N),
                RosterCell(date(4), "OFF", OFF)
            ),
            cells
        )
    }

    @Test
    fun `다른 사람 줄은 무시하고 내 이름 줄만 읽는다`() {
        val boxes = header(1, 2) +
            name("김철수", y = 50) + cell("N", 1, y = 50) + cell("N", 2, y = 50) +
            name() + cell("E", 1) + cell("D", 2)
        assertEquals(listOf(E, D), ids(boxes))
    }

    @Test
    fun `붙어서 한 덩어리로 읽힌 근무는 칸 수만큼 나눈다`() {
        // "OFFOFF" 상자가 2일·3일 칸에 걸쳐 있음
        val merged = TextBox("OFFOFF", colX(2) - 30, 100, colX(3) + 30, 120)
        val boxes = header(1, 2, 3) + name() + cell("D", 1) + merged
        assertEquals(listOf(D, OFF, OFF), ids(boxes))
    }

    @Test
    fun `머리글 날짜 숫자가 몇 개 빠져도 간격으로 칸 위치를 채운다`() {
        val boxes = header(1, 2, 4, 6) + name() + (1..7).map { cell("D", it) }
        val cells = assertIs<RosterParseResult.Found>(parse(boxes)).cells
        assertEquals((1..7).map { date(it) }, cells.map { it.date }) // 7일 머리글이 없어도 칸 위치는 이어짐
        assertEquals(List(7) { D }, cells.map { it.shiftTypeId })
    }

    @Test
    fun `말일을 넘는 칸은 버리고 개수를 알려준다`() {
        val boxes = header(28, 29, 30) + name() + (28..32).map { cell("N", it) }
        val result = assertIs<RosterParseResult.Found>(parse(boxes))
        assertEquals(date(30), result.cells.last().date)
        assertEquals(2, result.extraCount)
    }

    @Test
    fun `이름이 두 상자로 나뉘어 읽혀도 찾는다`() {
        val boxes = header(1, 2) + box("홍", 10, 100, width = 16) + box("길동", 40, 100, width = 30) +
            cell("D", 1) + cell("E", 2)
        assertEquals(listOf(D, E), ids(boxes))
    }

    @Test
    fun `이름을 못 찾으면 NameNotFound`() {
        val boxes = header(1, 2) + name("김철수") + cell("D", 1)
        assertEquals(RosterParseResult.NameNotFound, parse(boxes))
    }

    @Test
    fun `날짜 머리글이 없으면 줄로 묶어 차례대로 읽는다`() {
        val boxes = listOf(name(), box("D", 200, 100), box("E", 260, 100))
        assertEquals(listOf(D, E), ids(boxes))
    }

    @Test
    fun `붙은 글자 나누기 - 근무 약어로만 나뉘면 나누고 아니면 그대로`() {
        assertEquals(listOf("OFF", "OFF"), splitShiftCodes("OFFOFF", defaultShiftTypes))
        assertEquals(listOf("D", "D", "E"), splitShiftCodes("DDE", defaultShiftTypes))
        assertEquals(listOf("D"), splitShiftCodes("D", defaultShiftTypes))
        assertEquals(listOf("FF"), splitShiftCodes("FF", defaultShiftTypes))     // 모르는 글자
        assertEquals(listOf("휴무"), splitShiftCodes("휴무", defaultShiftTypes)) // 근무 이름은 한 칸
    }
}
