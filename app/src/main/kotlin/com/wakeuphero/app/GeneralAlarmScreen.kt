package com.wakeuphero.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wakeuphero.core.GeneralAlarm
import com.wakeuphero.core.dayName
import com.wakeuphero.core.defaultOneTimeDate
import com.wakeuphero.core.parseDateText
import com.wakeuphero.core.parseTimeText
import com.wakeuphero.core.repeatDaysText
import com.wakeuphero.core.validateGeneralAlarm
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

// 일반 알람 목록 (F2·F3): 스위치로 켜기/끄기, 누르면 편집, [+ 추가]
@Composable
fun GeneralAlarmListScreen(
    alarms: List<GeneralAlarm>,
    now: LocalDateTime,
    onBack: () -> Unit,
    onEdit: (Int?) -> Unit, // null = 새로 만들기
    onToggle: (Int, Boolean) -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextButton(onClick = onBack) { Text("← 홈") }
        Text("일반 알람", style = MaterialTheme.typography.titleLarge)
        if (alarms.isEmpty()) Text("근무와 상관없는 알람(시험 날, 운동 등)을 추가해 보세요.")

        alarms.forEach { alarm ->
            Card(modifier = Modifier.fillMaxWidth().clickable { onEdit(alarm.id) }) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(alarm.time.toString(), style = MaterialTheme.typography.headlineSmall)
                        Text(alarm.label.ifBlank { "알람" } + " · " + whenText(alarm, now), style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(checked = alarm.enabled, onCheckedChange = { onToggle(alarm.id, it) })
                }
            }
        }

        Button(onClick = { onEdit(null) }, modifier = Modifier.fillMaxWidth()) { Text("+ 일반 알람 추가") }
    }
}

// "평일" / "12/01 한 번" / "12/01 한 번 (지남)"
private fun whenText(alarm: GeneralAlarm, now: LocalDateTime): String {
    if (alarm.isRepeating) return repeatDaysText(alarm.days)
    val date = alarm.date ?: return "날짜 없음"
    val passed = if (date.atTime(alarm.time) <= now) " (지남)" else ""
    return "${date.monthValue}/${date.dayOfMonth} 한 번$passed"
}

// 일반 알람 편집 (F1·F4·F6): 시각, 이름, 반복 요일(안 고르면 한 번만 → 날짜), 삭제
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeneralAlarmEditScreen(
    original: GeneralAlarm?, // null = 새로 만들기
    now: LocalDateTime,
    onSave: (GeneralAlarm) -> Unit,
    onDelete: (Int) -> Unit,
    onClose: () -> Unit
) {
    val defaultTime = LocalTime.of(7, 0)
    var timeText by remember { mutableStateOf((original?.time ?: defaultTime).toString()) }
    var label by remember { mutableStateOf(original?.label ?: "") }
    var days by remember { mutableStateOf(original?.days ?: emptySet()) }
    var dateText by remember {
        mutableStateOf((original?.date ?: defaultOneTimeDate(now, original?.time ?: defaultTime)).toString())
    }

    // ── 입력값으로 결과 만들기 + 문제 찾기 (검사는 core) ──
    val time = parseTimeText(timeText)
    val date = if (days.isEmpty()) parseDateText(dateText) else null
    val alarm = GeneralAlarm(original?.id ?: 0, label.trim(), time ?: defaultTime, days, date, original?.enabled ?: true)
    val problems = buildList {
        if (time == null) add("시각은 07:00 처럼 입력해 주세요")
        if (days.isEmpty() && date == null) add("날짜는 2026-12-01 처럼 입력해 주세요")
        else validateGeneralAlarm(alarm)?.let(::add)
    }

    Column(
        modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextButton(onClick = onClose) { Text("← 목록") }
        Text(if (original == null) "일반 알람 추가" else "일반 알람 고치기", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(timeText, { timeText = it }, label = { Text("시각 (예: 07:00)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(label, { label = it }, label = { Text("이름 (예: 시험, 비워도 돼요)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

        Text("반복 요일 (안 고르면 한 번만)", style = MaterialTheme.typography.titleMedium)
        // FlowRow: 화면이 좁으면 다음 줄로 넘김 (7개가 한 줄에 안 들어가는 휴대폰 대비)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DayOfWeek.entries.forEach { day ->
                FilterChip(
                    selected = day in days,
                    onClick = { days = if (day in days) days - day else days + day },
                    label = { Text(dayName(day)) }
                )
            }
        }

        if (days.isEmpty()) {
            OutlinedTextField(dateText, { dateText = it }, label = { Text("날짜 (예: 2026-12-01)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { dateText = now.toLocalDate().toString() }) { Text("오늘") }
                OutlinedButton(onClick = { dateText = now.toLocalDate().plusDays(1).toString() }) { Text("내일") }
            }
        } else {
            Text("반복: " + repeatDaysText(days))
        }

        problems.forEach { Text("⚠️ $it", color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = { onSave(alarm); onClose() },
            enabled = problems.isEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("저장") }

        if (original != null) {
            OutlinedButton(onClick = { onDelete(original.id); onClose() }, modifier = Modifier.fillMaxWidth()) { Text("삭제") }
        }
    }
}
