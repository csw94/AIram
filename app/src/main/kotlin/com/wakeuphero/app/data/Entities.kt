package com.wakeuphero.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wakeuphero.core.AlarmRule
import com.wakeuphero.core.ShiftType
import com.wakeuphero.core.WorkDay
import java.time.LocalDate
import java.time.LocalTime

// DB 표(테이블) 모양. core의 data class와 1:1로 대응하고, 아래 변환 함수로 오간다.
// 날짜·시각은 DB에 글자로 저장한다 ("2026-12-01", "07:00") → 사람이 봐도 알기 쉽고 변환기가 필요 없음

@Entity(tableName = "shift_types")
data class ShiftTypeEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val shortName: String,
    val startTime: String?, // "07:00", 휴무는 null
    val color: String
)

@Entity(tableName = "alarm_rules")
data class AlarmRuleEntity(
    @PrimaryKey val shiftTypeId: Int, // 근무 유형마다 규칙 하나
    val minutesBefore: Int,
    val intervalMinutes: Int,
    val repeatCount: Int
)

@Entity(tableName = "work_days")
data class WorkDayEntity(
    @PrimaryKey val date: String, // "2026-12-01" (하루에 근무 하나)
    val shiftTypeId: Int,
    val alarmSkipped: Boolean
)

// ── core ↔ DB 변환 ───────────────────────────────────────────
fun ShiftTypeEntity.toCore() = ShiftType(id, name, shortName, startTime?.let(LocalTime::parse), color)
fun ShiftType.toEntity() = ShiftTypeEntity(id, name, shortName, startTime?.toString(), color)

fun AlarmRuleEntity.toCore() = AlarmRule(shiftTypeId, minutesBefore, intervalMinutes, repeatCount)
fun AlarmRule.toEntity() = AlarmRuleEntity(shiftTypeId, minutesBefore, intervalMinutes, repeatCount)

fun WorkDayEntity.toCore() = WorkDay(LocalDate.parse(date), shiftTypeId, alarmSkipped)
fun WorkDay.toEntity() = WorkDayEntity(date.toString(), shiftTypeId, alarmSkipped)
