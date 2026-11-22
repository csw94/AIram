package com.wakeuphero.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wakeuphero.core.AlarmRule
import com.wakeuphero.core.ShiftType
import com.wakeuphero.core.WorkDay
import com.wakeuphero.core.defaultAlarmRules
import com.wakeuphero.core.defaultShiftTypes
import com.wakeuphero.core.fillPattern
import com.wakeuphero.core.nextAlarm
import com.wakeuphero.core.nextAlarmMessage
import java.time.LocalDate
import java.time.LocalDateTime

// 홈 화면: 오늘·내일 근무 + 다음 알람
@Composable
fun HomeScreen(
    schedule: List<WorkDay>,
    shiftTypes: List<ShiftType>,
    alarmRules: List<AlarmRule>,
    now: LocalDateTime,
    onOpenCalendar: () -> Unit
) {
    val next = nextAlarm(now, schedule, shiftTypes, alarmRules)

    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("일어나세요용사여 ⏰", style = MaterialTheme.typography.headlineMedium)
        Text("오늘: " + describeDay(schedule, shiftTypes, now.toLocalDate()), style = MaterialTheme.typography.titleMedium)
        Text("내일: " + describeDay(schedule, shiftTypes, now.toLocalDate().plusDays(1)))
        Text(nextAlarmMessage(next), style = MaterialTheme.typography.bodyLarge)
        if (schedule.isEmpty()) {
            Text("아직 근무표가 없어요. 달력에서 [패턴으로 채우기]로 시작해 보세요!", style = MaterialTheme.typography.bodyMedium)
        }
        Button(onClick = onOpenCalendar) { Text("근무표 달력") }
    }
}

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
    MaterialTheme {
        HomeScreen(
            schedule = fillPattern(start, start.plusDays(30), listOf(1, 1, 2, 2, 3, 3, 4, 4)),
            shiftTypes = defaultShiftTypes,
            alarmRules = defaultAlarmRules,
            now = start.atTime(5, 0),
            onOpenCalendar = {}
        )
    }
}
