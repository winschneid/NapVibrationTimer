package com.example.napvibrationtimer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimerService : Service() {

    enum class State { IDLE, RUNNING, VIBRATING }

    companion object {
        private val _state = MutableStateFlow(State.IDLE)
        val state: StateFlow<State> = _state.asStateFlow()

        const val ACTION_STOP = "com.example.napvibrationtimer.STOP"
        const val EXTRA_TARGET_TIME = "target_time"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "timer_channel"
    }

    private var countDownTimer: CountDownTimer? = null
    private lateinit var vibrator: Vibrator
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val targetTimeMs = intent?.getLongExtra(EXTRA_TARGET_TIME, 0L) ?: 0L
        val totalMillis = targetTimeMs - System.currentTimeMillis()

        if (totalMillis <= 0) {
            stopSelf()
            return START_NOT_STICKY
        }

        _state.value = State.RUNNING
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Alarm running..."))

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "NapVibrationTimer::TimerWakeLock"
        ).apply { acquire(totalMillis + 60_000) }

        countDownTimer = object : CountDownTimer(totalMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                startVibration()
            }
        }.start()

        return START_NOT_STICKY
    }

    private fun startVibration() {
        countDownTimer = null
        _state.value = State.VIBRATING

        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 1000, 1000), 0))

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, buildNotification("Time's up! Tap Stop to dismiss."))
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        vibrator.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        _state.value = State.IDLE
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Vibration Timer",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, TimerService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Nap Vibration Timer")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(openIntent)
            .addAction(0, "Stop", stopIntent)
            .setOngoing(true)
            .build()
    }
}
