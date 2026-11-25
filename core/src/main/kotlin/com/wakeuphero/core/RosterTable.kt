package com.wakeuphero.core

import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.roundToInt

// S8-2: 사진 글자 상자 → 날짜 칸 위치로 근무 맞추기
// OCR은 칸을 빠뜨리거나("E"를 못 읽음) 붙여 읽기도("OFFOFF") 한다.
// 1일부터 순서대로 맞추면 한 칸만 빠져도 뒤 날짜가 다 밀리므로,
// 머리글 줄의 날짜 숫자(1 2 3 ...) 가로 위치를 기준으로 근무 글자를 "바로 위 날짜"에 맞춘다.
//   → 빠진 칸은 그날만 비고(사용자가 확인 화면에서 고름), 다른 날은 밀리지 않는다.
// 날짜 머리글이 없으면(개인 근무표 등) 줄로 묶어 차례대로 읽는다(parseRosterText).

fun parseRosterBoxes(boxes: List<TextBox>, name: String, month: YearMonth, shiftTypes: List<ShiftType>): RosterParseResult {
    val rows = groupRows(boxes)
    val days = month.lengthOfMonth()
    val columns = dayColumns(rows, days)
    if (columns == null || name.isBlank()) return parseRosterText(rowsFromBoxes(boxes), name, month, shiftTypes)

    // 내 이름 줄과 이름이 끝나는 가로 위치
    val compactName = name.filterNot { it.isWhitespace() }
    var nameRight: Int? = null
    val myRow = rows.firstOrNull { row ->
        val seen = StringBuilder()
        row.firstOrNull { box ->
            seen.append(box.text.filterNot { it.isWhitespace() })
            compactName in seen
        }?.also { nameRight = it.right } != null
    } ?: return RosterParseResult.NameNotFound

    // 이름 오른쪽 상자들 → (붙은 글자는 나눠서) 조각마다 가장 가까운 날짜 칸
    val pieces = myRow.filter { it.centerX > nameRight!! }.flatMap { splitBox(it, shiftTypes) }
    if (pieces.isEmpty()) return RosterParseResult.NoCodes
    val byDay = mutableMapOf<Int, Pair<String, Double>>() // 날짜 → (글자, 칸 가운데와의 거리)
    var extra = 0
    for ((code, x) in pieces) {
        val day = columns.dayAt(x)
        if (day < 1) continue
        if (day > days) { extra++; continue }
        val distance = abs(x - columns.xOf(day))
        if (byDay[day]?.let { it.second <= distance } != true) byDay[day] = code to distance
    }
    if (byDay.isEmpty()) return RosterParseResult.NoCodes

    val lastDay = maxOf(byDay.keys.max(), columns.lastHeaderDay.coerceAtMost(days))
    val cells = (1..lastDay).map { d ->
        val code = byDay[d]?.first ?: ""
        RosterCell(month.atDay(d), code, if (code.isEmpty()) null else matchShiftCode(code, shiftTypes))
    }
    return RosterParseResult.Found(cells, extra)
}

// 붙어서 읽힌 글자를 근무 약어로 나눈다 ("OFFOFF" → OFF, OFF). 그대로 알아볼 수 있거나 못 나누면 한 덩어리
fun splitShiftCodes(text: String, shiftTypes: List<ShiftType>): List<String> {
    if (matchShiftCode(text, shiftTypes) != null) return listOf(text)
    val normalized = text.uppercase().replace('0', 'O')
    val codes = shiftTypes.map { it.shortName.uppercase() }.filter { it.isNotEmpty() }.sortedByDescending { it.length }

    // 앞에서부터 약어를 떼어 보고, 막히면 다른 약어로 다시 (되돌아가기)
    fun split(from: Int): List<String>? {
        if (from == normalized.length) return emptyList()
        for (code in codes) {
            if (normalized.startsWith(code, from)) {
                split(from + code.length)?.let { rest -> return listOf(text.substring(from, from + code.length)) + rest }
            }
        }
        return null
    }
    return split(0)?.takeIf { it.isNotEmpty() } ?: listOf(text)
}

// 상자 하나를 (글자, 가로 위치) 조각들로: 나눈 경우 상자 너비를 글자 수 비율로 나눠 위치를 정한다
private fun splitBox(box: TextBox, shiftTypes: List<ShiftType>): List<Pair<String, Double>> {
    val parts = splitShiftCodes(box.text, shiftTypes)
    if (parts.size == 1) return listOf(box.text to box.centerX)
    val perChar = (box.right - box.left).toDouble() / box.text.length
    var start = box.left.toDouble()
    return parts.map { part ->
        val center = start + perChar * part.length / 2
        start += perChar * part.length
        part to center
    }
}

// 날짜 칸 위치: 머리글에서 읽은 날짜 숫자들(anchors: 날짜 → 가로 위치) + 칸 간격
private class DayColumns(val anchors: Map<Int, Double>, val spacing: Double) {
    val lastHeaderDay: Int get() = anchors.keys.max()

    // 가장 가까운 머리글 숫자에서 간격만큼 세어 날짜를 정한다 (머리글이 빠진 날도 계산됨)
    fun dayAt(x: Double): Int {
        val (day, ax) = anchors.minBy { abs(it.value - x) }
        return day + ((x - ax) / spacing).roundToInt()
    }

    fun xOf(day: Int): Double {
        val (d, ax) = anchors.minBy { abs(it.key - day) }
        return ax + (day - d) * spacing
    }
}

// 날짜 숫자(1~말일)가 가장 많은 줄을 머리글로 본다. 숫자가 2개 미만이거나 간격을 못 구하면 null
private fun dayColumns(rows: List<List<TextBox>>, days: Int): DayColumns? {
    fun dayNumbers(row: List<TextBox>) = row.mapNotNull { box -> box.text.toIntOrNull()?.takeIf { it in 1..days }?.let { it to box.centerX } }
    val header = rows.map { dayNumbers(it) }.maxByOrNull { it.size } ?: return null
    if (header.size < 2) return null
    val anchors = header.groupBy({ it.first }, { it.second }).mapValues { it.value.first() }
    // 이웃한 숫자끼리 (거리 ÷ 날짜 차이)의 중앙값 = 한 칸 너비
    val sorted = anchors.entries.sortedBy { it.key }
    val gaps = sorted.zipWithNext { a, b -> (b.value - a.value) / (b.key - a.key) }.filter { it > 0 }.sorted()
    if (gaps.isEmpty()) return null
    return DayColumns(anchors, gaps[gaps.size / 2])
}
