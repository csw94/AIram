package com.wakeuphero.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wakeuphero.app.alarm.AlarmScheduler
import com.wakeuphero.app.data.ScheduleRepository
import com.wakeuphero.core.GeneralAlarm
import com.wakeuphero.core.enableGeneralAlarm
import com.wakeuphero.core.UpcomingAlarm
import com.wakeuphero.core.nextUpcomingAlarm
import kotlinx.coroutines.flow.combine
import java.time.LocalDateTime
import com.wakeuphero.core.AlarmRule
import com.wakeuphero.core.ShiftType
import com.wakeuphero.core.WorkDay
import com.wakeuphero.core.DeleteShiftResult
import com.wakeuphero.core.applyPattern
import com.wakeuphero.core.applyRoster
import com.wakeuphero.core.RosterCell
import com.wakeuphero.core.removeShift
import com.wakeuphero.core.setShift
import com.wakeuphero.core.toggleSkip
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

// 화면들이 함께 쓰는 근무표 상태를 들고 있는 곳
// - ViewModel은 화면을 돌려도(회전) 사라지지 않는다
// - 편집 규칙은 core 함수, 저장은 저장소(Room)에 맡기고 여기서는 둘을 이어 주기만 한다
// - 흐름: 버튼 → core 함수로 새 근무표 계산 → 저장소에 저장 → DB가 바뀌면 Flow로 화면 자동 갱신
// - 근무표·근무 유형·알람 규칙이 바뀔 때마다 다음 알람을 계산해서 onAlarmChange로 알려준다 (→ 휴대폰 알람 예약)
class ScheduleViewModel(
    private val repository: ScheduleRepository,
    private val onAlarmChange: (UpcomingAlarm?) -> Unit = {}
) : ViewModel() {

    // stateIn: DB에서 흘러오는 Flow를 "지금 값"을 가진 StateFlow로 바꿔 화면에서 쓰기 쉽게
    val schedule: StateFlow<List<WorkDay>> = repository.schedule.asState(emptyList())
    val shiftTypes: StateFlow<List<ShiftType>> = repository.shiftTypes.asState(emptyList())
    val alarmRules: StateFlow<List<AlarmRule>> = repository.alarmRules.asState(emptyList())
    val generalAlarms: StateFlow<List<GeneralAlarm>> = repository.generalAlarms.asState(emptyList())

    init {
        viewModelScope.launch { repository.seedDefaultsIfEmpty() }
        // 넷 중 하나라도 바뀌면(combine) 다음 알람(근무·일반)을 다시 계산해서 예약
        viewModelScope.launch {
            combine(repository.schedule, repository.shiftTypes, repository.alarmRules, repository.generalAlarms) { s, t, r, g ->
                nextUpcomingAlarm(LocalDateTime.now(), s, t, r, g)
            }.collect { onAlarmChange(it) }
        }
    }

    fun setShift(date: LocalDate, shiftTypeId: Int) = edit { setShift(it, date, shiftTypeId) }

    fun removeShift(date: LocalDate) = edit { removeShift(it, date) }

    fun toggleSkip(date: LocalDate) = edit { toggleSkip(it, date) }

    // 그 달 전체(1일 ~ 말일)를 패턴으로 다시 채운다
    fun applyPatternToMonth(month: YearMonth, pattern: List<Int>) =
        edit { applyPattern(it, month.atDay(1), month.atEndOfMonth(), pattern) }

    // 근무표 글자·사진 입력(S8): 확인이 끝난 칸들 중 근무를 고른 칸만 반영
    fun applyRoster(cells: List<RosterCell>) = edit { applyRoster(it, cells) }

    // 근무 유형 저장 (검사는 화면에서 core의 validateShiftType으로 먼저 한다)
    fun saveShiftType(type: ShiftType, rule: AlarmRule?) =
        viewModelScope.launch { repository.saveShiftType(type, rule) }

    // 근무 유형 삭제: 근무표에서 쓰고 있으면 지우지 않고 InUse(날짜들)를 돌려준다 (판단은 core)
    fun deleteShiftType(id: Int): DeleteShiftResult {
        val result = com.wakeuphero.core.deleteShiftType(shiftTypes.value, alarmRules.value, schedule.value, id)
        if (result is DeleteShiftResult.Deleted) viewModelScope.launch { repository.deleteShiftType(id) }
        return result
    }

    // 일반 알람 (검사는 화면에서 core의 validateGeneralAlarm으로 먼저 한다)
    fun saveGeneralAlarm(alarm: GeneralAlarm) = viewModelScope.launch { repository.saveGeneralAlarm(alarm) }
    // 켤 때는 core의 enableGeneralAlarm으로 (지난 한 번 알람이면 날짜를 다음으로 옮김)
    fun setGeneralAlarmEnabled(id: Int, enabled: Boolean) = viewModelScope.launch {
        val alarm = generalAlarms.value.find { it.id == id } ?: return@launch
        if (enabled) repository.saveGeneralAlarm(enableGeneralAlarm(alarm, LocalDateTime.now()))
        else repository.setGeneralAlarmEnabled(id, false)
    }
    fun deleteGeneralAlarm(id: Int) = viewModelScope.launch { repository.deleteGeneralAlarm(id) }

    // 지금 근무표에 change를 적용한 결과를 저장 (바뀐 부분만 DB에 반영됨)
    private fun edit(change: (List<WorkDay>) -> List<WorkDay>) {
        val before = schedule.value
        val after = change(before)
        viewModelScope.launch { repository.saveSchedule(before, after) }
    }

    private fun <T> kotlinx.coroutines.flow.Flow<T>.asState(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.Eagerly, initial)

    companion object {
        // ViewModel에 저장소를 넘겨주는 공장 (앱 객체에서 저장소를 꺼내 온다)
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as WakeUpHeroApp
                ScheduleViewModel(app.repository) { info -> AlarmScheduler.schedule(app, info) }
            }
        }
    }
}
