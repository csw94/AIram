package com.wakeuphero.app

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue

// 숫자만 치면 format(core의 formatTimeInput / formatDateInput)이 ":"·"-"를 끼워 넣는 입력 칸
// 기호가 끼어들면 커서 위치가 어긋나므로 커서는 항상 맨 끝에 둔다 (짧은 칸이라 충분)
@Composable
fun FormattedField(
    text: String,
    onTextChange: (String) -> Unit,
    format: (String) -> String,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = TextFieldValue(text, TextRange(text.length)),
        onValueChange = { onTextChange(format(it.text)) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), // 숫자 키패드
        singleLine = true,
        modifier = modifier
    )
}
