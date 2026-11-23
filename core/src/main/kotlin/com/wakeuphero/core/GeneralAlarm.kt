package com.wakeuphero.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// 일반 알람 (F1): 근무와 상관없는 알람 (예: 시험 날, 운동)
// - days가 비어 있으면 "한 번만" → date 날짜에 울림
// - days가 있으면 그 요일마다 반복 (F6), date는 무시
data class GeneralAlarm(
    val id: Int,
    val label: String,                    // 이름 (비어도 됨 → "알람")
    val time: LocalTime,
    val days: Set<DayOfWeek> = emptySet(),
    val date: LocalDate? = null,
    val enabled: Boolean = true           // F3 켜기/끄기
) {
    val isRepeating: Boolean get() = days.isNotEmpty()
}

// from "이후" ~ until "까지" 사이에 이 알람이 울릴 시각들 (꺼 두었으면 없음)
fun generalAlarmTimes(alarm: GeneralAlarm, from: LocalDateTime, until: LocalDateTime): List<LocalDateTime> {
    if (!alarm.enabled) return emptyList()
    val candidates = if (alarm.isRepeating) {
        generateSequence(from.toLocalDate()) { it.plusDays(1) }
            .takeWhile { it <= until.toLocalDate() }
            .filter { it.dayOfWeek in alarm.days }
            .map { it.atTime(alarm.time) }
            .toList()
    } else {
        listOfNotNull(alarm.date?.atTime(alarm.time))
    }
    return candidates.filter { it > from && it <= until }
}

// 다가오는 알람 하나: 근무 알람이든 일반 알람이든 같은 모양으로 다룬다 (목록 화면·예약에 공용)
sealed interface UpcomingAlarm {
    val time: LocalDateTime
    val title: String
    val text: String

    data class Work(val info: AlarmInfo) : UpcomingAlarm {
        override val time get() = info.time
        override val title get() = alarmTitle(info)
        override val text get() = alarmText(info)
    }

    data class General(val alarm: GeneralAlarm, override val time: LocalDateTime) : UpcomingAlarm {
        override val title get() = alarm.label.ifBlank { "알람" }
        override val text get() = buildString {
            append(time.format(generalTimeFormat))
            if (alarm.isRepeating) append(" · ").append(repeatDaysText(alarm.days))
        }
    }
}

private val generalTimeFormat = DateTimeFormatter.ofPattern("MM/dd(E) HH:mm", Locale.KOREAN)

// now "이후" ~ until "까지"의 모든 알람을 시각순으로 (F2 목록)
// 같은 시각이면 근무 알람이 먼저
fun upcomingAlarms(
    now: LocalDateTime,
    until: LocalDateTime,
    schedule: List<WorkDay>,
    shiftTypes: List<ShiftType>,
    rules: List<AlarmRule>,
    generalAlarms: List<GeneralAlarm>
): List<UpcomingAlarm> {
    val work = workAlarmInfos(schedule, shiftTypes, rules)
        .filter { it.time > now && it.time <= until }
        .map { UpcomingAlarm.Work(it) }
    val general = generalAlarms.flatMap { alarm ->
        generalAlarmTimes(alarm, now, until).map { UpcomingAlarm.General(alarm, it) }
    }
    return (work + general).sortedBy { it.time }
}

// 다음에 예약할 알람 하나 (한 달 안, 없으면 null)
fun nextUpcomingAlarm(
    now: LocalDateTime,
    schedule: List<WorkDay>,
    shiftTypes: List<ShiftType>,
    rules: List<AlarmRule>,
    generalAlarms: List<GeneralAlarm>
): UpcomingAlarm? = upcomingAlarms(now, now.plusMonths(1), schedule, shiftTypes, rules, generalAlarms).firstOrNull()

// 저장 전 검사 (문제 없으면 null)
fun validateGeneralAlarm(alarm: GeneralAlarm): String? = when {
    alarm.label.length > 20 -> "이름은 20글자까지 쓸 수 있어요"
    !alarm.isRepeating && alarm.date == null -> "날짜나 반복 요일을 골라 주세요"
    else -> null
}

// "한 번만" 알람의 기본 날짜: 오늘 그 시각이 아직 안 지났으면 오늘, 지났으면 내일
fun defaultOneTimeDate(now: LocalDateTime, time: LocalTime): LocalDate =
    if (now.toLocalTime() < time) now.toLocalDate() else now.toLocalDate().plusDays(1)

private val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
private val weekend = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

// 반복 요일 글: "매일" / "평일" / "주말" / "월 수 금" (월요일부터)
fun repeatDaysText(days: Set<DayOfWeek>): String = when (days) {
    DayOfWeek.entries.toSet() -> "매일"
    weekdays -> "평일"
    weekend -> "주말"
    else -> days.sorted().joinToString(" ") { dayName(it) }
}

fun dayName(day: DayOfWeek): String = "월화수목금토일"[day.value - 1].toString()

private val dateFormat = DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(java.time.format.ResolverStyle.STRICT)

// "2026-12-01" → 날짜 (모양이 다르거나 없는 날짜면 null). STRICT: 2월 30일 같은 날을 말일로 바꿔 주지 않음
fun parseDateText(text: String): LocalDate? =
    try {
        LocalDate.parse(text.trim(), dateFormat)
    } catch (e: java.time.format.DateTimeParseException) {
        null
    }
