package com.wakeuphero.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wakeuphero.core.RosterCell
import com.wakeuphero.core.RosterParseResult
import com.wakeuphero.core.ShiftType
import com.wakeuphero.core.defaultShiftTypes
import com.wakeuphero.core.parseRosterText
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

// S8 근무표 입력 (1단계: 글자 붙여넣기 → 확인·수정 → 저장)
// 다음 단계에서 [사진에서 읽기]가 붙으면, 사진에서 뽑은 글자가 같은 칸에 들어간다
@Composable
fun RosterImportScreen(
    initialMonth: YearMonth,
    initialName: String,
    shiftTypes: List<ShiftType>,
    onSave: (name: String, cells: List<RosterCell>) -> Unit,
    onClose: () -> Unit
) {
    var month by rememberSaveable { mutableStateOf(initialMonth) }
    var name by rememberSaveable { mutableStateOf(initialName) }
    var text by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<RosterParseResult?>(null) } // 해석 결과 (확인 화면)
    var cells by remember { mutableStateOf<List<RosterCell>>(emptyList()) } // 사용자가 고친 칸들
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    fun parse() {
        val r = parseRosterText(text, name, month, shiftTypes)
        result = r
        cells = (r as? RosterParseResult.Found)?.cells ?: emptyList()
    }

    Column(
        modifier = Modifier.padding(16.dp).imePadding().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextButton(onClick = onClose) { Text("← 달력") }
        Text("근무표 글자로 입력", style = MaterialTheme.typography.titleLarge)
        Text(
            "근무표 글자를 붙여넣으면 내 이름 줄의 근무를 1일부터 차례로 읽어요. 저장 전에 한 칸씩 확인할 수 있어요.",
            style = MaterialTheme.typography.bodySmall
        )

        // 어느 달 근무표인지 (달을 바꾸면 다시 읽어야 함)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { month = month.minusMonths(1); result = null }) { Text("◀") }
            Text(
                "${month.year}년 ${month.monthValue}월",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            TextButton(onClick = { month = month.plusMonths(1); result = null }) { Text("▶") }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it; result = null },
            label = { Text("내 이름") },
            supportingText = { Text("비우면 붙여넣은 글자 전체를 내 근무로 봐요") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; result = null },
            label = { Text("근무표 글자") },
            placeholder = { Text("예) 홍길동 D D E E N N OFF OFF ...") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { parse() }, enabled = text.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text("읽기")
        }

        when (val r = result) {
            null -> {}
            RosterParseResult.NameNotFound -> ErrorText("'${name.trim()}' 이름이 있는 줄을 찾지 못했어요. 이름을 확인하거나 비워 보세요.")
            RosterParseResult.NoCodes -> ErrorText("이름 뒤에 근무 글자가 없어요.")
            is RosterParseResult.Found -> {
                val unknown = cells.count { it.shiftTypeId == null }
                Text(
                    "${cells.size}일 읽음" + if (unknown > 0) " · 모르는 칸 ${unknown}개 (눌러서 고르기, 안 고르면 저장 안 함)" else "",
                    fontWeight = FontWeight.Bold
                )
                if (r.extraCount > 0) {
                    ErrorText("${month.monthValue}월은 ${month.lengthOfMonth()}일까지라 뒤의 ${r.extraCount}칸은 버렸어요.")
                }
                cells.forEachIndexed { i, cell ->
                    RosterCellRow(cell, shiftTypes.find { it.id == cell.shiftTypeId }) { editingIndex = i }
                }
                Text(
                    "저장하면 이 날짜들의 기존 근무가 바뀌어요.",
                    style = MaterialTheme.typography.bodySmall
                )
                Button(
                    onClick = { onSave(name, cells); onClose() },
                    enabled = cells.any { it.shiftTypeId != null },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("근무표에 저장") }
            }
        }
    }

    // 한 칸 고치기: 근무 고르기 또는 이 날은 저장 안 함
    editingIndex?.let { i ->
        val cell = cells[i]
        AlertDialog(
            onDismissRequest = { editingIndex = null },
            title = { Text("${cell.date.format(cellDateFormat)} · 읽은 글자 '${cell.code}'") },
            text = {
                Column {
                    shiftTypes.forEach { shift ->
                        val mark = if (shift.id == cell.shiftTypeId) " ✓" else ""
                        TextButton(
                            onClick = {
                                cells = cells.toMutableList().also { it[i] = cell.copy(shiftTypeId = shift.id) }
                                editingIndex = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("${shift.name} (${shift.shortName})$mark") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { editingIndex = null }) { Text("닫기") } },
            dismissButton = {
                TextButton(onClick = {
                    cells = cells.toMutableList().also { it[i] = cell.copy(shiftTypeId = null) }
                    editingIndex = null
                }) { Text("이 날은 저장 안 함") }
            }
        )
    }
}

private val cellDateFormat = DateTimeFormatter.ofPattern("M/d (E)", Locale.KOREAN)

@Composable
private fun ErrorText(message: String) = Text(message, color = MaterialTheme.colorScheme.error)

// 확인 목록 한 줄: 날짜 · 읽은 글자 → 근무 (근무 색 배경, 모르면 빨간 글자)
@Composable
private fun RosterCellRow(cell: RosterCell, shift: ShiftType?, onClick: () -> Unit) {
    val background = shift?.let { hexToColor(it.color).copy(alpha = 0.45f) }
        ?: MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(background),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(cell.date.format(cellDateFormat), modifier = Modifier.weight(1f).padding(start = 12.dp))
        Text("'${cell.code}' →", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = onClick, modifier = Modifier.weight(1.2f).padding(4.dp)) {
            Text(
                shift?.let { "${it.name} (${it.shortName})" } ?: "고르기",
                color = if (shift == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 640)
@Composable
private fun RosterImportScreenPreview() {
    WakeUpHeroTheme {
        RosterImportScreen(
            initialMonth = YearMonth.of(2026, 12),
            initialName = "홍길동",
            shiftTypes = defaultShiftTypes,
            onSave = { _, _ -> },
            onClose = {}
        )
    }
}
