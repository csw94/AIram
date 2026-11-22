package com.wakeuphero.app.data

import com.wakeuphero.core.AlarmRule
import com.wakeuphero.core.ShiftType
import com.wakeuphero.core.WorkDay
import com.wakeuphero.core.defaultAlarmRules
import com.wakeuphero.core.defaultShiftTypes
import com.wakeuphero.core.scheduleDiff
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 저장소: ViewModel은 DB를 직접 몰라도 되게, core 모양(WorkDay 등)으로만 주고받는다
class ScheduleRepository(private val dao: AppDao) {

    val schedule: Flow<List<WorkDay>> = dao.observeWorkDays().map { list -> list.map { it.toCore() } }
    val shiftTypes: Flow<List<ShiftType>> = dao.observeShiftTypes().map { list -> list.map { it.toCore() } }
    val alarmRules: Flow<List<AlarmRule>> = dao.observeAlarmRules().map { list -> list.map { it.toCore() } }

    // 처음 실행이면 기본 근무 유형(D/E/N/OFF)과 알람 규칙을 넣어 둔다
    suspend fun seedDefaultsIfEmpty() {
        if (dao.countShiftTypes() == 0) {
            dao.upsertShiftTypes(defaultShiftTypes.map { it.toEntity() })
            dao.upsertAlarmRules(defaultAlarmRules.map { it.toEntity() })
        }
    }

    // 근무 유형 저장 (휴무면 rule = null → 알람 규칙 삭제)
    suspend fun saveShiftType(type: ShiftType, rule: AlarmRule?) =
        dao.saveShiftType(type.toEntity(), rule?.toEntity())

    suspend fun deleteShiftType(id: Int) = dao.deleteShiftType(id)

    // 근무표 before → after 에서 바뀐 부분만 저장 (어떤 게 바뀌었는지는 core의 scheduleDiff가 계산)
    suspend fun saveSchedule(before: List<WorkDay>, after: List<WorkDay>) {
        val diff = scheduleDiff(before, after)
        dao.applyScheduleChanges(
            upserts = diff.upserts.map { it.toEntity() },
            deletions = diff.deletions.map { it.toString() }
        )
    }
}
