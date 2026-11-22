package com.wakeuphero.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.wakeuphero.app.MainActivity
import com.wakeuphero.app.WakeUpHeroApp
import com.wakeuphero.core.AlarmInfo
import com.wakeuphero.core.alarmText
import com.wakeuphero.core.alarmTitle
import com.wakeuphero.core.nextAlarmInfo
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.ZoneId

// 휴대폰의 알람 시계(AlarmManager)에 "다음 알람 하나만" 예약한다 (도미노 방식)
// - 울리면 AlarmReceiver가 다시 rescheduleFromDb()를 불러 그다음 알람을 예약
object AlarmScheduler {
    private const val REQUEST_NEXT = 0   // 근무표 알람 자리
    private const val REQUEST_SNOOZE = 1 // 다시 알림·테스트 자리 (근무표 알람과 따로)

    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    // 다음 알람 예약 (info == null 이면 예약 취소)
    fun schedule(context: Context, info: AlarmInfo?) {
        if (info == null) {
            cancel(context, REQUEST_NEXT)
            return
        }
        setAlarm(context, REQUEST_NEXT, info.time, alarmTitle(info), alarmText(info))
    }

    // DB에서 최신 근무표를 읽어 다음 알람을 다시 예약 (울린 뒤, 재부팅 뒤, 시간 변경 뒤)
    suspend fun rescheduleFromDb(context: Context) {
        val repo = (context.applicationContext as WakeUpHeroApp).repository
        val info = nextAlarmInfo(
            LocalDateTime.now(),
            repo.schedule.first(),
            repo.shiftTypes.first(),
            repo.alarmRules.first()
        )
        schedule(context, info)
    }

    fun snooze(context: Context, minutes: Long, title: String, text: String) =
        setAlarm(context, REQUEST_SNOOZE, LocalDateTime.now().plusMinutes(minutes), title, "다시 알림 · $text")

    // 테스트용: seconds초 뒤에 울리기
    fun scheduleTest(context: Context, seconds: Long) =
        setAlarm(context, REQUEST_SNOOZE, LocalDateTime.now().plusSeconds(seconds), "테스트 알람", "알람이 잘 울리는지 확인해요")

    private fun setAlarm(context: Context, requestCode: Int, time: LocalDateTime, title: String, text: String) {
        if (!canScheduleExact(context)) return // 권한이 없으면 예약 불가 (홈 화면에서 안내)

        val fire = PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, AlarmReceiver::class.java)
                .setAction(AlarmReceiver.ACTION_FIRE)
                .putExtra(AlarmActivity.EXTRA_TITLE, title)
                .putExtra(AlarmActivity.EXTRA_TEXT, text),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // 상단 바 알람 아이콘을 눌렀을 때 열 화면
        val show = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val millis = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // setAlarmClock: 알람 시계 앱용. 절전 모드에서도 정확히 울리고 상단 바에 알람 아이콘이 뜬다
        context.getSystemService(AlarmManager::class.java)
            .setAlarmClock(AlarmManager.AlarmClockInfo(millis, show), fire)
    }

    private fun cancel(context: Context, requestCode: Int) {
        val pending = PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_FIRE),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        context.getSystemService(AlarmManager::class.java).cancel(pending)
        pending.cancel()
    }
}
