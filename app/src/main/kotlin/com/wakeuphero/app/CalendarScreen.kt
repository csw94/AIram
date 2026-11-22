package com.wakeuphero.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakeuphero.core.ShiftType
import com.wakeuphero.core.WorkDay
import com.wakeuphero.core.defaultShiftTypes
import com.wakeuphero.core.fillPattern
import com.wakeuphero.core.monthGrid
import com.wakeuphero.core.parsePatternCodes
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

// 근무표 달력 화면
// - ◀ ▶ 로 달 이동
// - 날짜를 누르면 근무 고르기 / 지우기 / 이번만 끄기
// - [패턴으로 채우기] 로 그 달 전체 채우기
@Composable
fun CalendarScreen(
    schedule: List<WorkDay>,
    shiftTypes: List<ShiftType>,
    onBack: () -> Unit,
    onSetShift: (LocalDate, Int) -> Unit,
    onRemoveShift: (LocalDate) -> Unit,
    onToggleSkip: (LocalDate) -> Unit,
    onApplyPattern: (YearMonth, List<Int>) -> Unit,
    initialMonth: YearMonth = YearMonth.now()
) {
    var month by remember { mutableStateOf(initialMonth) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showPatternDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onBack) { Text("← 홈") }

        // 달 이동
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { month = month.minusMonths(1) }) { Text("◀") }
            Text(
                "${month.year}년 ${month.monthValue}월",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            TextButton(onClick = { month = month.plusMonths(1) }) { Text("▶") }
        }

        // 요일 머리글
        Row {
            listOf("일", "월", "화", "수", "목", "금", "토").forEachIndexed { i, name ->
                Text(
                    name,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = when (i) { 0 -> Color(0xFFE53935); 6 -> Color(0xFF1E88E5); else -> Color.Unspecified }
                )
            }
        }

        // 달력 칸 (core의 monthGrid가 7칸씩 나눠 준다)
        monthGrid(month).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { date ->
                    Box(modifier = Modifier.weight(1f).aspectRatio(0.8f)) {
                        if (date != null) {
                            val workDay = schedule.find { it.date == date }
                            val shift = workDay?.let { wd -> shiftTypes.find { it.id == wd.shiftTypeId } }
                            DayCell(date, shift, workDay?.alarmSkipped == true) { selectedDate = date }
                        }
                    }
                }
            }
        }

        Button(onClick = { showPatternDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text("패턴으로 채우기")
        }
        Text("* = 이번만 끄기 (알람 안 울림)", style = MaterialTheme.typography.bodySmall)
    }

    // 날짜를 눌렀을 때
    selectedDate?.let { date ->
        val workDay = schedule.find { it.date == date }
        val shift = workDay?.let { wd -> shiftTypes.find { it.id == wd.shiftTypeId } }
        DayDialog(
            date = date,
            workDay = workDay,
            currentShift = shift,
            shiftTypes = shiftTypes,
            onPick = { onSetShift(date, it); selectedDate = null },
            onRemove = { onRemoveShift(date); selectedDate = null },
            onToggleSkip = { onToggleSkip(date); selectedDate = null },
            onDismiss = { selectedDate = null }
        )
    }

    if (showPatternDialog) {
        PatternDialog(
            month = month,
            shiftTypes = shiftTypes,
            onApply = { onApplyPattern(month, it); showPatternDialog = false },
            onDismiss = { showPatternDialog = false }
        )
    }
}

// 달력 한 칸: 날짜 숫자 + 근무 약어 (근무 색 배경)
@Composable
private fun DayCell(date: LocalDate, shift: ShiftType?, skipped: Boolean, onClick: () -> Unit) {
    val background = shift?.let { hexColor(it.color).copy(alpha = 0.45f) } ?: Color(0x11000000)
    Column(
        modifier = Modifier
            .fillMaxSize() // 칸 전체를 채워서 모든 칸의 크기를 똑같이
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("${date.dayOfMonth}", fontSize = 11.sp)
        Text(
            text = (shift?.shortName ?: "") + if (skipped) "*" else "",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private val dialogDateFormat = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)

// 날짜 눌렀을 때 나오는 창: 근무 고르기 / 지우기 / 이번만 끄기
@Composable
private fun DayDialog(
    date: LocalDate,
    workDay: WorkDay?,
    currentShift: ShiftType?,
    shiftTypes: List<ShiftType>,
    onPick: (Int) -> Unit,
    onRemove: () -> Unit,
    onToggleSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(date.format(dialogDateFormat)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                shiftTypes.forEach { shift ->
                    val time = shift.startTime?.let { " · $it" } ?: ""
                    val mark = if (shift.id == currentShift?.id) " ✓" else ""
                    TextButton(onClick = { onPick(shift.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text("${shift.name} (${shift.shortName})$time$mark")
                    }
                }
                // 이번만 끄기는 알람이 있는 근무에만
                if (workDay != null && currentShift != null && !currentShift.isOff) {
                    TextButton(onClick = onToggleSkip, modifier = Modifier.fillMaxWidth()) {
                        Text(if (workDay.alarmSkipped) "🔔 알람 다시 켜기" else "🔕 이번만 끄기")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
        dismissButton = {
            if (workDay != null) TextButton(onClick = onRemove) { Text("근무 지우기") }
        }
    )
}

// 패턴으로 그 달 전체 채우기
@Composable
private fun PatternDialog(
    month: YearMonth,
    shiftTypes: List<ShiftType>,
    onApply: (List<Int>) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("D D E E N N OFF OFF") }
    val pattern = parsePatternCodes(text, shiftTypes) // 모르는 약어가 있으면 null
    val codes = shiftTypes.joinToString(", ") { it.shortName }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${month.monthValue}월 패턴으로 채우기") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${month.monthValue}월 1일부터 말일까지 패턴을 반복해서 채워요. 이 달에 입력한 근무는 바뀌어요.")
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("패턴 (띄어쓰기로 구분)") },
                    isError = pattern == null,
                    supportingText = { Text(if (pattern == null) "사용할 수 있는 근무: $codes" else "${pattern.size}일 주기") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { pattern?.let(onApply) }, enabled = pattern != null) { Text("채우기") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

// "#FFD54F" → Compose 색
private fun hexColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.LightGray)

@Preview(showBackground = true, heightDp = 640)
@Composable
private fun CalendarScreenPreview() {
    val month = YearMonth.of(2026, 12)
    MaterialTheme {
        CalendarScreen(
            schedule = fillPattern(month.atDay(1), month.atEndOfMonth(), listOf(1, 1, 2, 2, 3, 3, 4, 4)),
            shiftTypes = defaultShiftTypes,
            onBack = {}, onSetShift = { _, _ -> }, onRemoveShift = {}, onToggleSkip = {}, onApplyPattern = { _, _ -> },
            initialMonth = month
        )
    }
}
