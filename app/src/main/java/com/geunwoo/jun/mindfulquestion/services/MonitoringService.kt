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
    private var shouldShowPopup = false
    private var isPaused = false
    private var pauseEndTime = 0L // 일시중지 종료 시간

    private lateinit var powerManager: PowerManager
    private lateinit var screenReceiver: BroadcastReceiver

    companion object {
        const val CHANNEL_ID = "mindful_question_channel"
        const val NOTIFICATION_ID = 1
        // const val TARGET_INTERVAL = 10 * 60 * 1000L // 10분 (밀리초)
        const val TARGET_INTERVAL = 30 * 1000L // 테스트용: 30초
        const val TICK_INTERVAL = 1000L // 1초마다 체크

        private var instance: MonitoringService? = null

        fun getInstance(): MonitoringService? = instance

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

        fun pauseService(context: Context, durationMinutes: Int) {
            getInstance()?.pause(durationMinutes)
        }

        fun resumeService(context: Context) {
            getInstance()?.resume()
        }

        fun isPaused(): Boolean {
            return getInstance()?.isPaused ?: false
        }

        fun getRemainingPauseTime(): Long {
            val instance = getInstance() ?: return 0
            if (!instance.isPaused) return 0
            val remaining = instance.pauseEndTime - System.currentTimeMillis()
            return if (remaining > 0) remaining else 0
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
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
        instance = null
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
                "마음챙김 질문 서비스",
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

        val contentText = if (isPaused) {
            val remainingMinutes = ((pauseEndTime - System.currentTimeMillis()) / 1000 / 60).toInt()
            "일시중지 중 (${remainingMinutes}분 남음)"
        } else {
            "스크린 타임: ${seconds}초 / 30초 (테스트)"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("마음챙김 질문 실행 중")
            .setContentText(contentText)
            .setSmallIcon(com.geunwoo.jun.mindfulquestion.R.drawable.ic_notification_praying_hands)
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

                // 일시중지 상태 확인
                if (isPaused) {
                    if (currentTime >= pauseEndTime) {
                        // 일시중지 종료
                        isPaused = false
                        pauseEndTime = 0L
                        android.util.Log.d("MonitoringService", "일시중지 종료 - 타이머 재개")
                        updateNotification()
                    } else {
                        // 일시중지 중 - 알림만 업데이트
                        android.util.Log.d("MonitoringService", "일시중지 중")
                        updateNotification()
                        lastTickTime = currentTime
                        handler.postDelayed(this, TICK_INTERVAL)
                        return
                    }
                }

                // 화면이 켜져 있고, 팝업이 표시되지 않았을 때만 시간 누적
                if (isScreenOn && !shouldShowPopup) {
                    val elapsedTime = currentTime - lastTickTime
                    accumulatedTime += elapsedTime
                    android.util.Log.d("MonitoringService", "스크린타임 카운트 중: ${accumulatedTime / 1000}초")
                } else {
                    if (shouldShowPopup) {
                        android.util.Log.d("MonitoringService", "팝업 표시 중 - 스크린타임 카운트 중지")
                    }
                }

                // 알림 업데이트 (5초마다)
                if (accumulatedTime % 5000 < TICK_INTERVAL) {
                    updateNotification()
                }

                // 10분 도달 시 팝업 실행
                if (accumulatedTime >= TARGET_INTERVAL) {
                    onTimerComplete()
                    accumulatedTime = 0 // 타이머 리셋
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
        android.util.Log.d("MonitoringService", "스크린 타임 10분 도달! 팝업 실행 - 스크린타임 중지")

        shouldShowPopup = true
        AppUsageAccessibilityService.setShouldShowPopupAgain(true)
        AppUsageAccessibilityService.setPopupShowing(true)

        // PopupActivity 실행
        showPopup()
    }

    private fun showPopup() {
        val intent = Intent(this, com.geunwoo.jun.mindfulquestion.ui.PopupActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    fun notifyPopupCompleted() {
        shouldShowPopup = false
    }

    fun pause(durationMinutes: Int) {
        isPaused = true
        pauseEndTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        android.util.Log.d("MonitoringService", "${durationMinutes}분 동안 일시중지")
        updateNotification()
    }

    fun resume() {
        isPaused = false
        pauseEndTime = 0L
        android.util.Log.d("MonitoringService", "일시중지 해제")
        updateNotification()
    }
}
