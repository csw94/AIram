package com.wakeuphero.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

// DAO(Data Access Object): DB에 무엇을 묻고 쓸지 적어 두면 Room이 실제 코드를 만들어 준다
@Dao
interface AppDao {

    // Flow: DB 내용이 바뀔 때마다 새 목록을 자동으로 흘려보내 준다 → 화면이 알아서 갱신됨
    @Query("SELECT * FROM work_days ORDER BY date")
    fun observeWorkDays(): Flow<List<WorkDayEntity>>

    @Query("SELECT * FROM shift_types ORDER BY id")
    fun observeShiftTypes(): Flow<List<ShiftTypeEntity>>

    @Query("SELECT * FROM alarm_rules")
    fun observeAlarmRules(): Flow<List<AlarmRuleEntity>>

    @Query("SELECT * FROM general_alarms ORDER BY time, id")
    fun observeGeneralAlarms(): Flow<List<GeneralAlarmEntity>>

    @Upsert
    suspend fun upsertGeneralAlarm(alarm: GeneralAlarmEntity)

    @Query("UPDATE general_alarms SET enabled = :enabled WHERE id = :id")
    suspend fun setGeneralAlarmEnabled(id: Int, enabled: Boolean)

    @Query("DELETE FROM general_alarms WHERE id = :id")
    suspend fun deleteGeneralAlarm(id: Int)

    @Query("SELECT COUNT(*) FROM shift_types")
    suspend fun countShiftTypes(): Int

    @Upsert // 있으면 덮어쓰고, 없으면 새로 넣기
    suspend fun upsertWorkDays(days: List<WorkDayEntity>)

    @Query("DELETE FROM work_days WHERE date IN (:dates)")
    suspend fun deleteWorkDays(dates: List<String>)

    @Upsert
    suspend fun upsertShiftTypes(types: List<ShiftTypeEntity>)

    @Upsert
    suspend fun upsertAlarmRules(rules: List<AlarmRuleEntity>)

    @Query("DELETE FROM shift_types WHERE id = :id")
    suspend fun deleteShiftTypeRow(id: Int)

    @Query("DELETE FROM alarm_rules WHERE shiftTypeId = :id")
    suspend fun deleteAlarmRule(id: Int)

    // 근무 유형과 그 알람 규칙을 함께 저장/삭제 (한 묶음)
    @Transaction
    suspend fun saveShiftType(type: ShiftTypeEntity, rule: AlarmRuleEntity?) {
        upsertShiftTypes(listOf(type))
        if (rule != null) upsertAlarmRules(listOf(rule)) else deleteAlarmRule(type.id) // 휴무면 규칙 없음
    }

    @Transaction
    suspend fun deleteShiftType(id: Int) {
        deleteAlarmRule(id)
        deleteShiftTypeRow(id)
    }

    // @Transaction: 넣기와 지우기를 한 묶음으로 → 중간에 앱이 꺼져도 반만 저장되는 일이 없다
    @Transaction
    suspend fun applyScheduleChanges(upserts: List<WorkDayEntity>, deletions: List<String>) {
        if (deletions.isNotEmpty()) deleteWorkDays(deletions)
        if (upserts.isNotEmpty()) upsertWorkDays(upserts)
    }
}
