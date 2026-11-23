package com.wakeuphero.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MathMissionTest {
    @Test
    fun `문제는 3개이고 덧셈과 곱셈이 번갈아 나온다`() {
        val problems = generateMathProblems(Random(1))
        assertEquals(3, problems.size)
        assertEquals(listOf('+', '×', '+'), problems.map { it.op })
    }

    @Test
    fun `덧셈은 두 자리끼리 곱셈은 2부터 9까지`() {
        repeat(500) { seed ->
            generateMathProblems(Random(seed)).forEach { p ->
                val range = if (p.op == '+') 10..89 else 2..9
                assertTrue(p.a in range && p.b in range, "범위 밖: $p")
            }
        }
    }

    @Test
    fun `정답과 문제 글`() {
        assertEquals(85, MathProblem(47, 38, '+').answer)
        assertEquals(56, MathProblem(7, 8, '×').answer)
        assertEquals("47 + 38 = ?", MathProblem(47, 38, '+').text)
    }

    @Test
    fun `같은 씨앗이면 같은 문제 (화면을 돌려도 문제가 안 바뀌게)`() {
        assertEquals(generateMathProblems(Random(42)), generateMathProblems(Random(42)))
    }

    @Test
    fun `맞히면 다음 문제로 넘어가고 틀리면 그대로`() {
        val mission = MathMission(listOf(MathProblem(10, 20, '+'), MathProblem(3, 4, '×')))
        val wrong = mission.submit("31")
        assertEquals(0, wrong.solved)
        val right = mission.submit(" 30 ")
        assertEquals(1, right.solved)
        assertEquals(MathProblem(3, 4, '×'), right.current)
        assertFalse(right.isDone)
    }

    @Test
    fun `숫자가 아니거나 빈 답은 틀린 것`() {
        val mission = MathMission(listOf(MathProblem(10, 20, '+')))
        assertEquals(0, mission.submit("").solved)
        assertEquals(0, mission.submit("삼십").solved)
    }

    @Test
    fun `모두 맞히면 끝 그 뒤 답은 무시`() {
        val done = MathMission(listOf(MathProblem(1, 1, '+'))).submit("2")
        assertTrue(done.isDone)
        assertEquals(null, done.current)
        assertEquals(done, done.submit("2"))
    }
}
