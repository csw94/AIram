package com.wakeuphero.app.alarm

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import com.wakeuphero.app.AppSettings
import com.wakeuphero.app.WakeUpHeroTheme
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// 알람이 울릴 때 뜨는 화면: [5분 뒤 다시 알림] [끄기] (미션 알람이면 [끄기] 대신 수학 문제)
class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // 위쪽 상태 표시줄 아이콘 색을 밝은/다크 모드에 맞춰 자동으로
        showOverLockScreen()

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "알람"
        val text = intent.getStringExtra(EXTRA_TEXT) ?: ""
        val now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        val mission = AppSettings.isMathMission(this)

        setContent {
            WakeUpHeroTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // imePadding + 스크롤: 키보드가 올라오면 그만큼 화면을 줄여 [확인] 버튼이 가려지지 않게
                    Column(
                        modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
                    ) {
                        Text(now, fontSize = 72.sp)
                        Text("⏰ $title", style = MaterialTheme.typography.headlineMedium)
                        Text(text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                        Text("일어나세요 용사여!", style = MaterialTheme.typography.bodyLarge)
                        val snoozeButton = @Composable {
                            OutlinedButton(onClick = { snooze(title, text) }, modifier = Modifier.fillMaxWidth()) {
                                Text("${SNOOZE_MINUTES}분 뒤 다시 알림")
                            }
                        }
                        if (mission) {
                            BackHandler {} // 미션 중에는 뒤로가기로 빠져나가지 못하게
                            MathMissionSection(onDone = { dismiss() }) // 답 칸 바로 아래에 [확인]이 오도록 문제를 먼저
                            snoozeButton()
                        } else {
                            snoozeButton()
                            Button(onClick = { dismiss() }, modifier = Modifier.fillMaxWidth()) {
                                Text("끄기", fontSize = 20.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun dismiss() {
        AlarmService.stop(this) // 소리·진동 멈춤
        finish()
    }

    private fun snooze(title: String, text: String) {
        AlarmScheduler.snooze(this, SNOOZE_MINUTES, title, text.removePrefix("다시 알림 · "))
        dismiss()
    }

    // 잠금 화면 위에 보이고, 꺼진 화면을 켠다
    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_TEXT = "text"
        const val SNOOZE_MINUTES = 5L
    }
}
