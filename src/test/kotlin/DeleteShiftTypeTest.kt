import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DeleteShiftTypeTest {

    private fun date(d: Int) = LocalDate.of(2026, 12, d)

    // 기본 근무 유형 id: D=1, E=2, N=3, OFF=4
    private val D = 1
    private val E = 2
    private val N = 3

    @Test
    fun `근무표에서 안 쓰는 근무 유형은 알람 규칙과 함께 삭제된다`() {
        val schedule = listOf(WorkDay(date(1), D), WorkDay(date(2), E))

        val result = deleteShiftType(defaultShiftTypes, defaultAlarmRules, schedule, N)

        assertIs<DeleteShiftResult.Deleted>(result)
        assertEquals(listOf("D", "E", "OFF"), result.shiftTypes.map { it.shortName })
        assertEquals(listOf(D, E), result.rules.map { it.shiftTypeId })
    }

    @Test
    fun `근무표에서 쓰는 근무 유형은 삭제되지 않고 쓰는 날짜를 날짜순으로 알려준다`() {
        val schedule = listOf(WorkDay(date(5), E), WorkDay(date(1), D), WorkDay(date(3), E))

        val result = deleteShiftType(defaultShiftTypes, defaultAlarmRules, schedule, E)

        assertEquals(DeleteShiftResult.InUse(listOf(date(3), date(5))), result)
    }

    @Test
    fun `지난 날짜에만 쓰였어도 삭제되지 않는다`() {
        val schedule = listOf(WorkDay(LocalDate.of(2020, 1, 1), D))

        val result = deleteShiftType(defaultShiftTypes, defaultAlarmRules, schedule, D)

        assertEquals(DeleteShiftResult.InUse(listOf(LocalDate.of(2020, 1, 1))), result)
    }

    @Test
    fun `알람을 끈 날도 쓰는 것으로 본다`() {
        val schedule = listOf(WorkDay(date(1), D, alarmSkipped = true))

        val result = deleteShiftType(defaultShiftTypes, defaultAlarmRules, schedule, D)

        assertEquals(DeleteShiftResult.InUse(listOf(date(1))), result)
    }

    @Test
    fun `없는 근무 유형을 지우려 하면 NotFound`() {
        val result = deleteShiftType(defaultShiftTypes, defaultAlarmRules, emptyList(), 99)

        assertEquals(DeleteShiftResult.NotFound, result)
    }

    @Test
    fun `알람 규칙이 없는 휴무 유형도 삭제할 수 있다`() {
        val result = deleteShiftType(defaultShiftTypes, defaultAlarmRules, emptyList(), 4) // OFF

        assertIs<DeleteShiftResult.Deleted>(result)
        assertEquals(listOf("D", "E", "N"), result.shiftTypes.map { it.shortName })
        assertEquals(defaultAlarmRules, result.rules) // 규칙은 그대로
    }
}
