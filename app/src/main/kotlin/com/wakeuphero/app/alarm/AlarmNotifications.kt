package com.wakeuphero.app.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager

// 알람 알림: 알람 소리로 "끌 때까지 반복" 울리고, 잠금 화면이면 알람 화면(AlarmActivity)을 바로 띄운다
object AlarmNotifications {
    private const val CHANNEL_ID = "alarm_v1" // 채널 설정(소리 등)을 바꾸면 이름도 바꿔야 새 설정이 적용됨
    const val NOTIFICATION_ID = 1001

    // 알림 채널: Android 8.0+ 에서 소리·진동은 채널 단위로 정해진다
    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "알람", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "출근 알람"
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM) // "알람 볼륨"으로 울림
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 800, 400, 800)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun show(context: Context, title: String, text: String) {
        ensureChannel(context)

        // 알람 화면을 여는 PendingIntent (잠금 화면: 바로 전체 화면 / 사용 중: 알림을 누르면 열림)
        val openAlarm = PendingIntent.getActivity(
            context, 0,
            Intent(context, AlarmActivity::class.java)
                .putExtra(AlarmActivity.EXTRA_TITLE, title)
                .putExtra(AlarmActivity.EXTRA_TEXT, text)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ $title")
            .setContentText(text)
            .setCategory(Notification.CATEGORY_ALARM)
            .setFullScreenIntent(openAlarm, true)
            .setContentIntent(openAlarm)
            .setOngoing(true) // 밀어서 지울 수 없음 → 알람 화면에서 끄기
            .addAction(action(context, "끄기", AlarmReceiver.ACTION_DISMISS, title, text, 2))
            .addAction(action(context, "${AlarmActivity.SNOOZE_MINUTES}분 뒤", AlarmReceiver.ACTION_SNOOZE, title, text, 3))
            .build()
            .apply { flags = flags or Notification.FLAG_INSISTENT } // 끌 때까지 소리 반복

        context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    // 알림에 붙는 버튼: 누르면 AlarmReceiver가 끄기/다시 알림 처리
    private fun action(context: Context, label: String, action: String, title: String, text: String, requestCode: Int) =
        Notification.Action.Builder(
            null, label,
            PendingIntent.getBroadcast(
                context, requestCode,
                Intent(context, AlarmReceiver::class.java)
                    .setAction(action)
                    .putExtra(AlarmActivity.EXTRA_TITLE, title)
                    .putExtra(AlarmActivity.EXTRA_TEXT, text),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }
}
