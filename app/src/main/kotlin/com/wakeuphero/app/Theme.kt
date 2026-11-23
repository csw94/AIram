package com.wakeuphero.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// 앱 공용 테마 (A5): 휴대폰이 다크 모드면 어두운 색, 아니면 밝은 색
// 모든 화면(앱·알람 화면)이 이걸로 감싸므로, 색은 직접 쓰지 말고 MaterialTheme.colorScheme에서 꺼내 쓴다
@Composable
fun WakeUpHeroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
        content = content
    )
}
