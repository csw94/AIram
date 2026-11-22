package com.wakeuphero.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wakeuphero.core.WorkDay
import com.wakeuphero.core.defaultAlarmRules
import com.wakeuphero.core.defaultShiftTypes
import com.wakeuphero.core.fillPattern
import com.wakeuphero.core.nextAlarm
import com.wakeuphero.core.nextAlarmMessage
import java.time.LocalDate
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 아직 저장 기능이 없어서, 오늘부터 한 달간 예시 패턴(D D E E N N OFF OFF)을 채워서 보여준다
        val today = LocalDate.now()
        val schedule = fillPattern(today, today.plusMonths(1), listOf(1, 1, 2, 2, 3, 3, 4, 4))

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(schedule = schedule, now = LocalDateTime.now())
                }
            }
        }
    }
}

// 홈 화면: 오늘 근무 + 다음 알람
@Composable
fun HomeScreen(schedule: List<WorkDay>, now: LocalDateTime) {
    val todayWork = schedule.find { it.date == now.toLocalDate() }
    val todayShift = todayWork?.let { wd -> defaultShiftTypes.find { it.id == wd.shiftTypeId } }
    val next = nextAlarm(now, schedule, defaultShiftTypes, defaultAlarmRules)

    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("일어나세요용사여 ⏰", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = if (todayShift == null) "오늘: 근무 없음"
            else "오늘: ${todayShift.name} (${todayShift.shortName})" +
                (todayShift.startTime?.let { " · 출근 $it" } ?: ""),
            style = MaterialTheme.typography.titleMedium
        )
        Text(nextAlarmMessage(next), style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    val start = LocalDate.of(2026, 12, 1)
    MaterialTheme {
        HomeScreen(
            schedule = fillPattern(start, start.plusDays(30), listOf(1, 1, 2, 2, 3, 3, 4, 4)),
            now = start.atTime(5, 0)
        )
    }
}
