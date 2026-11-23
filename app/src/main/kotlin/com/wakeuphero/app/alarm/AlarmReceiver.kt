package com.wakeuphero.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// 예약한 시각이 되면 휴대폰이 이 리시버를 깨운다 → 알람 울리기 + 다음 알람 예약
// 알림의 [끄기] [5분 뒤] 버튼도 여기로 온다
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(AlarmActivity.EXTRA_TITLE) ?: "알람"
        val text = intent.getStringExtra(AlarmActivity.EXTRA_TEXT) ?: ""
        when (intent.action) {
            ACTION_FIRE -> {
                AlarmService.start(context, title, text)
                rescheduleAsync(context)
            }
            ACTION_DISMISS -> AlarmService.stop(context)
            ACTION_SNOOZE -> {
                AlarmScheduler.snooze(context, AlarmActivity.SNOOZE_MINUTES, title, text.removePrefix("다시 알림 · "))
                AlarmService.stop(context)
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.wakeuphero.app.ALARM_FIRE"
        const val ACTION_DISMISS = "com.wakeuphero.app.ALARM_DISMISS" // 알림의 [끄기]
        const val ACTION_SNOOZE = "com.wakeuphero.app.ALARM_SNOOZE"   // 알림의 [5분 뒤]
    }
}

// 휴대폰 재부팅·시간 변경·앱 업데이트·권한 변경 뒤에 다음 알람을 다시 예약
// (재부팅하면 AlarmManager 예약이 모두 사라지기 때문)
class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        rescheduleAsync(context)
    }
}

// 리시버는 금방 끝나야 해서, DB 읽기는 goAsync로 잠깐 시간을 벌어 백그라운드에서 한다
private fun BroadcastReceiver.rescheduleAsync(context: Context) {
    val pending = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            AlarmScheduler.rescheduleFromDb(context)
        } finally {
            pending.finish()
        }
    }
}
