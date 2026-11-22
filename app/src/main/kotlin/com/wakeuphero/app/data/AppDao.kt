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

    // @Transaction: 넣기와 지우기를 한 묶음으로 → 중간에 앱이 꺼져도 반만 저장되는 일이 없다
    @Transaction
    suspend fun applyScheduleChanges(upserts: List<WorkDayEntity>, deletions: List<String>) {
        if (deletions.isNotEmpty()) deleteWorkDays(deletions)
        if (upserts.isNotEmpty()) upsertWorkDays(upserts)
    }
}
