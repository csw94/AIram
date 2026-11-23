package com.wakeuphero.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.wakeuphero.core.AlarmRule
import com.wakeuphero.core.DeleteShiftResult
import com.wakeuphero.core.ShiftType
import com.wakeuphero.core.alarmPreview
import com.wakeuphero.core.formatTimeInput
import com.wakeuphero.core.isValid
import com.wakeuphero.core.nextShiftTypeId
import com.wakeuphero.core.parseTimeText
import com.wakeuphero.core.validateShiftType
import java.time.format.DateTimeFormatter

// 근무 유형 목록 화면: 누르면 편집, [+ 추가]로 새로 만들기
@Composable
fun ShiftTypeListScreen(
    shiftTypes: List<ShiftType>,
    alarmRules: List<AlarmRule>,
    onBack: () -> Unit,
    onEdit: (Int?) -> Unit // null = 새로 만들기
) {
    Column(
        modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextButton(onClick = onBack) { Text("← 홈") }
        Text("근무 유형 설정", style = MaterialTheme.typography.titleLarge)

        shiftTypes.forEach { shift ->
            val rule = alarmRules.find { it.shiftTypeId == shift.id }
            Card(modifier = Modifier.fillMaxWidth().clickable { onEdit(shift.id) }) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    ColorDot(shift.color)
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text("${shift.name} (${shift.shortName})", style = MaterialTheme.typography.titleMedium)
                        Text(
                            when {
                                shift.startTime == null -> "휴무 · 알람 없음"
                                rule == null -> "출근 ${shift.startTime} · 알람 규칙 없음"
                                else -> "출근 ${shift.startTime} · ${rule.minutesBefore}분 전부터 ${rule.intervalMinutes}분 간격 ${rule.repeatCount}번"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        Button(onClick = { onEdit(null) }, modifier = Modifier.fillMaxWidth()) { Text("+ 근무 유형 추가") }
    }
}

// 고를 수 있는 색 (#RRGGBB)
private val colorChoices = listOf("#FFD54F", "#FFB74D", "#BA68C8", "#64B5F6", "#81C784", "#E57373", "#BDBDBD")

private val previewFormat = DateTimeFormatter.ofPattern("HH:mm")

// 근무 유형 편집 화면 (새로 만들기 / 고치기 / 삭제)
// 입력값 검사는 전부 core 함수(validateShiftType, parseTimeText, isValid)로 한다
@Composable
fun ShiftTypeEditScreen(
    original: ShiftType?,         // null = 새로 만들기
    originalRule: AlarmRule?,
    allShiftTypes: List<ShiftType>,
    onSave: (ShiftType, AlarmRule?) -> Unit,
    onDelete: (Int) -> DeleteShiftResult,
    onClose: () -> Unit
) {
    val id = original?.id ?: nextShiftTypeId(allShiftTypes)

    // 입력 칸 상태 (글자 그대로 들고 있다가 저장할 때 검사)
    var name by remember { mutableStateOf(original?.name ?: "") }
    var shortName by remember { mutableStateOf(original?.shortName ?: "") }
    var isOff by remember { mutableStateOf(original != null && original.isOff) }
    var timeText by remember { mutableStateOf(original?.startTime?.toString() ?: "09:00") }
    var color by remember { mutableStateOf(original?.color ?: colorChoices[3]) }
    var beforeText by remember { mutableStateOf((originalRule?.minutesBefore ?: 90).toString()) }
    var intervalText by remember { mutableStateOf((originalRule?.intervalMinutes ?: 10).toString()) }
    var countText by remember { mutableStateOf((originalRule?.repeatCount ?: 3).toString()) }
    var deleteMessage by remember { mutableStateOf<String?>(null) }

    // ── 입력값으로 결과 만들기 + 문제 찾기 ──
    val startTime = if (isOff) null else parseTimeText(timeText)
    val shiftType = ShiftType(id, name.trim(), shortName.trim().uppercase(), startTime, color)
    val rule = if (isOff) null else AlarmRule(
        id,
        beforeText.toIntOrNull() ?: 0,
        intervalText.toIntOrNull() ?: 0,
        countText.toIntOrNull() ?: 0
    )
    val problems = buildList {
        addAll(validateShiftType(shiftType, allShiftTypes))
        if (!isOff && startTime == null) add("출근 시간은 07:00 처럼 입력해 주세요")
        if (rule != null && !rule.isValid()) add("알람 규칙: 모든 값은 1 이상, 마지막 알람이 출근 시간보다 먼저 울려야 해요")
    }

    Column(
        modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextButton(onClick = onClose) { Text("← 목록") }
        Text(if (original == null) "근무 유형 추가" else "근무 유형 고치기", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(name, { name = it }, label = { Text("이름 (예: 주간)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(shortName, { shortName = it }, label = { Text("약어 (예: D, 5글자까지)") }, modifier = Modifier.fillMaxWidth())

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("휴무 (알람 없음)", modifier = Modifier.weight(1f))
            Switch(checked = isOff, onCheckedChange = { isOff = it })
        }

        if (!isOff) {
            FormattedField(timeText, { timeText = it }, ::formatTimeInput, "출근 시간 (예: 0700 → 07:00)", Modifier.fillMaxWidth())

            Text("알람 규칙", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(beforeText, { beforeText = it }, "몇 분 전부터", Modifier.weight(1f))
                NumberField(intervalText, { intervalText = it }, "몇 분 간격", Modifier.weight(1f))
                NumberField(countText, { countText = it }, "몇 번", Modifier.weight(1f))
            }
            // 미리보기: 규칙이 올바를 때만
            if (startTime != null && rule != null && rule.isValid()) {
                Text("미리보기: " + alarmPreview(startTime, rule).joinToString("  ") { it.format(previewFormat) })
            }
        }

        Text("색상", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            colorChoices.forEach { c ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(hexToColor(c), CircleShape)
                        .border(if (c == color) 3.dp else 0.dp, Color.Black, CircleShape)
                        .clickable { color = c }
                )
            }
        }

        problems.forEach { Text("⚠️ $it", color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = { onSave(shiftType, rule); onClose() },
            enabled = problems.isEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("저장") }

        if (original != null) {
            OutlinedButton(
                onClick = {
                    when (val result = onDelete(original.id)) {
                        is DeleteShiftResult.Deleted -> onClose()
                        is DeleteShiftResult.InUse -> deleteMessage =
                            "근무표의 ${result.dates.size}일에서 쓰고 있어서 지울 수 없어요 (" +
                                result.dates.take(3).joinToString(", ") { "${it.monthValue}/${it.dayOfMonth}" } +
                                (if (result.dates.size > 3) " 등)" else ")") +
                                ". 근무표에서 먼저 바꿔 주세요."
                        DeleteShiftResult.NotFound -> onClose()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("삭제") }
        }
        deleteMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onChange(text.filter { it.isDigit() }.take(4)) }, // 숫자만
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
    )
}

@Composable
private fun ColorDot(hex: String) {
    Box(modifier = Modifier.size(20.dp).background(hexToColor(hex), CircleShape))
}

// "#FFD54F" → Compose 색
fun hexToColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.LightGray)
