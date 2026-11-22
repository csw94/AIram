package com.wakeuphero.core

import java.time.LocalDate

// 근무표가 before → after 로 바뀔 때 저장소(DB)에 반영해야 할 것
data class ScheduleDiff(
    val upserts: List<WorkDay>,   // 새로 생기거나 내용이 바뀐 근무 (넣거나 덮어쓰기)
    val deletions: List<LocalDate> // 없어진 날짜 (지우기)
)

// 전체를 지우고 다시 쓰는 대신, 바뀐 부분만 골라서 DB 작업을 줄인다
fun scheduleDiff(before: List<WorkDay>, after: List<WorkDay>): ScheduleDiff {
    val beforeByDate = before.associateBy { it.date } // 날짜 → 근무 (빨리 찾기용)
    val afterDates = after.map { it.date }.toSet()

    // data class는 == 로 모든 값(날짜·근무·이번만 끄기)을 한 번에 비교할 수 있다
    val upserts = after.filter { beforeByDate[it.date] != it }.sortedBy { it.date }
    val deletions = before.map { it.date }.filter { it !in afterDates }.sorted()
    return ScheduleDiff(upserts, deletions)
}
