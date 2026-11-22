import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AlarmCalculatorTest {

    // ── 테스트에서 공통으로 쓰는 준비물 ──────────────────────────
    private val day = ShiftType(1, "주간", "D", LocalTime.of(7, 0), "#FFD54F")
    private val off = ShiftType(2, "휴무", "OFF", null, "#BDBDBD")
    private val midnight = ShiftType(3, "새벽", "M", LocalTime.of(0, 30), "#7E57C2")

    private val dayRule = AlarmRule(shiftTypeId = 1, minutesBefore = 90, intervalMinutes = 10, repeatCount = 3)
    private val midnightRule = AlarmRule(shiftTypeId = 3, minutesBefore = 120, intervalMinutes = 10, repeatCount = 1)

    private val shiftTypes = listOf(day, off, midnight)
    private val rules = listOf(dayRule, midnightRule)

    // 날짜·시각을 짧게 쓰기 위한 도우미
    private fun date(month: Int, d: Int) = LocalDate.of(2026, month, d)
    private fun at(month: Int, d: Int, h: Int, m: Int) = LocalDateTime.of(2026, month, d, h, m)

    // ── 1. 알람 규칙 검증 ─────────────────────────────────────
    @Test
    fun `정상 규칙 - 90분 전부터 10분 간격 3번은 올바르다`() {
        assertTrue(dayRule.isValid())
    }

    @Test
    fun `마지막 알람이 출근 후에 울리면 잘못된 규칙이다`() {
        // 06:30, 06:50, 07:10 → 07:10은 출근(07:00) 후
        assertFalse(AlarmRule(1, minutesBefore = 30, intervalMinutes = 20, repeatCount = 3).isValid())
    }

    @Test
    fun `마지막 알람이 출근 시간 정각이면 잘못된 규칙이다`() {
        // 06:30, 06:45, 07:00 → 출근 정각에 울리면 이미 늦음
        assertFalse(AlarmRule(1, minutesBefore = 30, intervalMinutes = 15, repeatCount = 3).isValid())
    }

    @Test
    fun `0 이하의 값이 있으면 잘못된 규칙이다`() {
        assertFalse(AlarmRule(1, minutesBefore = 0, intervalMinutes = 10, repeatCount = 1).isValid())
        assertFalse(AlarmRule(1, minutesBefore = 30, intervalMinutes = 0, repeatCount = 1).isValid())
        assertFalse(AlarmRule(1, minutesBefore = 30, intervalMinutes = 10, repeatCount = 0).isValid())
    }

    // ── 2. 하루치 알람 계산 ───────────────────────────────────
    @Test
    fun `주간 근무 07시 출근이면 05시30분, 40분, 50분에 울린다`() {
        val result = alarmTimesFor(WorkDay(date(12, 1), day.id), day, dayRule)
        assertEquals(listOf(at(12, 1, 5, 30), at(12, 1, 5, 40), at(12, 1, 5, 50)), result)
    }

    @Test
    fun `00시30분 출근 2시간 전이면 전날 22시30분에 울린다`() {
        val result = alarmTimesFor(WorkDay(date(12, 2), midnight.id), midnight, midnightRule)
        assertEquals(listOf(at(12, 1, 22, 30)), result)
    }

    @Test
    fun `휴무일에는 알람이 없다`() {
        val result = alarmTimesFor(WorkDay(date(12, 1), off.id), off, dayRule)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `이번만 끄기를 누른 날은 알람이 없다`() {
        val result = alarmTimesFor(WorkDay(date(12, 1), day.id, alarmSkipped = true), day, dayRule)
        assertTrue(result.isEmpty())
    }

    // ── 3. 다음 알람 구하기 ───────────────────────────────────
    @Test
    fun `첫 알람 전이면 첫 알람이 다음 알람이다`() {
        val schedule = listOf(WorkDay(date(12, 1), day.id))
        assertEquals(at(12, 1, 5, 30), nextAlarm(at(12, 1, 5, 0), schedule, shiftTypes, rules))
    }

    @Test
    fun `알람 사이 시각이면 그다음 알람을 고른다`() {
        val schedule = listOf(WorkDay(date(12, 1), day.id))
        assertEquals(at(12, 1, 5, 40), nextAlarm(at(12, 1, 5, 35), schedule, shiftTypes, rules))
    }

    @Test
    fun `지금 막 울린 알람은 제외하고 그다음을 고른다`() {
        // 05:30 알람이 울린 직후 다음 알람을 예약하는 상황 (도미노 방식)
        val schedule = listOf(WorkDay(date(12, 1), day.id))
        assertEquals(at(12, 1, 5, 40), nextAlarm(at(12, 1, 5, 30), schedule, shiftTypes, rules))
    }

    @Test
    fun `휴무일은 건너뛰고 다음 근무일 알람을 고른다`() {
        val schedule = listOf(
            WorkDay(date(12, 1), day.id),
            WorkDay(date(12, 2), off.id),
            WorkDay(date(12, 3), off.id),
            WorkDay(date(12, 4), day.id)
        )
        // 12/1 마지막 알람(05:50) 이후 → 휴무 이틀 건너뛰고 12/4 05:30
        assertEquals(at(12, 4, 5, 30), nextAlarm(at(12, 1, 6, 0), schedule, shiftTypes, rules))
    }

    @Test
    fun `오늘이 휴무여도 내일 새벽 근무 알람이 오늘 밤에 울린다`() {
        val schedule = listOf(
            WorkDay(date(12, 1), off.id),
            WorkDay(date(12, 2), midnight.id)
        )
        assertEquals(at(12, 1, 22, 30), nextAlarm(at(12, 1, 21, 0), schedule, shiftTypes, rules))
    }

    @Test
    fun `이번만 끄기를 누른 날은 건너뛴다`() {
        val schedule = listOf(
            WorkDay(date(12, 1), day.id, alarmSkipped = true),
            WorkDay(date(12, 2), day.id)
        )
        assertEquals(at(12, 2, 5, 30), nextAlarm(at(12, 1, 0, 0), schedule, shiftTypes, rules))
    }

    @Test
    fun `한 달 안에 있는 근무는 찾는다`() {
        // 12/1 06:00 기준 → 한 달 뒤(1/1 06:00) 안에 있는 12/31 근무
        val schedule = listOf(
            WorkDay(date(12, 1), day.id),
            WorkDay(date(12, 31), day.id)
        )
        assertEquals(at(12, 31, 5, 30), nextAlarm(at(12, 1, 6, 0), schedule, shiftTypes, rules))
    }

    @Test
    fun `한 달보다 뒤에 있는 근무는 찾지 않는다`() {
        // 12/1 06:00 기준 → 한 달 뒤(1/1 06:00)보다 늦은 1/5 근무
        val schedule = listOf(
            WorkDay(date(12, 1), day.id),
            WorkDay(LocalDate.of(2027, 1, 5), day.id)
        )
        assertNull(nextAlarm(at(12, 1, 6, 0), schedule, shiftTypes, rules))
    }

    @Test
    fun `앞으로 근무가 없으면 다음 알람은 없다`() {
        val schedule = listOf(WorkDay(date(12, 1), day.id))
        assertNull(nextAlarm(at(12, 1, 6, 0), schedule, shiftTypes, rules))
    }

    @Test
    fun `근무표가 비어 있으면 다음 알람은 없다`() {
        assertNull(nextAlarm(at(12, 1, 6, 0), emptyList(), shiftTypes, rules))
    }
}
