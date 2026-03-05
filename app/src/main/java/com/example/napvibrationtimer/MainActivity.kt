package com.example.napvibrationtimer

import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.NumberPicker
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
import androidx.compose.ui.viewinterop.AndroidView

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

@Composable
fun VibrationTimerScreen() {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(ComponentActivity.VIBRATOR_SERVICE) as Vibrator }

    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(10) }
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
        val totalMillis = (hours * 3600 + minutes * 60) * 1000L

        if (totalMillis == 0L) {
            return
        }

        isTimerRunning = true

        countDownTimer = object : CountDownTimer(totalMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val hoursLeft = (millisUntilFinished / 1000 / 3600).toInt()
                val minutesLeft = ((millisUntilFinished / 1000 % 3600) / 60).toInt()
                statusText = "Timer running: %02d:%02d".format(hoursLeft, minutesLeft)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Hours",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                AndroidView(
                    factory = { context ->
                        NumberPicker(context).apply {
                            minValue = 0
                            maxValue = 23
                            value = hours
                            setOnValueChangedListener { _, _, newVal ->
                                hours = newVal
                            }
                        }
                    },
                    update = { picker ->
                        picker.isEnabled = !isTimerRunning && !isVibrating
                    }
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Minutes",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                AndroidView(
                    factory = { context ->
                        NumberPicker(context).apply {
                            minValue = 0
                            maxValue = 59
                            value = minutes
                            setOnValueChangedListener { _, _, newVal ->
                                minutes = newVal
                            }
                        }
                    },
                    update = { picker ->
                        picker.isEnabled = !isTimerRunning && !isVibrating
                    }
                )
            }
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
