package com.wakeuphero.core

import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TextRowsTest {

    // 글자 상자 하나: 가운데 x, 위쪽 y, 높이 20, 너비 30
    private fun box(text: String, x: Int, y: Int, height: Int = 20) =
        TextBox(text, left = x - 15, top = y, right = x + 15, bottom = y + height)

    @Test
    fun `같은 줄의 글자는 왼쪽부터 띄어쓰기로 잇는다`() {
        val boxes = listOf(box("E", 200, 100), box("홍길동", 50, 100), box("D", 120, 100))
        assertEquals("홍길동 D E", rowsFromBoxes(boxes))
    }

    @Test
    fun `높이가 다른 줄은 위에서부터 한 줄씩 나눈다`() {
        val boxes = listOf(
            box("N", 120, 200), box("김철수", 50, 200),
            box("D", 120, 100), box("홍길동", 50, 100)
        )
        assertEquals("홍길동 D\n김철수 N", rowsFromBoxes(boxes))
    }

    @Test
    fun `사진이 조금 기울어 높이가 몇 칸씩 어긋나도 같은 줄로 본다`() {
        val boxes = listOf(box("홍길동", 50, 100), box("D", 120, 104), box("E", 200, 108), box("N", 280, 112))
        assertEquals("홍길동 D E N", rowsFromBoxes(boxes))
    }

    @Test
    fun `글자 상자가 없으면 빈 글자`() {
        assertEquals("", rowsFromBoxes(emptyList()))
    }

    @Test
    fun `묶은 줄은 근무표 해석에 그대로 쓸 수 있다`() {
        val boxes = listOf(
            box("1", 120, 50), box("2", 200, 50),
            box("홍길동", 50, 100), box("D", 120, 101), box("0FF", 200, 99),
            box("김철수", 50, 150), box("N", 120, 150), box("N", 200, 150)
        )
        val result = parseRosterText(rowsFromBoxes(boxes), "홍길동", YearMonth.of(2026, 12), defaultShiftTypes)
        assertEquals(listOf(1, 4), assertIs<RosterParseResult.Found>(result).cells.map { it.shiftTypeId })
    }
}
