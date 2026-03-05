package com.example.napvibrationtimer

import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VibrationTimerScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibrationTimerScreen() {
    val context = LocalContext.current
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(VibratorManager::class.java)
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
    }

    val now = remember { Calendar.getInstance() }
    val timePickerState = rememberTimePickerState(
        initialHour = now.get(Calendar.HOUR_OF_DAY),
        initialMinute = now.get(Calendar.MINUTE),
        is24Hour = true
    )
    var showTimePicker by remember { mutableStateOf(false) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var isVibrating by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }
    var countDownTimer by remember { mutableStateOf<CountDownTimer?>(null) }

    fun stopVibration() {
        vibrator.cancel()
        isVibrating = false
        isTimerRunning = false
        statusText = ""
    }

    fun stopTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
        isTimerRunning = false
        statusText = ""
    }

    fun startVibration() {
        isTimerRunning = false
        isVibrating = true
        statusText = "Time's up!"

        val pattern = longArrayOf(0, 1000, 1000)
        val vibrationEffect = VibrationEffect.createWaveform(pattern, 0)
        vibrator.vibrate(vibrationEffect)
    }

    fun startTimer() {
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
            set(Calendar.MINUTE, timePickerState.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val current = Calendar.getInstance()
        if (!target.after(current)) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }

        val totalMillis = target.timeInMillis - current.timeInMillis
        isTimerRunning = true

        countDownTimer = object : CountDownTimer(totalMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val hoursLeft = (millisUntilFinished / 1000 / 3600).toInt()
                val minutesLeft = ((millisUntilFinished / 1000 % 3600) / 60).toInt()
                statusText = String.format(Locale.getDefault(), "Timer running: %02d:%02d", hoursLeft, minutesLeft)
            }

            override fun onFinish() {
                startVibration()
            }
        }.start()
    }

    DisposableEffect(Unit) {
        onDispose {
            countDownTimer?.cancel()
            vibrator.cancel()
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("OK")
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Wake-up Time",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedButton(
            onClick = { showTimePicker = true },
            enabled = !isTimerRunning && !isVibrating,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(
                text = String.format(Locale.getDefault(), "%02d:%02d", timePickerState.hour, timePickerState.minute),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (statusText.isNotEmpty()) {
            Text(
                text = statusText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isVibrating) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                when {
                    isVibrating -> stopVibration()
                    isTimerRunning -> stopTimer()
                    else -> startTimer()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .height(56.dp)
        ) {
            Text(
                text = if (isTimerRunning || isVibrating) "STOP" else "START",
                fontSize = 18.sp
            )
        }
    }
}
