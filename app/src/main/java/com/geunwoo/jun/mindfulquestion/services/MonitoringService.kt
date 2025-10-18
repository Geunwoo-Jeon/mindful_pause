package com.geunwoo.jun.mindfulquestion.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.geunwoo.jun.mindfulquestion.MainActivity

class MonitoringService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null
    private var accumulatedTime = 0L // 누적 스크린 타임 (밀리초)
    private var lastTickTime = 0L
    private var isScreenOn = false

    private lateinit var powerManager: PowerManager
    private lateinit var screenReceiver: BroadcastReceiver

    companion object {
        const val CHANNEL_ID = "mindful_question_channel"
        const val NOTIFICATION_ID = 1
        const val TARGET_INTERVAL = 10 * 60 * 1000L // 10분 (밀리초)
        const val TICK_INTERVAL = 1000L // 1초마다 체크
        // const val TARGET_INTERVAL = 30 * 1000L // 테스트용: 30초

        fun startService(context: Context) {
            val intent = Intent(context, MonitoringService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, MonitoringService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        registerScreenReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Foreground 알림 시작
        startForeground(NOTIFICATION_ID, createNotification())

        // 현재 화면 상태 확인
        isScreenOn = powerManager.isInteractive

        // 타이머 시작
        startTimer()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
        unregisterReceiver(screenReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun registerScreenReceiver() {
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        isScreenOn = true
                        lastTickTime = System.currentTimeMillis()
                        android.util.Log.d("MonitoringService", "화면 켜짐 - 타이머 재개")
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        isScreenOn = false
                        android.util.Log.d("MonitoringService", "화면 꺼짐 - 타이머 일시정지")
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "마인드풀 질문 서비스",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "자기 성찰을 위한 질문 알림"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val minutes = (accumulatedTime / 1000 / 60).toInt()
        val seconds = ((accumulatedTime / 1000) % 60).toInt()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("마인드풀 질문 실행 중")
            .setContentText("스크린 타임: ${minutes}분 ${seconds}초 / 10분")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun startTimer() {
        lastTickTime = System.currentTimeMillis()

        timerRunnable = object : Runnable {
            override fun run() {
                val currentTime = System.currentTimeMillis()

                // 화면이 켜져 있을 때만 시간 누적
                if (isScreenOn) {
                    val elapsedTime = currentTime - lastTickTime
                    accumulatedTime += elapsedTime

                    // 알림 업데이트 (5초마다)
                    if (accumulatedTime % 5000 < TICK_INTERVAL) {
                        updateNotification()
                    }

                    // 10분 도달 시 팝업 실행
                    if (accumulatedTime >= TARGET_INTERVAL) {
                        onTimerComplete()
                        accumulatedTime = 0 // 타이머 리셋
                    }
                }

                lastTickTime = currentTime

                // 다음 틱 예약
                handler.postDelayed(this, TICK_INTERVAL)
            }
        }

        // 첫 타이머 시작
        handler.postDelayed(timerRunnable!!, TICK_INTERVAL)
    }

    private fun stopTimer() {
        timerRunnable?.let {
            handler.removeCallbacks(it)
        }
        timerRunnable = null
    }

    private fun onTimerComplete() {
        android.util.Log.d("MonitoringService", "스크린 타임 10분 도달! 팝업 실행")

        // TODO: Phase 3에서 PopupActivity 실행 로직 추가
        // val intent = Intent(this, PopupActivity::class.java)
        // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // startActivity(intent)
    }
}
