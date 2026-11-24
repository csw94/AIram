package com.wakeuphero.app.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.wakeuphero.app.AppSettings
import com.wakeuphero.core.SNOOZE_OPTIONS
import com.wakeuphero.core.snoozeLabel

// 알람 알림: 잠금 화면이면 알람 화면(AlarmActivity)을 바로 띄운다
// 소리·진동은 AlarmService가 직접 울리므로 이 알림 자체는 조용하다
object AlarmNotifications {
    private const val CHANNEL_ID = "alarm_v2" // 채널 설정(소리 등)을 바꾸면 이름도 바꿔야 새 설정이 적용됨 (v1: 알림 소리로 울림 → 무음 모드에서 막혀 v2로)
    private const val OLD_CHANNEL_ID = "alarm_v1"
    const val NOTIFICATION_ID = 1001

    // 알림 채널: 중요도 높음(전체 화면·상단 표시), 소리·진동은 끔 (서비스가 담당)
    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.deleteNotificationChannel(OLD_CHANNEL_ID)
        val channel = NotificationChannel(CHANNEL_ID, "알람", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "출근 알람"
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    fun build(context: Context, title: String, text: String, sound: String?): Notification {
        ensureChannel(context)

        // 알람 화면을 여는 PendingIntent (잠금 화면: 바로 전체 화면 / 사용 중: 알림을 누르면 열림)
        val openAlarm = PendingIntent.getActivity(
            context, 0,
            Intent(context, AlarmActivity::class.java)
                .putExtra(AlarmActivity.EXTRA_TITLE, title)
                .putExtra(AlarmActivity.EXTRA_TEXT, text)
                .putExtra(AlarmActivity.EXTRA_SOUND, sound)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ $title")
            .setContentText(text)
            .setCategory(Notification.CATEGORY_ALARM)
            .setFullScreenIntent(openAlarm, true)
            .setContentIntent(openAlarm)
            .setOngoing(true) // 밀어서 지울 수 없음 → 알람 화면에서 끄기
            .apply {
                // 미션 알람이면 [끄기] 버튼을 빼서 문제를 건너뛰지 못하게 (알림을 누르면 알람 화면에서 풀기)
                if (!AppSettings.isMathMission(context)) addAction(action(context, "끄기", AlarmReceiver.ACTION_DISMISS, title, text, sound, 2))
            }
            .apply {
                // [5분 뒤] [10분 뒤]: 버튼마다 요청 코드를 달리해야 서로 덮어쓰지 않음 (3, 4)
                SNOOZE_OPTIONS.forEachIndexed { i, minutes ->
                    addAction(action(context, snoozeLabel(minutes), AlarmReceiver.ACTION_SNOOZE, title, text, sound, 3 + i, minutes))
                }
            }
            .build()
    }

    // 알림에 붙는 버튼: 누르면 AlarmReceiver가 끄기/다시 알림 처리 (알림 버튼은 최대 3개)
    private fun action(context: Context, label: String, action: String, title: String, text: String, sound: String?, requestCode: Int, minutes: Long? = null) =
        Notification.Action.Builder(
            null, label,
            PendingIntent.getBroadcast(
                context, requestCode,
                Intent(context, AlarmReceiver::class.java)
                    .setAction(action)
                    .putExtra(AlarmActivity.EXTRA_TITLE, title)
                    .putExtra(AlarmActivity.EXTRA_TEXT, text)
                    .putExtra(AlarmActivity.EXTRA_SOUND, sound)
                    .apply { if (minutes != null) putExtra(AlarmActivity.EXTRA_MINUTES, minutes) },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }
}
