package com.wakeuphero.core

import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShiftTypeEditTest {

    private fun type(id: Int, name: String, short: String, color: String = "#FFD54F") =
        ShiftType(id, name, short, LocalTime.of(9, 0), color)

    // ── validateShiftType ─────────────────────────────────────
    @Test
    fun `올바른 새 근무 유형은 문제가 없다`() {
        assertTrue(validateShiftType(type(5, "당직", "DUTY"), defaultShiftTypes).isEmpty())
    }

    @Test
    fun `자기 자신을 고칠 때는 같은 약어여도 괜찮다`() {
        assertTrue(validateShiftType(type(1, "주간근무", "D"), defaultShiftTypes).isEmpty())
    }

    @Test
    fun `이름이나 약어가 비어 있으면 문제`() {
        assertEquals(1, validateShiftType(type(5, "  ", "X"), defaultShiftTypes).size)
        assertEquals(1, validateShiftType(type(5, "당직", ""), defaultShiftTypes).size)
    }

    @Test
    fun `다른 근무 유형과 약어가 겹치면 문제 - 대소문자 무시`() {
        val problems = validateShiftType(type(5, "새 주간", "d"), defaultShiftTypes)
        assertEquals(1, problems.size)
        assertTrue(problems[0].contains("D"))
    }

    @Test
    fun `약어에 띄어쓰기가 있거나 5글자를 넘으면 문제`() {
        // 패턴은 띄어쓰기로 나누고, 달력 한 칸에 들어가야 하니까
        assertEquals(1, validateShiftType(type(5, "당직", "D 2"), defaultShiftTypes).size)
        assertEquals(1, validateShiftType(type(5, "당직", "ABCDEF"), defaultShiftTypes).size)
    }

    @Test
    fun `약어가 PATTERN이나 SKIP이면 문제 - 근무표 파일 명령어와 겹침`() {
        assertEquals(1, validateShiftType(type(5, "건너뛰기", "SKIP"), defaultShiftTypes).size)
    }

    @Test
    fun `색상이 #RRGGBB 형식이 아니면 문제`() {
        assertEquals(1, validateShiftType(type(5, "당직", "DT", color = "노랑"), defaultShiftTypes).size)
    }

    // ── nextShiftTypeId ───────────────────────────────────────
    @Test
    fun `새 id는 가장 큰 id 다음 번호`() {
        assertEquals(5, nextShiftTypeId(defaultShiftTypes))
        assertEquals(1, nextShiftTypeId(emptyList()))
    }

    // ── parseTimeText ─────────────────────────────────────────
    @Test
    fun `시간 글자를 읽는다`() {
        assertEquals(LocalTime.of(7, 0), parseTimeText("7:00"))
        assertEquals(LocalTime.of(7, 0), parseTimeText(" 07:00 "))
        assertEquals(LocalTime.of(23, 30), parseTimeText("23:30"))
    }

    @Test
    fun `시간 형식이 틀리면 null`() {
        assertNull(parseTimeText("24:00"))
        assertNull(parseTimeText("7시"))
        assertNull(parseTimeText(""))
    }

    // ── alarmPreview ──────────────────────────────────────────
    @Test
    fun `알람 미리보기`() {
        val rule = AlarmRule(1, minutesBefore = 90, intervalMinutes = 10, repeatCount = 3)
        assertEquals(
            listOf(LocalTime.of(5, 30), LocalTime.of(5, 40), LocalTime.of(5, 50)),
            alarmPreview(LocalTime.of(7, 0), rule)
        )
    }

    @Test
    fun `자정을 넘는 미리보기도 시각만 보여준다`() {
        val rule = AlarmRule(1, minutesBefore = 60, intervalMinutes = 30, repeatCount = 2)
        assertEquals(listOf(LocalTime.of(23, 30), LocalTime.of(0, 0)), alarmPreview(LocalTime.of(0, 30), rule))
    }
}
