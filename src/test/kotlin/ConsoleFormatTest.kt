import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals

class ConsoleFormatTest {

    private fun date(d: Int) = LocalDate.of(2026, 12, d)

    @Test
    fun `한 달 근무표를 일요일 시작 달력으로 출력한다`() {
        // 2026년 12월 1일은 화요일
        val schedule = listOf(
            WorkDay(date(1), 1), // D
            WorkDay(date(2), 1), // D
            WorkDay(date(3), 2), // E
            WorkDay(date(4), 4), // OFF
            WorkDay(date(5), 3)  // N
        )
        // 한 칸 = 4글자, 오른쪽 정렬 / 근무 없는 날은 "-"
        val expected = listOf(
            "2026년 12월",
            "  일  월  화  수  목  금  토",
            "           D   D   E OFF   N",
            "   -   -   -   -   -   -   -",
            "   -   -   -   -   -   -   -",
            "   -   -   -   -   -   -   -",
            "   -   -   -   -   -"
        ).joinToString("\n")

        assertEquals(expected, formatCalendar(YearMonth.of(2026, 12), schedule, defaultShiftTypes))
    }

    @Test
    fun `다른 달 근무는 달력에 나오지 않는다`() {
        val schedule = listOf(WorkDay(LocalDate.of(2027, 1, 1), 1))
        val result = formatCalendar(YearMonth.of(2026, 12), schedule, defaultShiftTypes)
        assertEquals(false, result.contains(" D"))
    }

    @Test
    fun `다음 알람을 월일 요일 시각으로 알려준다`() {
        assertEquals("다음 알람: 12/01(화) 05:30", nextAlarmMessage(LocalDateTime.of(2026, 12, 1, 5, 30)))
    }

    @Test
    fun `다음 알람이 없으면 없다고 알려준다`() {
        assertEquals("다음 알람: 없음 (한 달 안에 알람이 울릴 근무가 없어요)", nextAlarmMessage(null))
    }
}
