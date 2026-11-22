package com.wakeuphero.core

import java.time.LocalDate

// 근무 패턴(근무 유형 id 목록)을 start ~ end(끝날 포함)까지 반복해서 날짜별 근무로 펼친다
// 예) [D, D, OFF], 12/1 ~ 12/5 → 12/1 D, 12/2 D, 12/3 OFF, 12/4 D, 12/5 D
fun fillPattern(start: LocalDate, end: LocalDate, pattern: List<Int>): List<WorkDay> {
    if (pattern.isEmpty() || start > end) return emptyList()

    val result = mutableListOf<WorkDay>()
    var date = start
    var i = 0
    while (date <= end) {
        // i % pattern.size → 패턴 끝에 도달하면 다시 처음으로 (0, 1, 2, 0, 1, 2, ...)
        result += WorkDay(date, pattern[i % pattern.size])
        date = date.plusDays(1)
        i++
    }
    return result
}
