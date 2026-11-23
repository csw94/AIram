package com.wakeuphero.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDateTime

// 앱에 있는 화면 목록
enum class Screen { HOME, CALENDAR, SHIFT_LIST, SHIFT_EDIT, GENERAL_LIST, GENERAL_EDIT }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // 위쪽 상태 표시줄 아이콘 색을 밝은/다크 모드에 맞춰 자동으로
        setContent {
            WakeUpHeroTheme {
                Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                    App()
                }
            }
        }
    }
}

// 지금 어느 화면인지 기억해 두고 그 화면을 그린다 (화면이 적어서 Navigation 라이브러리 없이)
@Composable
fun App(vm: ScheduleViewModel = viewModel(factory = ScheduleViewModel.Factory)) {
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var editingShiftId by rememberSaveable { mutableStateOf<Int?>(null) } // null = 새로 만들기
    var editingAlarmId by rememberSaveable { mutableStateOf<Int?>(null) }  // null = 새로 만들기
    val schedule by vm.schedule.collectAsState()
    val shiftTypes by vm.shiftTypes.collectAsState()
    val alarmRules by vm.alarmRules.collectAsState()
    val generalAlarms by vm.generalAlarms.collectAsState()
    val context = LocalContext.current
    var mathMission by remember { mutableStateOf(AppSettings.isMathMission(context)) }

    when (screen) {
        Screen.HOME -> HomeScreen(
            schedule = schedule,
            shiftTypes = shiftTypes,
            alarmRules = alarmRules,
            generalAlarms = generalAlarms,
            now = LocalDateTime.now(),
            onOpenCalendar = { screen = Screen.CALENDAR },
            onOpenSettings = { screen = Screen.SHIFT_LIST },
            onOpenGeneralAlarms = { screen = Screen.GENERAL_LIST },
            mathMission = mathMission,
            onMathMissionChange = { mathMission = it; AppSettings.setMathMission(context, it) }
        )

        Screen.CALENDAR -> {
            BackHandler { screen = Screen.HOME } // 휴대폰 뒤로가기 → 홈
            CalendarScreen(
                schedule = schedule,
                shiftTypes = shiftTypes,
                onBack = { screen = Screen.HOME },
                onSetShift = vm::setShift,
                onRemoveShift = vm::removeShift,
                onToggleSkip = vm::toggleSkip,
                onApplyPattern = vm::applyPatternToMonth
            )
        }

        Screen.SHIFT_LIST -> {
            BackHandler { screen = Screen.HOME }
            ShiftTypeListScreen(
                shiftTypes = shiftTypes,
                alarmRules = alarmRules,
                onBack = { screen = Screen.HOME },
                onEdit = { id -> editingShiftId = id; screen = Screen.SHIFT_EDIT }
            )
        }

        Screen.SHIFT_EDIT -> {
            BackHandler { screen = Screen.SHIFT_LIST }
            val original = shiftTypes.find { it.id == editingShiftId }
            ShiftTypeEditScreen(
                original = original,
                originalRule = alarmRules.find { it.shiftTypeId == editingShiftId },
                allShiftTypes = shiftTypes,
                onSave = vm::saveShiftType,
                onDelete = vm::deleteShiftType,
                onClose = { screen = Screen.SHIFT_LIST }
            )
        }

        Screen.GENERAL_LIST -> {
            BackHandler { screen = Screen.HOME }
            GeneralAlarmListScreen(
                alarms = generalAlarms,
                now = LocalDateTime.now(),
                onBack = { screen = Screen.HOME },
                onEdit = { id -> editingAlarmId = id; screen = Screen.GENERAL_EDIT },
                onToggle = vm::setGeneralAlarmEnabled
            )
        }

        Screen.GENERAL_EDIT -> {
            BackHandler { screen = Screen.GENERAL_LIST }
            GeneralAlarmEditScreen(
                original = generalAlarms.find { it.id == editingAlarmId },
                now = LocalDateTime.now(),
                onSave = { vm.saveGeneralAlarm(it) },
                onDelete = { vm.deleteGeneralAlarm(it) },
                onClose = { screen = Screen.GENERAL_LIST }
            )
        }
    }
}
