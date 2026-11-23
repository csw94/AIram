package com.wakeuphero.core

import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class GeneralAlarmTest {
    // 2026-12-01 은 화요일
    private val dec1 = LocalDate.of(2026, 12, 1)
    private fun at(day: Int, h: Int, m: Int = 0) = LocalDateTime.of(2026, 12, day, h, m)

    private val dayShift = ShiftType(1, "주간", "D", LocalTime.of(7, 0), "#FFD54F")
    private val dayRule = AlarmRule(1, 60, 10, 2) // 06:00, 06:10

    private fun oneTime(id: Int, date: LocalDate, time: LocalTime, enabled: Boolean = true) =
        GeneralAlarm(id, "시험", time, date = date, enabled = enabled)

    private fun weekly(id: Int, time: LocalTime, vararg days: java.time.DayOfWeek) =
        GeneralAlarm(id, "운동", time, days = days.toSet())

    // ── 하루치·기간 안의 울릴 시각 ──

    @Test
    fun `한 번 울리는 알람은 그 날짜 그 시각에만 울린다`() {
        val alarm = oneTime(1, dec1, LocalTime.of(8, 30))
        assertEquals(listOf(at(1, 8, 30)), generalAlarmTimes(alarm, at(1, 0), at(31, 0)))
    }

    @Test
    fun `반복 알람은 고른 요일마다 울린다`() {
        val alarm = weekly(1, LocalTime.of(9, 0), MONDAY, WEDNESDAY)
        // 12-01(화) ~ 12-08(화): 수 12-02, 월 12-07
        assertEquals(listOf(at(2, 9), at(7, 9)), generalAlarmTimes(alarm, at(1, 0), at(8, 23)))
    }

    @Test
    fun `꺼 둔 알람은 울리지 않는다`() {
        val alarm = oneTime(1, dec1, LocalTime.of(8, 30), enabled = false)
        assertEquals(emptyList(), generalAlarmTimes(alarm, at(1, 0), at(31, 0)))
    }

    @Test
    fun `기간의 시작 시각과 같은 알람은 빼고 끝 시각과 같은 알람은 넣는다`() {
        val alarm = weekly(1, LocalTime.of(9, 0), TUESDAY)
        // 12-01 09:00 은 시작과 같아서 제외, 12-08 09:00 은 끝과 같아서 포함
        assertEquals(listOf(at(8, 9)), generalAlarmTimes(alarm, at(1, 9), at(8, 9)))
    }

    // ── 근무 알람과 합친 목록 ──

    @Test
    fun `다가오는 알람은 근무 알람과 일반 알람을 시각순으로 섞어 보여준다`() {
        val schedule = listOf(WorkDay(dec1, 1))
        val general = listOf(oneTime(7, dec1, LocalTime.of(6, 5)))

        val list = upcomingAlarms(at(1, 0), at(2, 0), schedule, listOf(dayShift), listOf(dayRule), general)

        assertEquals(listOf(at(1, 6), at(1, 6, 5), at(1, 6, 10)), list.map { it.time })
        assertIs<UpcomingAlarm.Work>(list[0])
        assertIs<UpcomingAlarm.General>(list[1])
    }

    @Test
    fun `다음 알람은 한 달 안에서 가장 빠른 것 하나`() {
        val general = listOf(weekly(7, LocalTime.of(5, 0), WEDNESDAY))
        val next = nextUpcomingAlarm(at(1, 0), listOf(WorkDay(dec1, 1)), listOf(dayShift), listOf(dayRule), general)
        assertEquals(at(1, 6), next?.time) // 근무 알람 06:00 이 수요일 05:00 보다 빠름
    }

    @Test
    fun `일반 알람만 있어도 다음 알람이 나온다`() {
        val next = nextUpcomingAlarm(at(1, 10), emptyList(), emptyList(), emptyList(), listOf(weekly(7, LocalTime.of(9, 0), TUESDAY)))
        assertEquals(at(8, 9), next?.time) // 오늘(화) 09:00 은 지났으니 다음 주 화요일
    }

    @Test
    fun `한 달 안에 알람이 없으면 null`() {
        val next = nextUpcomingAlarm(at(1, 10), emptyList(), emptyList(), emptyList(), listOf(oneTime(1, dec1, LocalTime.of(9, 0))))
        assertNull(next)
    }

    // ── 검사 ──

    @Test
    fun `날짜도 반복 요일도 없으면 오류`() {
        assertEquals("날짜나 반복 요일을 골라 주세요", validateGeneralAlarm(GeneralAlarm(1, "", LocalTime.of(9, 0)), at(1, 0)))
    }

    @Test
    fun `이름이 20글자를 넘으면 오류이고 빈 이름은 괜찮다`() {
        assertEquals("이름은 20글자까지 쓸 수 있어요", validateGeneralAlarm(GeneralAlarm(1, "가".repeat(21), LocalTime.of(9, 0), date = dec1), at(1, 0)))
        assertNull(validateGeneralAlarm(GeneralAlarm(1, "", LocalTime.of(9, 0), date = dec1), at(1, 0)))
    }

    @Test
    fun `한 번 알람이 이미 지난 시각이면 오류이고 반복 알람은 괜찮다`() {
        val passed = GeneralAlarm(1, "", LocalTime.of(9, 0), date = dec1)
        assertEquals("이미 지난 시각이에요. 날짜나 시각을 바꿔 주세요", validateGeneralAlarm(passed, at(1, 9)))
        assertNull(validateGeneralAlarm(passed, at(1, 8, 59)))
        assertNull(validateGeneralAlarm(weekly(1, LocalTime.of(9, 0), TUESDAY), at(1, 10)))
    }

    // ── 켜기 ──

    @Test
    fun `지난 한 번 알람을 다시 켜면 다음에 올 그 시각으로 날짜가 옮겨진다`() {
        val passed = oneTime(1, dec1, LocalTime.of(9, 0), enabled = false)
        assertEquals(passed.copy(enabled = true, date = dec1.plusDays(1)), enableGeneralAlarm(passed, at(1, 10)))
    }

    @Test
    fun `안 지난 한 번 알람과 반복 알람은 켜기만 한다`() {
        val future = oneTime(1, dec1.plusDays(3), LocalTime.of(9, 0), enabled = false)
        assertEquals(future.copy(enabled = true), enableGeneralAlarm(future, at(1, 10)))
        val repeat = weekly(2, LocalTime.of(9, 0), MONDAY).copy(enabled = false)
        assertEquals(repeat.copy(enabled = true), enableGeneralAlarm(repeat, at(1, 10)))
    }

    // ── 화면에 쓰는 도우미 ──

    @Test
    fun `날짜 글은 2026-12-01 모양만 받는다`() {
        assertEquals(dec1, parseDateText(" 2026-12-01 "))
        assertNull(parseDateText("2026-12-32"))
        assertNull(parseDateText("2026-02-30"))
        assertNull(parseDateText("12/01"))
    }

    @Test
    fun `한 번 알람의 기본 날짜는 시각이 지났으면 내일 아니면 오늘`() {
        assertEquals(dec1, defaultOneTimeDate(at(1, 8), LocalTime.of(9, 0)))
        assertEquals(dec1.plusDays(1), defaultOneTimeDate(at(1, 9), LocalTime.of(9, 0)))
    }

    @Test
    fun `반복 요일 글은 매일 평일 주말을 묶어서 보여준다`() {
        assertEquals("매일", repeatDaysText(java.time.DayOfWeek.entries.toSet()))
        assertEquals("평일", repeatDaysText(setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)))
        assertEquals("주말", repeatDaysText(setOf(SATURDAY, SUNDAY)))
        assertEquals("월 수 일", repeatDaysText(setOf(SUNDAY, MONDAY, WEDNESDAY)))
    }

    @Test
    fun `일반 알람의 알림 글`() {
        val upcoming = UpcomingAlarm.General(weekly(7, LocalTime.of(9, 0), MONDAY), at(7, 9))
        assertEquals("운동", upcoming.title)
        assertEquals("12/07(월) 09:00 · 월", upcoming.text)
        val noName = UpcomingAlarm.General(GeneralAlarm(8, "", LocalTime.of(9, 0), date = dec1), at(1, 9))
        assertEquals("알람", noName.title)
        assertEquals("12/01(화) 09:00", noName.text)
    }
}
