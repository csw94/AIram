package com.wakeuphero.core

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

private const val MAX_SHORT_NAME = 5                         // 달력 한 칸에 들어갈 만큼
private val RESERVED_WORDS = setOf("PATTERN", "SKIP")         // 근무표 파일 명령어
private val COLOR_REGEX = Regex("^#[0-9A-Fa-f]{6}$")          // #RRGGBB

// 근무 유형 입력값 검사 → 문제 목록 (비어 있으면 저장 가능)
fun validateShiftType(type: ShiftType, existing: List<ShiftType>): List<String> {
    val problems = mutableListOf<String>()
    val short = type.shortName.trim().uppercase()

    if (type.name.isBlank()) problems += "이름을 입력해 주세요"

    when {
        short.isEmpty() -> problems += "약어를 입력해 주세요"
        short.contains(Regex("\\s")) -> problems += "약어에는 띄어쓰기를 쓸 수 없어요"
        short.length > MAX_SHORT_NAME -> problems += "약어는 ${MAX_SHORT_NAME}글자까지 쓸 수 있어요"
        short in RESERVED_WORDS -> problems += "'$short'는 근무표 명령어라 약어로 쓸 수 없어요"
        // 자기 자신(같은 id)은 빼고 약어가 겹치는지 확인
        existing.any { it.id != type.id && it.shortName.uppercase() == short } ->
            problems += "약어 '$short'는 이미 다른 근무 유형이 쓰고 있어요"
    }

    if (!COLOR_REGEX.matches(type.color)) problems += "색상 형식이 올바르지 않아요 (예: #FFD54F)"

    return problems
}

// 새 근무 유형에 줄 id (지금 있는 것 중 가장 큰 id + 1)
fun nextShiftTypeId(existing: List<ShiftType>): Int = (existing.maxOfOrNull { it.id } ?: 0) + 1

// "7:00"과 "07:00" 둘 다 읽힘. STRICT: "24:00"처럼 범위를 벗어난 값은 너그럽게 넘기지 않고 오류로
private val timeFormat = DateTimeFormatter.ofPattern("H:mm").withResolverStyle(ResolverStyle.STRICT)

// "7:00", "07:00", "23:30" → LocalTime, 형식이 틀리면 null
fun parseTimeText(text: String): LocalTime? =
    try {
        LocalTime.parse(text.trim(), timeFormat)
    } catch (e: DateTimeParseException) {
        null
    }

// 출근 시간과 알람 규칙으로 울릴 시각 미리보기 (예: 07:00, 90분 전·10분 간격·3번 → 05:30, 05:40, 05:50)
// 아무 날짜나 하나 잡아서 alarmTimesFor로 계산한 뒤 시각만 꺼낸다 → 계산 규칙이 한 곳에만 있도록
fun alarmPreview(startTime: LocalTime, rule: AlarmRule): List<LocalTime> {
    val anyDay = LocalDate.of(2000, 1, 2)
    val shift = ShiftType(rule.shiftTypeId, "", "", startTime, "#000000")
    return alarmTimesFor(WorkDay(anyDay, shift.id), shift, rule).map { it.toLocalTime() }
}
