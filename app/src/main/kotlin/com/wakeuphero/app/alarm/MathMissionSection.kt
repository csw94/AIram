package com.wakeuphero.app.alarm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakeuphero.core.MathMission
import com.wakeuphero.core.generateMathProblems
import kotlin.random.Random

// 미션 알람(A1): 문제를 모두 맞히면 onDone (= 알람 끄기)
// 씨앗·푼 개수는 rememberSaveable → 화면을 돌려도 같은 문제·진행 상태 유지
@Composable
fun MathMissionSection(onDone: () -> Unit) {
    val seed by rememberSaveable { mutableStateOf(Random.nextLong()) }
    var solved by rememberSaveable { mutableIntStateOf(0) }
    var answer by rememberSaveable { mutableStateOf("") }
    var wrong by rememberSaveable { mutableStateOf(false) }
    val problems = remember(seed) { generateMathProblems(Random(seed)) }
    val mission = MathMission(problems, solved)

    fun submit() {
        val next = mission.submit(answer)
        wrong = next.solved == mission.solved // 그대로면 틀린 것
        solved = next.solved
        answer = ""
        if (next.isDone) onDone()
    }

    val problem = mission.current ?: return
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("끄려면 문제를 풀어요 (${solved + 1}/${problems.size})", style = MaterialTheme.typography.titleMedium)
        Text(problem.text, fontSize = 36.sp)
        OutlinedTextField(
            value = answer,
            onValueChange = { text -> answer = text.filter { it.isDigit() }.take(4); wrong = false },
            label = { Text("답") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }), // 키패드의 [완료]로도 제출
            modifier = Modifier.fillMaxWidth()
        )
        if (wrong) Text("틀렸어요. 다시 풀어 보세요", color = MaterialTheme.colorScheme.error)
        Button(onClick = { submit() }, enabled = answer.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Text("확인", fontSize = 20.sp)
        }
    }
}
