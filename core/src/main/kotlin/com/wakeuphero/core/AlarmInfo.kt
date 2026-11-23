package com.wakeuphero.core

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// 다음 알람 하나에 대한 자세한 정보 (알람 화면에 보여줄 내용)
data class AlarmInfo(
    val time: LocalDateTime, // 울릴 시각
    val workDay: WorkDay,    // 어느 날 근무의 알람인지 (자정 전 알람이면 다음 날 근무일 수 있음)
    val shiftType: ShiftType,
    val order: Int,          // 몇 번째 알람인지 (1부터)
    val total: Int           // 그 근무의 알람 총 개수
)

// 현재 시각(now) "이후" ~ 한 달 뒤까지 중 가장 빠른 알람의 자세한 정보 (없으면 null)
// 근무 유형이나 알람 규칙을 못 찾으면 그날은 알람 없음
fun nextAlarmInfo(
    now: LocalDateTime,
    schedule: List<WorkDay>,
    shiftTypes: List<ShiftType>,
    rules: List<AlarmRule>
): AlarmInfo? {
    val limit = now.plusMonths(1)
    return workAlarmInfos(schedule, shiftTypes, rules)
        .filter { it.time > now && it.time <= limit } // 방금 울린 알람(now와 같음)은 제외
        .minByOrNull { it.time }
}

// 근무표 전체의 근무 알람 (기간 제한 없음) — 근무 유형이나 알람 규칙을 못 찾으면 그날은 알람 없음
internal fun workAlarmInfos(
    schedule: List<WorkDay>,
    shiftTypes: List<ShiftType>,
    rules: List<AlarmRule>
): List<AlarmInfo> =
    schedule
        .flatMap { workDay ->
            val shiftType = shiftTypes.find { it.id == workDay.shiftTypeId } ?: return@flatMap emptyList()
            val rule = rules.find { it.shiftTypeId == workDay.shiftTypeId } ?: return@flatMap emptyList()
            val times = alarmTimesFor(workDay, shiftType, rule)
            // 시각마다 "몇 번째인지"를 함께 묶어 둔다
            times.mapIndexed { index, time -> AlarmInfo(time, workDay, shiftType, index + 1, times.size) }
        }

private val workStartFormat = DateTimeFormatter.ofPattern("MM/dd(E) HH:mm", Locale.KOREAN)

// 알람 화면·알림에 쓸 글: 제목 "주간 근무", 내용 "12/01(화) 07:00 출근 · 1/3번째 알람"
fun alarmTitle(info: AlarmInfo): String = "${info.shiftType.name} 근무"

fun alarmText(info: AlarmInfo): String {
    val workStart = info.shiftType.startTime?.let { info.workDay.date.atTime(it) }
    val start = workStart?.format(workStartFormat)?.let { "$it 출근 · " } ?: ""
    return "$start${info.order}/${info.total}번째 알람"
}
