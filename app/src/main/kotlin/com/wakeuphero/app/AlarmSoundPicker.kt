package com.wakeuphero.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.fillMaxWidth

// 근무 유형 알람음 고르기 (A4): 휴대폰의 기본 "소리 선택" 창을 띄운다 (누르면 미리 들려줌)
// soundUri == null 이면 휴대폰 기본 알람음
@Composable
fun AlarmSoundRow(soundUri: String?, onChange: (String?) -> Unit) {
    val context = LocalContext.current
    val title = remember(soundUri) { soundTitle(context, soundUri) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult // 취소면 그대로
        val picked = result.data?.pickedUri()
        // "기본값"을 고르면 null(= 기본 알람음)로 저장 → 나중에 휴대폰 기본 알람음을 바꾸면 따라감
        onChange(picked?.takeIf { it != RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) }?.toString())
    }

    Text("알람음", style = MaterialTheme.typography.titleMedium)
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(
            onClick = {
                launcher.launch(
                    Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "알람음 고르기")
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false) // 무음 알람은 안 됨
                        .putExtra(
                            RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                            soundUri?.let(Uri::parse) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("🔔 $title") }
    }
}

private fun Intent.pickedUri(): Uri? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
    }

// 화면에 보여 줄 소리 이름 (못 읽으면 "고른 소리")
private fun soundTitle(context: Context, soundUri: String?): String {
    if (soundUri == null) return "기본 알람음"
    return try {
        RingtoneManager.getRingtone(context, Uri.parse(soundUri))?.getTitle(context) ?: "고른 소리"
    } catch (e: Exception) {
        "고른 소리"
    }
}
