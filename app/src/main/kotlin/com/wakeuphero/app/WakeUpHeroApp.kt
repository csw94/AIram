package com.wakeuphero.app

import android.app.Application
import com.wakeuphero.app.data.AppDatabase
import com.wakeuphero.app.data.ScheduleRepository

// 앱 전체에서 하나만 있는 객체 → DB와 저장소를 여기서 한 번만 만든다
class WakeUpHeroApp : Application() {
    val repository: ScheduleRepository by lazy { ScheduleRepository(AppDatabase.create(this).dao()) }
}
