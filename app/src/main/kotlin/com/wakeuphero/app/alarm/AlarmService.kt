package com.wakeuphero.app.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

// 알람 소리·진동을 "직접" 울리는 포그라운드 서비스
// 알림 소리는 무음·방해 금지·절전 모드에서 막힐 수 있어서, 시계 앱처럼 알람 볼륨(USAGE_ALARM)으로 직접 재생한다
class AlarmService : Service() {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent?.getStringExtra(AlarmActivity.EXTRA_TITLE) ?: "알람"
        val text = intent?.getStringExtra(AlarmActivity.EXTRA_TEXT) ?: ""
        val sound = intent?.getStringExtra(AlarmActivity.EXTRA_SOUND)

        val notification = AlarmNotifications.build(this, title, text, sound)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                AlarmNotifications.NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(AlarmNotifications.NOTIFICATION_ID, notification)
        }
        startRinging(sound)
        return START_NOT_STICKY
    }

    private fun startRinging(sound: String?) {
        stopSound() // 이미 울리는 중이면(다시 알림 겹침 등) 처음부터 다시
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wakeuphero:alarm")
            .apply { acquire(MAX_RING_MILLIS) }

        // 고른 알람음 → 못 틀면(지워진 소리 등) 기본 알람음
        player = sound?.let { playLooping(Uri.parse(it)) } ?: playLooping(alarmSoundUri())

        vibrator = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }).also { v ->
            val effect = VibrationEffect.createWaveform(VIBRATION_PATTERN, 0) // 0 = 처음부터 반복
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
            }
        }
    }

    // 소리를 끌 때까지 반복 재생 (못 틀면 null → 다음 후보로, 그래도 안 되면 진동·화면만)
    private fun playLooping(uri: Uri?): MediaPlayer? {
        if (uri == null) return null
        val mp = MediaPlayer()
        return try {
            mp.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM) // 알람 볼륨 → 무음 모드에서도 울림
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmService, uri)
                isLooping = true // 끌 때까지 반복
                prepare()
                start()
                Log.i(TAG, "알람음 재생: $uri")
            }
        } catch (e: Exception) {
            Log.w(TAG, "알람음 재생 실패: $uri", e)
            mp.release()
            null
        }
    }

    // 알람 소리 → 없으면 벨소리 → 없으면 알림음
    private fun alarmSoundUri() =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    private fun stopSound() {
        player?.run { try { stop() } catch (_: Exception) {}; release() }
        player = null
        vibrator?.cancel()
        vibrator = null
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    override fun onDestroy() {
        stopSound()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "AlarmService"
        private const val MAX_RING_MILLIS = 10 * 60 * 1000L // 깨어 있게 붙잡는 최대 시간
        private val VIBRATION_PATTERN = longArrayOf(0, 800, 400, 800, 1000)

        fun start(context: Context, title: String, text: String, sound: String?) {
            context.startForegroundService(
                Intent(context, AlarmService::class.java)
                    .putExtra(AlarmActivity.EXTRA_TITLE, title)
                    .putExtra(AlarmActivity.EXTRA_TEXT, text)
                    .putExtra(AlarmActivity.EXTRA_SOUND, sound)
            )
        }

        // 서비스를 끝내면 onDestroy에서 소리·진동이 멈추고 알림도 사라진다
        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmService::class.java))
            AlarmNotifications.cancel(context)
        }
    }
}
