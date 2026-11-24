package com.wakeuphero.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wakeuphero.core.AlarmRule
import com.wakeuphero.core.ShiftType
import com.wakeuphero.core.WorkDay
import com.wakeuphero.core.defaultAlarmRules
import com.wakeuphero.core.defaultShiftTypes
import com.wakeuphero.core.fillPattern
import com.wakeuphero.core.GeneralAlarm
import com.wakeuphero.core.upcomingAlarms
import com.wakeuphero.core.UpcomingAlarm
import com.wakeuphero.core.nextAlarmMessage
import java.time.LocalDate
import java.time.LocalDateTime

// 홈 화면: 오늘·내일 근무 + 다음 알람 + 오늘·내일 울릴 알람 목록(F2)
@Composable
fun HomeScreen(
    schedule: List<WorkDay>,
    shiftTypes: List<ShiftType>,
    alarmRules: List<AlarmRule>,
    generalAlarms: List<GeneralAlarm> = emptyList(),
    now: LocalDateTime,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenGeneralAlarms: () -> Unit = {},
    mathMission: Boolean = false,
    onMathMissionChange: (Boolean) -> Unit = {},
    showAlarmStatus: Boolean = true // 미리보기(Preview)에서는 끄기
) {
    // 지금부터 내일 23:59까지 울릴 알람 (근무 + 일반, 시각순). 모레 00:00은 "내일"이 아니므로 제외
    val soon = upcomingAlarms(now, now.toLocalDate().plusDays(1).atTime(23, 59, 59), schedule, shiftTypes, alarmRules, generalAlarms)
    val month = upcomingAlarms(now, now.plusMonths(1), schedule, shiftTypes, alarmRules, generalAlarms)
    val next = month.firstOrNull()?.time
    // 디버그 테스트 알람은 다음 근무 알람의 알람음으로 (A4 확인용)
    val testSound = month.filterIsInstance<UpcomingAlarm.Work>().firstOrNull()?.sound

    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("일어나세요용사여 ⏰", style = MaterialTheme.typography.headlineMedium)
        Text("오늘: " + describeDay(schedule, shiftTypes, now.toLocalDate()), style = MaterialTheme.typography.titleMedium)
        Text("내일: " + describeDay(schedule, shiftTypes, now.toLocalDate().plusDays(1)))
        Text(nextAlarmMessage(next), style = MaterialTheme.typography.bodyLarge)
        if (soon.isNotEmpty()) {
            Text("오늘·내일 알람", style = MaterialTheme.typography.titleMedium)
            soon.take(MAX_SOON).forEach { Text("• ${dayLabel(it.time, now)} ${it.time.toLocalTime()}  ${it.title}") }
            if (soon.size > MAX_SOON) Text("… 외 ${soon.size - MAX_SOON}개")
        }
        if (schedule.isEmpty()) {
            Text("아직 근무표가 없어요. 달력에서 [패턴으로 채우기]로 시작해 보세요!", style = MaterialTheme.typography.bodyMedium)
        }
        Button(onClick = onOpenCalendar) { Text("근무표 달력") }
        OutlinedButton(onClick = onOpenSettings) { Text("근무 유형 설정") }
        OutlinedButton(onClick = onOpenGeneralAlarms) { Text("일반 알람") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("미션 알람", style = MaterialTheme.typography.titleMedium)
                Text("수학 문제 3개를 풀어야 알람이 꺼져요", style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = mathMission, onCheckedChange = onMathMissionChange)
        }
        if (showAlarmStatus) AlarmPermissionSection(testSound)
    }
}

private const val MAX_SOON = 8

private fun dayLabel(time: LocalDateTime, now: LocalDateTime) =
    if (time.toLocalDate() == now.toLocalDate()) "오늘" else "내일"

// "주간 (D) · 출근 07:00" / "휴무 (OFF) 😴" / "근무 없음" + 이번만 끄기 표시
private fun describeDay(schedule: List<WorkDay>, shiftTypes: List<ShiftType>, date: LocalDate): String {
    val workDay = schedule.find { it.date == date } ?: return "근무 없음"
    val shift = shiftTypes.find { it.id == workDay.shiftTypeId } ?: return "근무 없음"
    val time = shift.startTime?.let { " · 출근 $it" } ?: " 😴"
    val skipped = if (workDay.alarmSkipped) " (알람 끔)" else ""
    return "${shift.name} (${shift.shortName})$time$skipped"
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    val start = LocalDate.of(2026, 12, 1)
    WakeUpHeroTheme {
        HomeScreen(
            schedule = fillPattern(start, start.plusDays(30), listOf(1, 1, 2, 2, 3, 3, 4, 4)),
            shiftTypes = defaultShiftTypes,
            alarmRules = defaultAlarmRules,
            now = start.atTime(5, 0),
            onOpenCalendar = {},
            showAlarmStatus = false
        )
    }
}
