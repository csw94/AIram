package com.wakeuphero.app

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.wakeuphero.app.alarm.AlarmScheduler

// 알람이 제대로 울리려면 필요한 권한 3가지를 확인하고, 없으면 켜는 버튼을 보여준다
// + 디버그 앱에서는 "10초 뒤 테스트 알람" 버튼
@Composable
fun AlarmPermissionSection(testSound: String? = null) {
    val context = LocalContext.current
    // 설정 화면에 갔다가 돌아오면(onResume) 다시 확인하도록 숫자를 바꿔 화면을 새로 그린다
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refresh++
        onPauseOrDispose { }
    }
    val askNotification = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }

    @Suppress("UNUSED_EXPRESSION") refresh // refresh가 바뀌면 아래를 다시 계산
    val notificationsOk = notificationsEnabled(context)
    val exactOk = AlarmScheduler.canScheduleExact(context)
    val fullScreenOk = fullScreenAllowed(context)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!notificationsOk || !exactOk || !fullScreenOk) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("⚠️ 알람이 울리려면 권한이 필요해요", style = MaterialTheme.typography.titleSmall)
                    if (!notificationsOk) TextButton(onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            askNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else openAppSettings(context, Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    }) { Text("알림 허용하기") }
                    if (!exactOk && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) TextButton(onClick = {
                        openAppSettings(context, Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    }) { Text("정확한 알람 허용하기") }
                    if (!fullScreenOk && Build.VERSION.SDK_INT >= 34) TextButton(onClick = {
                        openAppSettings(context, Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                    }) { Text("잠금 화면 알람 화면 허용하기") }
                }
            }
        }

        if (BuildConfig.DEBUG) {
            OutlinedButton(onClick = { AlarmScheduler.scheduleTest(context, 10, testSound) }) {
                Text("🔔 10초 뒤 테스트 알람 (디버그용, 다음 근무 알람음)")
            }
        }
    }
}

private fun notificationsEnabled(context: Context): Boolean =
    context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()

private fun fullScreenAllowed(context: Context): Boolean =
    Build.VERSION.SDK_INT < 34 || context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

// 우리 앱의 설정 화면 열기 (권한 켜는 곳)
private fun openAppSettings(context: Context, action: String) {
    val intent = Intent(action)
        .setData(Uri.parse("package:${context.packageName}"))
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
        .onFailure { // 그 설정 화면이 없는 휴대폰이면 앱 정보 화면으로
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
}
