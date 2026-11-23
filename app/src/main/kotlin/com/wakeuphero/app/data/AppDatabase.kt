package com.wakeuphero.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ShiftTypeEntity::class, AlarmRuleEntity::class, WorkDayEntity::class, GeneralAlarmEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        // 1 → 2: 일반 알람 표만 새로 만든다 (기존 근무표·근무 유형은 그대로)
        // 표 모양은 GeneralAlarmEntity와 정확히 같아야 한다 (Room이 열 때 확인함)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `general_alarms` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`label` TEXT NOT NULL, `time` TEXT NOT NULL, `days` TEXT NOT NULL, " +
                        "`date` TEXT, `enabled` INTEGER NOT NULL)"
                )
            }
        }

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "wakeuphero.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
