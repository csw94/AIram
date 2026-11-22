package com.wakeuphero.core

import java.time.LocalDate
import java.time.YearMonth

// 근무표 편집 함수들: 원본은 그대로 두고 "바뀐 새 근무표"를 돌려준다 (항상 날짜순)

// 그 날짜의 근무를 바꾸거나 새로 넣는다 (근무가 바뀌면 "이번만 끄기"는 해제)
fun setShift(schedule: List<WorkDay>, date: LocalDate, shiftTypeId: Int): List<WorkDay> =
    (removeShift(schedule, date) + WorkDay(date, shiftTypeId)).sortedBy { it.date }

// 그 날짜의 근무를 지운다
fun removeShift(schedule: List<WorkDay>, date: LocalDate): List<WorkDay> =
    schedule.filter { it.date != date }

// 그 날짜의 "이번만 끄기"를 켜고/끈다 (근무가 없으면 그대로)
fun toggleSkip(schedule: List<WorkDay>, date: LocalDate): List<WorkDay> =
    schedule.map { if (it.date == date) it.copy(alarmSkipped = !it.alarmSkipped) else it }

// start ~ end 기간을 패턴으로 다시 채운다 (기간 밖 근무는 그대로)
fun applyPattern(schedule: List<WorkDay>, start: LocalDate, end: LocalDate, pattern: List<Int>): List<WorkDay> {
    val outside = schedule.filter { it.date < start || it.date > end }
    return (outside + fillPattern(start, end, pattern)).sortedBy { it.date }
}

// "D D E E N N OFF OFF" 같은 글자를 근무 유형 id 목록으로 (모르는 약어가 있거나 비어 있으면 null)
fun parsePatternCodes(text: String, shiftTypes: List<ShiftType>): List<Int>? {
    val codes = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (codes.isEmpty()) return null
    return codes.map { code ->
        shiftTypes.find { it.shortName == code.uppercase() }?.id ?: return null
    }
}

// 달력 칸: 일요일 시작, 한 줄에 7칸, 1일 앞과 말일 뒤 빈칸은 null
fun monthGrid(month: YearMonth): List<List<LocalDate?>> {
    // 1일이 무슨 요일인지 → 앞 빈칸 수 (일=0, 월=1, ..., 토=6)
    val blanks = month.atDay(1).dayOfWeek.value % 7
    val cells: List<LocalDate?> = List(blanks) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    // 7칸씩 자르고, 마지막 주는 뒤를 null로 채워 7칸 맞추기
    return cells.chunked(7).map { week -> week + List(7 - week.size) { null } }
}
