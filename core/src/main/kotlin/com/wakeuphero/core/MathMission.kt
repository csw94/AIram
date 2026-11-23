package com.wakeuphero.core

import kotlin.random.Random

// 미션 알람 (A1): 수학 문제를 다 풀어야 알람이 꺼진다

data class MathProblem(val a: Int, val b: Int, val op: Char) { // op: '+' 또는 '×'
    val answer: Int get() = if (op == '+') a + b else a * b
    val text: String get() = "$a $op $b = ?"
}

// 문제 count개: 덧셈(두 자리끼리)과 곱셈(2~9끼리)을 번갈아
// random을 밖에서 받는 이유: 테스트에서 결과를 고정하고, 화면을 돌려도 같은 씨앗으로 같은 문제를 다시 만들기 위해
fun generateMathProblems(random: Random, count: Int = 3): List<MathProblem> =
    (0 until count).map { i ->
        if (i % 2 == 0) MathProblem(random.nextInt(10, 90), random.nextInt(10, 90), '+')
        else MathProblem(random.nextInt(2, 10), random.nextInt(2, 10), '×')
    }

// 미션 진행 상태: solved개 풀었음
data class MathMission(val problems: List<MathProblem>, val solved: Int = 0) {
    val isDone: Boolean get() = solved >= problems.size
    val current: MathProblem? get() = problems.getOrNull(solved)

    // 답을 내면: 맞으면 다음 문제로, 틀리면(숫자가 아니어도) 그대로
    fun submit(answerText: String): MathMission {
        val problem = current ?: return this
        return if (answerText.trim().toIntOrNull() == problem.answer) copy(solved = solved + 1) else this
    }
}
