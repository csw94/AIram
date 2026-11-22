import java.time.LocalDate

// 근무 유형 삭제 결과 (셋 중 하나)
// sealed class: 결과의 종류를 미리 정해 두는 문법 → when으로 나눌 때 빠뜨린 경우가 있으면 컴파일러가 알려준다
sealed class DeleteShiftResult {
    // 삭제 성공: 그 유형과 알람 규칙을 뺀 새 목록
    data class Deleted(val shiftTypes: List<ShiftType>, val rules: List<AlarmRule>) : DeleteShiftResult()

    // 근무표에서 쓰고 있어서 삭제 불가: 쓰고 있는 날짜들 (날짜순)
    data class InUse(val dates: List<LocalDate>) : DeleteShiftResult()

    // 그런 근무 유형이 없음
    data object NotFound : DeleteShiftResult()
}

// 근무 유형 삭제. 근무표에서 한 번이라도 쓰였으면(지난 날짜, 알람 끈 날 포함) 삭제하지 않는다
fun deleteShiftType(
    shiftTypes: List<ShiftType>,
    rules: List<AlarmRule>,
    schedule: List<WorkDay>,
    shiftTypeId: Int
): DeleteShiftResult {
    if (shiftTypes.none { it.id == shiftTypeId }) return DeleteShiftResult.NotFound

    val usedDates = schedule.filter { it.shiftTypeId == shiftTypeId }.map { it.date }.sorted()
    if (usedDates.isNotEmpty()) return DeleteShiftResult.InUse(usedDates)

    return DeleteShiftResult.Deleted(
        shiftTypes = shiftTypes.filter { it.id != shiftTypeId },
        rules = rules.filter { it.shiftTypeId != shiftTypeId } // 그 유형의 알람 규칙도 함께 삭제
    )
}
