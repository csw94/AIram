import java.time.LocalDate
import java.time.format.DateTimeParseException

// 근무표 파일 내용을 읽은 결과
data class ParseResult(
    val workDays: List<WorkDay>, // 제대로 읽은 근무
    val errors: List<String>     // 잘못된 줄 안내 (예: "3번째 줄: 알 수 없는 근무 'X'")
)

// 근무표 파일의 각 줄("2026-12-01 D")을 WorkDay 목록으로 바꾼다
// - 빈 줄, "#"으로 시작하는 줄은 무시
// - 잘못된 줄은 건너뛰고 errors에 "n번째 줄: 이유"를 남긴다
fun parseSchedule(lines: List<String>, shiftTypes: List<ShiftType>): ParseResult {
    val workDays = mutableListOf<WorkDay>()
    val errors = mutableListOf<String>()
    val seenDates = mutableSetOf<LocalDate>() // 이미 나온 날짜 (중복 확인용)

    lines.forEachIndexed { index, rawLine ->
        val lineNo = index + 1
        val line = rawLine.trim()
        if (line.isEmpty() || line.startsWith("#")) return@forEachIndexed

        // 공백 여러 칸도 하나로 보고 나눈다
        val parts = line.split(Regex("\\s+"))
        if (parts.size != 2) {
            errors += "${lineNo}번째 줄: '날짜 근무' 형식이어야 해요 (예: 2026-12-01 D)"
            return@forEachIndexed
        }

        val date = try {
            LocalDate.parse(parts[0]) // "2026-12-01" 형식만 받음
        } catch (e: DateTimeParseException) {
            errors += "${lineNo}번째 줄: 날짜 '${parts[0]}'를 읽을 수 없어요 (예: 2026-12-01)"
            return@forEachIndexed
        }

        val code = parts[1].uppercase()
        val shiftType = shiftTypes.find { it.shortName == code }
        if (shiftType == null) {
            errors += "${lineNo}번째 줄: 알 수 없는 근무 '${parts[1]}'"
            return@forEachIndexed
        }

        if (!seenDates.add(date)) { // add가 false면 이미 있던 날짜
            errors += "${lineNo}번째 줄: ${date} 근무가 이미 입력되어 있어요"
            return@forEachIndexed
        }

        workDays += WorkDay(date, shiftType.id)
    }

    return ParseResult(workDays, errors)
}
