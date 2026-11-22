package com.wakeuphero.app

import androidx.lifecycle.ViewModel
import com.wakeuphero.core.AlarmRule
import com.wakeuphero.core.ShiftType
import com.wakeuphero.core.WorkDay
import com.wakeuphero.core.applyPattern
import com.wakeuphero.core.defaultAlarmRules
import com.wakeuphero.core.defaultShiftTypes
import com.wakeuphero.core.fillPattern
import com.wakeuphero.core.removeShift
import com.wakeuphero.core.setShift
import com.wakeuphero.core.toggleSkip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth

// 화면들이 함께 쓰는 근무표 상태를 들고 있는 곳
// - ViewModel은 화면을 돌려도(회전) 사라지지 않는다
// - 실제 계산·편집 규칙은 전부 core 함수에 맡기고, 여기서는 "상태를 바꿔 끼우기"만 한다
// - ⚠️ 아직 저장 기능이 없어서 앱을 끄면 초기화된다 (M3에서 Room으로 저장 예정)
class ScheduleViewModel : ViewModel() {

    val shiftTypes: List<ShiftType> = defaultShiftTypes
    val rules: List<AlarmRule> = defaultAlarmRules

    // 처음에는 이번 달을 예시 패턴(D D E E N N OFF OFF)으로 채워 둔다
    private val _schedule = MutableStateFlow(examplePattern())
    val schedule: StateFlow<List<WorkDay>> = _schedule.asStateFlow()

    fun setShift(date: LocalDate, shiftTypeId: Int) = _schedule.update { setShift(it, date, shiftTypeId) }

    fun removeShift(date: LocalDate) = _schedule.update { removeShift(it, date) }

    fun toggleSkip(date: LocalDate) = _schedule.update { toggleSkip(it, date) }

    // 그 달 전체(1일 ~ 말일)를 패턴으로 다시 채운다
    fun applyPatternToMonth(month: YearMonth, pattern: List<Int>) =
        _schedule.update { applyPattern(it, month.atDay(1), month.atEndOfMonth(), pattern) }

    private fun examplePattern(): List<WorkDay> {
        val month = YearMonth.now()
        return fillPattern(month.atDay(1), month.atEndOfMonth(), listOf(1, 1, 2, 2, 3, 3, 4, 4))
    }
}
