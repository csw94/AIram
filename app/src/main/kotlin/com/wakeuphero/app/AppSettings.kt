package com.wakeuphero.app

import android.content.Context

// 앱 설정 (값 몇 개뿐이라 Room 대신 SharedPreferences: 키-값 저장소)
// 알람 화면·알림처럼 앱이 꺼져 있다가 깨어나는 곳에서도 바로 읽을 수 있다
object AppSettings {
    private const val FILE = "settings"
    private const val KEY_MATH_MISSION = "math_mission"

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    // 미션 알람(A1): 켜면 수학 문제를 다 풀어야 알람이 꺼진다 (기본: 꺼짐)
    fun isMathMission(context: Context): Boolean = prefs(context).getBoolean(KEY_MATH_MISSION, false)

    fun setMathMission(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_MATH_MISSION, enabled).apply()
}
