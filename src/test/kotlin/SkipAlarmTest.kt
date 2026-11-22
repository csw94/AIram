import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SkipAlarmTest {

    private fun date(d: Int) = LocalDate.of(2026, 12, d)

    // 기본 근무 유형 id: D=1, E=2, N=3, OFF=4
    private val D = 1
    private val E = 2
    private val OFF = 4

    // ── 1. skipAlarm 함수 ─────────────────────────────────────
    @Test
    fun `그 날짜만 알람을 끄고 근무는 그대로 둔다`() {
        val schedule = listOf(WorkDay(date(1), D), WorkDay(date(2), E))
        assertEquals(
            listOf(WorkDay(date(1), D), WorkDay(date(2), E, alarmSkipped = true)),
            skipAlarm(schedule, date(2))
        )
    }

    @Test
    fun `근무표에 없는 날짜면 그대로 돌려준다`() {
        val schedule = listOf(WorkDay(date(1), D))
        assertEquals(schedule, skipAlarm(schedule, date(5)))
    }

    // ── 2. schedule.txt의 SKIP 줄 ─────────────────────────────
    @Test
    fun `SKIP 줄은 패턴 근무는 그대로 두고 알람만 끈다`() {
        val result = parseSchedule(
            listOf("PATTERN 2026-12-01 2026-12-03 D E D", "skip 2026-12-02"),
            defaultShiftTypes
        )
        assertEquals(
            listOf(WorkDay(date(1), D), WorkDay(date(2), E, alarmSkipped = true), WorkDay(date(3), D)),
            result.workDays
        )
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `SKIP 줄이 PATTERN 줄보다 위에 있어도 적용된다`() {
        val result = parseSchedule(
            listOf("SKIP 2026-12-01", "PATTERN 2026-12-01 2026-12-02 D"),
            defaultShiftTypes
        )
        assertEquals(
            listOf(WorkDay(date(1), D, alarmSkipped = true), WorkDay(date(2), D)),
            result.workDays
        )
    }

    @Test
    fun `근무가 없는 날이나 휴무인 날을 SKIP하면 오류`() {
        val result = parseSchedule(
            listOf("2026-12-01 OFF", "SKIP 2026-12-01", "SKIP 2026-12-09"),
            defaultShiftTypes
        )
        assertEquals(listOf(WorkDay(date(1), OFF)), result.workDays)
        assertEquals(2, result.errors.size)
        assertTrue(result.errors[0].startsWith("2번째 줄"))
        assertTrue(result.errors[1].startsWith("3번째 줄"))
    }

    @Test
    fun `SKIP 줄 형식이 틀리면 오류`() {
        val result = parseSchedule(
            listOf("2026-12-01 D", "SKIP", "SKIP 12월1일", "SKIP 2026-12-01 D"),
            defaultShiftTypes
        )
        assertEquals(listOf(WorkDay(date(1), D)), result.workDays) // 알람은 꺼지지 않음
        assertEquals(3, result.errors.size)
    }

    @Test
    fun `오류는 줄 번호 순서대로 알려준다`() {
        // SKIP 오류는 근무표를 다 읽은 뒤에 확인하지만, 보여줄 때는 줄 순서대로
        val result = parseSchedule(
            listOf("SKIP 2026-12-09", "2026-12-01 X"),
            defaultShiftTypes
        )
        assertEquals(2, result.errors.size)
        assertTrue(result.errors[0].startsWith("1번째 줄"))
        assertTrue(result.errors[1].startsWith("2번째 줄"))
    }

    // ── 3. 다음 알람 & 달력 표시 ──────────────────────────────
    @Test
    fun `SKIP한 날은 다음 알람에서 건너뛴다`() {
        val result = parseSchedule(
            listOf("PATTERN 2026-12-01 2026-12-02 D", "SKIP 2026-12-01"),
            defaultShiftTypes
        )
        val next = nextAlarm(
            LocalDateTime.of(2026, 12, 1, 0, 0), result.workDays, defaultShiftTypes, defaultAlarmRules
        )
        assertEquals(LocalDateTime.of(2026, 12, 2, 5, 30), next)
    }

    @Test
    fun `달력에서 알람을 끈 날은 별표를 붙이고 아래에 설명을 단다`() {
        val schedule = listOf(
            WorkDay(date(1), D),
            WorkDay(date(2), E, alarmSkipped = true)
        )
        val expected = listOf(
            "2026년 12월",
            "  일  월  화  수  목  금  토",
            "           D  E*   -   -   -",
            "   -   -   -   -   -   -   -",
            "   -   -   -   -   -   -   -",
            "   -   -   -   -   -   -   -",
            "   -   -   -   -   -",
            "* = 이번만 끄기 (알람 안 울림)"
        ).joinToString("\n")

        assertEquals(expected, formatCalendar(YearMonth.of(2026, 12), schedule, defaultShiftTypes))
    }
}
