import java.time.LocalDate
import java.time.format.DateTimeParseException

// 근무표 파일 내용을 읽은 결과
data class ParseResult(
    val workDays: List<WorkDay>, // 제대로 읽은 근무 (날짜순)
    val errors: List<String>     // 잘못된 줄 안내 (예: "3번째 줄: 알 수 없는 근무 'X'")
)

// 근무표 파일의 각 줄을 WorkDay 목록으로 바꾼다
// - "2026-12-01 D"                              → 그 날짜 근무
// - "PATTERN 2026-12-01 2026-12-31 D D E E ..." → 기간 동안 패턴 반복
// - "SKIP 2026-12-03"                           → 그날 근무는 그대로, 알람만 끄기
// - 빈 줄, "#"으로 시작하는 줄은 무시
// - 우선순위: 날짜 줄 > 패턴 줄 (줄 순서와 상관없음), 패턴끼리 겹치면 아래 줄이 우선
// - SKIP은 근무표를 다 만든 뒤에 적용 (줄 순서와 상관없음)
// - 잘못된 줄은 건너뛰고 errors에 "n번째 줄: 이유"를 줄 번호 순서대로 남긴다
fun parseSchedule(lines: List<String>, shiftTypes: List<ShiftType>): ParseResult {
    val patternDays = mutableMapOf<LocalDate, WorkDay>() // 패턴으로 채운 근무
    val manualDays = mutableMapOf<LocalDate, WorkDay>()  // 날짜 줄로 직접 쓴 근무
    val skips = mutableListOf<Pair<Int, LocalDate>>()    // (줄 번호, 알람 끌 날짜)
    val errors = mutableListOf<Pair<Int, String>>()      // (줄 번호, 오류 이유)

    lines.forEachIndexed { index, rawLine ->
        val lineNo = index + 1
        val line = rawLine.trim()
        if (line.isEmpty() || line.startsWith("#")) return@forEachIndexed

        // 공백 여러 칸도 하나로 보고 나눈다
        val parts = line.split(Regex("\\s+"))

        val error = when (parts[0].uppercase()) {
            "PATTERN" -> parsePatternLine(parts, shiftTypes, patternDays)
            "SKIP" -> parseSkipLine(parts, lineNo, skips)
            else -> parseDateLine(parts, shiftTypes, manualDays)
        }
        if (error != null) errors += lineNo to error
    }

    // 패턴 위에 날짜 줄을 덮어쓴다 → 날짜 줄이 우선
    var schedule = (patternDays + manualDays).values.sortedBy { it.date }

    // 근무표가 다 만들어진 뒤에 SKIP 적용 (그래야 줄 순서와 상관없이 동작)
    for ((lineNo, date) in skips) {
        val workDay = schedule.find { it.date == date }
        val shiftType = workDay?.let { wd -> shiftTypes.find { it.id == wd.shiftTypeId } }
        when {
            workDay == null -> errors += lineNo to "${date}에는 근무가 없어서 끌 알람이 없어요"
            shiftType?.isOff == true -> errors += lineNo to "${date}은 휴무라 원래 알람이 없어요"
            else -> schedule = skipAlarm(schedule, date)
        }
    }

    val messages = errors.sortedBy { it.first }.map { (lineNo, reason) -> "${lineNo}번째 줄: $reason" }
    return ParseResult(schedule, messages)
}

// "SKIP 2026-12-03" 한 줄 읽기. 날짜만 모아 두고 실제 적용은 나중에. 실패하면 오류 이유
private fun parseSkipLine(parts: List<String>, lineNo: Int, skips: MutableList<Pair<Int, LocalDate>>): String? {
    if (parts.size != 2) return "'SKIP 날짜' 형식이어야 해요 (예: SKIP 2026-12-03)"
    val date = parseDate(parts[1]) ?: return "날짜 '${parts[1]}'를 읽을 수 없어요 (예: 2026-12-03)"
    skips += lineNo to date
    return null
}

// "2026-12-01 D" 한 줄 읽기. 성공하면 null, 실패하면 오류 이유를 돌려준다
private fun parseDateLine(
    parts: List<String>,
    shiftTypes: List<ShiftType>,
    manualDays: MutableMap<LocalDate, WorkDay>
): String? {
    if (parts.size != 2) return "'날짜 근무' 형식이어야 해요 (예: 2026-12-01 D)"

    val date = parseDate(parts[0]) ?: return "날짜 '${parts[0]}'를 읽을 수 없어요 (예: 2026-12-01)"
    val shiftType = findShift(parts[1], shiftTypes) ?: return "알 수 없는 근무 '${parts[1]}'"
    if (date in manualDays) return "${date} 근무가 이미 입력되어 있어요"

    manualDays[date] = WorkDay(date, shiftType.id)
    return null
}

// "PATTERN 시작일 끝날 근무 근무 ..." 한 줄 읽기. 성공하면 null, 실패하면 오류 이유
private fun parsePatternLine(
    parts: List<String>,
    shiftTypes: List<ShiftType>,
    patternDays: MutableMap<LocalDate, WorkDay>
): String? {
    if (parts.size < 4) return "'PATTERN 시작일 끝날 근무...' 형식이어야 해요 (예: PATTERN 2026-12-01 2026-12-31 D D E E N N OFF OFF)"

    val start = parseDate(parts[1]) ?: return "날짜 '${parts[1]}'를 읽을 수 없어요 (예: 2026-12-01)"
    val end = parseDate(parts[2]) ?: return "날짜 '${parts[2]}'를 읽을 수 없어요 (예: 2026-12-31)"
    if (start > end) return "시작일(${start})이 끝날(${end})보다 늦어요"

    // 근무 약어를 하나라도 모르면 그 줄 전체를 건너뛴다 (패턴이 어긋나면 안 되니까)
    val codes = parts.drop(3)
    val pattern = codes.map { code ->
        findShift(code, shiftTypes)?.id ?: return "알 수 없는 근무 '${code}'"
    }

    fillPattern(start, end, pattern).forEach { patternDays[it.date] = it } // 겹치면 아래 줄이 덮어씀
    return null
}

// "2026-12-01" → LocalDate, 형식이 틀리면 null
private fun parseDate(text: String): LocalDate? =
    try {
        LocalDate.parse(text)
    } catch (e: DateTimeParseException) {
        null
    }

// 근무 약어로 근무 유형 찾기 (대소문자 무시)
private fun findShift(code: String, shiftTypes: List<ShiftType>): ShiftType? =
    shiftTypes.find { it.shortName == code.uppercase() }
