package com.example.napvibrationtimer

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    VibrationTimerScreen(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

enum class Mode { TIME, DURATION }

private val durationPresets = listOf(10, 15, 20, 30, 45, 60, 90)

private fun formatRemaining(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibrationTimerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val serviceState by TimerService.state.collectAsStateWithLifecycle()
    val isRunning = serviceState.status == TimerService.TimerUiState.Status.RUNNING
    val isVibrating = serviceState.status == TimerService.TimerUiState.Status.VIBRATING

    val now = remember { Calendar.getInstance() }
    var selectedHour by remember { mutableIntStateOf(now.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(now.get(Calendar.MINUTE)) }
    var showTimePicker by remember { mutableStateOf(false) }

    var mode by remember { mutableStateOf(Mode.TIME) }
    var selectedDurationMinutes by remember { mutableIntStateOf(30) }
    var remainingMillis by remember { mutableLongStateOf(0L) }

    LaunchedEffect(serviceState.status, serviceState.targetTimeMs) {
        if (serviceState.status == TimerService.TimerUiState.Status.RUNNING) {
            while (true) {
                remainingMillis = serviceState.targetTimeMs - System.currentTimeMillis()
                delay(1000)
            }
        } else {
            remainingMillis = 0L
        }
    }

    fun startTimer() {
        val targetMs = when (mode) {
            Mode.TIME -> {
                val target = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, selectedHour)
                    set(Calendar.MINUTE, selectedMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (!target.after(Calendar.getInstance())) {
                    target.add(Calendar.DAY_OF_MONTH, 1)
                }
                target.timeInMillis
            }
            Mode.DURATION -> System.currentTimeMillis() + selectedDurationMinutes * 60_000L
        }
        val intent = Intent(context, TimerService::class.java).apply {
            putExtra(TimerService.EXTRA_TARGET_TIME, targetMs)
        }
        context.startForegroundService(intent)
    }

    fun stopService() {
        context.stopService(Intent(context, TimerService::class.java))
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedHour = timePickerState.hour
                    selectedMinute = timePickerState.minute
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Mode selector
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = mode == Mode.TIME,
                onClick = { if (!isRunning && !isVibrating) mode = Mode.TIME },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text("時刻")
            }
            SegmentedButton(
                selected = mode == Mode.DURATION,
                onClick = { if (!isRunning && !isVibrating) mode = Mode.DURATION },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text("時間")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        when (mode) {
            Mode.TIME -> {
                Text(
                    text = "Wake-up Time",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    enabled = !isRunning && !isVibrating
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Mode.DURATION -> {
                Text(
                    text = "Sleep Duration",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                val row1 = durationPresets.take(4)
                val row2 = durationPresets.drop(4)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row1.forEach { minutes ->
                            FilterChip(
                                selected = selectedDurationMinutes == minutes,
                                onClick = { if (!isRunning && !isVibrating) selectedDurationMinutes = minutes },
                                label = { Text("${minutes}分") },
                                enabled = !isRunning && !isVibrating
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row2.forEach { minutes ->
                            FilterChip(
                                selected = selectedDurationMinutes == minutes,
                                onClick = { if (!isRunning && !isVibrating) selectedDurationMinutes = minutes },
                                label = { Text("${minutes}分") },
                                enabled = !isRunning && !isVibrating
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Remaining time / Time's up
        when {
            isRunning && remainingMillis > 0 -> {
                Text(
                    text = "残り ${formatRemaining(remainingMillis)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            isVibrating -> {
                Text(
                    text = "Time's up!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (isRunning || isVibrating) stopService() else startTimer()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .height(56.dp)
        ) {
            Text(
                text = if (isRunning || isVibrating) "STOP" else "START",
                fontSize = 18.sp
            )
        }
    }
}
