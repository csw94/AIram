package com.wakeuphero.core

import java.time.LocalDate
import java.time.YearMonth

// S8 근무표 사진 입력의 "글자 → 근무표" 부분
// 사진에서 글자를 뽑는 방법(붙여넣기, OCR 등)과 상관없이, 뽑힌 글자를 여기서 해석한다.
//   예) "홍길동 D E N OFF ..."  →  12/1 D, 12/2 E, 12/3 N, 12/4 OFF ...

// 해석한 한 칸: 날짜, 원래 글자, 알아본 근무 유형 id (모르면 null → 사용자가 확인 화면에서 고름)
data class RosterCell(val date: LocalDate, val code: String, val shiftTypeId: Int?)

sealed class RosterParseResult {
    // cells: 1일부터 차례로, extraCount: 말일을 넘어 버려진 칸 수
    data class Found(val cells: List<RosterCell>, val extraCount: Int) : RosterParseResult()
    data object NameNotFound : RosterParseResult() // 이름이 있는 줄이 없음
    data object NoCodes : RosterParseResult()      // 근무 칸이 하나도 없음
}

// 휴무를 적는 흔한 다른 표기
private val offAliases = setOf("O", "/", "휴", "휴무")

// text: 인식된 글자 전체, name: 내 이름 (비우면 모든 줄을 이어서 내 근무표로 봄), month: 그 근무표의 달
fun parseRosterText(text: String, name: String, month: YearMonth, shiftTypes: List<ShiftType>): RosterParseResult {
    val row: String = if (name.isBlank()) {
        text
    } else {
        // 이름 글자 사이에 띄어쓰기가 있어도 찾기 (OCR이 "홍 길동"처럼 읽는 경우)
        val pattern = Regex(name.filterNot { it.isWhitespace() }.map { Regex.escape(it.toString()) }.joinToString("\\s*"))
        text.lines().firstNotNullOfOrNull { line -> pattern.find(line)?.let { line.substring(it.range.last + 1) } }
            ?: return RosterParseResult.NameNotFound
    }

    val codes = row.split(Regex("[\\s,|]+")).filter { it.isNotEmpty() }
    if (codes.isEmpty()) return RosterParseResult.NoCodes

    val days = month.lengthOfMonth()
    val cells = codes.take(days).mapIndexed { i, code ->
        RosterCell(month.atDay(i + 1), code, matchShiftCode(code, shiftTypes))
    }
    return RosterParseResult.Found(cells, extraCount = (codes.size - days).coerceAtLeast(0))
}

// 글자 하나를 근무 유형 id로: 약어(대소문자 무시) → 근무 이름 → 휴무 다른 표기 순. 모르면 null
// OCR이 O를 숫자 0으로 읽는 일이 많아서 0은 O로 바꿔 본다 ("0FF" → "OFF")
fun matchShiftCode(code: String, shiftTypes: List<ShiftType>): Int? {
    val normalized = code.trim().uppercase().replace('0', 'O')
    shiftTypes.find { it.shortName.uppercase() == normalized }?.let { return it.id }
    shiftTypes.find { it.name == code.trim() }?.let { return it.id }
    if (normalized in offAliases) return shiftTypes.firstOrNull { it.isOff }?.id
    return null
}

// 확인이 끝난 칸들을 근무표에 넣는다: 근무를 알아본 칸만 바꾸고, 모르는 칸·다른 날은 그대로
fun applyRoster(schedule: List<WorkDay>, cells: List<RosterCell>): List<WorkDay> =
    cells.fold(schedule) { acc, cell ->
        cell.shiftTypeId?.let { setShift(acc, cell.date, it) } ?: acc
    }
