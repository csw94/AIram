import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val CELL_WIDTH = 4 // 달력 한 칸 = 4글자

// 한 달 근무표를 달력 모양 문자열로 만든다 (일요일 시작, 근무 없는 날은 "-")
fun formatCalendar(month: YearMonth, schedule: List<WorkDay>, shiftTypes: List<ShiftType>): String {
    val lines = mutableListOf<String>()
    lines += "${month.year}년 ${month.monthValue}월"
    // 한글은 화면에서 2칸을 차지하므로 앞에 공백 2칸 → 한 칸 4글자 폭
    lines += listOf("일", "월", "화", "수", "목", "금", "토").joinToString("") { "  $it" }

    // 1일이 무슨 요일인지 → 앞을 몇 칸 비울지 (일=0, 월=1, ..., 토=6)
    // dayOfWeek.value 는 월=1 ... 일=7 이라서 7로 나눈 나머지를 쓴다
    val blankCells = month.atDay(1).dayOfWeek.value % 7

    val row = StringBuilder(" ".repeat(CELL_WIDTH * blankCells))
    var cellsInRow = blankCells

    for (day in 1..month.lengthOfMonth()) {
        val date = month.atDay(day)
        val workDay = schedule.find { it.date == date }
        val code = workDay?.let { wd -> shiftTypes.find { it.id == wd.shiftTypeId }?.shortName } ?: "-"
        val mark = if (workDay?.alarmSkipped == true) "*" else "" // 이번만 끄기 표시
        row.append((code + mark).padStart(CELL_WIDTH))
        cellsInRow++

        if (cellsInRow == 7) { // 토요일까지 채웠으면 다음 줄로
            lines += row.toString()
            row.clear()
            cellsInRow = 0
        }
    }
    if (row.isNotEmpty()) lines += row.toString() // 마지막 주

    // 이 달에 알람을 끈 날이 있으면 별표 설명 추가
    if (schedule.any { YearMonth.from(it.date) == month && it.alarmSkipped }) {
        lines += "* = 이번만 끄기 (알람 안 울림)"
    }

    return lines.joinToString("\n")
}

private val alarmFormatter = DateTimeFormatter.ofPattern("MM/dd(E) HH:mm", Locale.KOREAN)

// 다음 알람 안내 문장 (예: "다음 알람: 12/01(화) 05:30")
fun nextAlarmMessage(alarm: LocalDateTime?): String =
    if (alarm == null) "다음 알람: 없음 (한 달 안에 알람이 울릴 근무가 없어요)"
    else "다음 알람: ${alarm.format(alarmFormatter)}"
