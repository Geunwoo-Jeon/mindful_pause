package com.geunwoo.jun.mindfulquestion.services

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.geunwoo.jun.mindfulquestion.ui.PopupActivity

class AppUsageAccessibilityService : AccessibilityService() {

    companion object {
        private var instance: AppUsageAccessibilityService? = null
        private var isPopupShowing = false
        private var shouldShowPopupAgain = false
        private var todayScreenTimeMillis = 0L
        private var lastEventTime = 0L
        private var currentDayOfYear = 0
        private const val PREFS_NAME = "screen_time_prefs"
        private const val KEY_SCREEN_TIME = "screen_time_millis"
        private const val KEY_DAY_OF_YEAR = "day_of_year"
        private const val KEY_LAST_EVENT_TIME = "last_event_time"

        fun isServiceEnabled(): Boolean = instance != null

        fun setPopupShowing(showing: Boolean) {
            isPopupShowing = showing
        }

        fun setShouldShowPopupAgain(should: Boolean) {
            shouldShowPopupAgain = should
        }

        fun isPopupCurrentlyShowing(): Boolean = isPopupShowing

        fun getTodayScreenTimeMinutes(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val calendar = java.util.Calendar.getInstance()
            val today = calendar.get(java.util.Calendar.DAY_OF_YEAR)

            // 저장된 날짜 가져오기
            val savedDayOfYear = prefs.getInt(KEY_DAY_OF_YEAR, 0)

            // 날짜가 바뀌면 스크린타임 리셋
            if (today != savedDayOfYear) {
                todayScreenTimeMillis = 0L
                currentDayOfYear = today
                prefs.edit()
                    .putLong(KEY_SCREEN_TIME, 0L)
                    .putInt(KEY_DAY_OF_YEAR, today)
                    .apply()
            } else {
                // 같은 날이면 저장된 값 로드
                todayScreenTimeMillis = prefs.getLong(KEY_SCREEN_TIME, 0L)
                currentDayOfYear = savedDayOfYear
            }

            return (todayScreenTimeMillis / 1000 / 60).toInt()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        // 저장된 스크린타임 데이터 로드
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val calendar = java.util.Calendar.getInstance()
        val today = calendar.get(java.util.Calendar.DAY_OF_YEAR)
        val savedDayOfYear = prefs.getInt(KEY_DAY_OF_YEAR, 0)

        if (today == savedDayOfYear) {
            // 같은 날이면 저장된 값 로드
            todayScreenTimeMillis = prefs.getLong(KEY_SCREEN_TIME, 0L)
            lastEventTime = prefs.getLong(KEY_LAST_EVENT_TIME, System.currentTimeMillis())
        } else {
            // 날짜가 바뀌었으면 초기화
            todayScreenTimeMillis = 0L
            lastEventTime = System.currentTimeMillis()
            prefs.edit()
                .putLong(KEY_SCREEN_TIME, 0L)
                .putInt(KEY_DAY_OF_YEAR, today)
                .putLong(KEY_LAST_EVENT_TIME, lastEventTime)
                .apply()
        }

        currentDayOfYear = today
        android.util.Log.d("AccessibilityService", "서비스 연결됨 - 스크린타임: ${todayScreenTimeMillis / 1000 / 60}분")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            val className = event.className?.toString() ?: return

            // 스크린타임 추적
            val currentTime = System.currentTimeMillis()
            val timeSinceLastEvent = currentTime - lastEventTime

            // 합리적인 시간 범위 내의 이벤트만 카운트 (60초 이하)
            if (timeSinceLastEvent in 0..60000) {
                todayScreenTimeMillis += timeSinceLastEvent

                // SharedPreferences에 저장
                val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putLong(KEY_SCREEN_TIME, todayScreenTimeMillis)
                    .putLong(KEY_LAST_EVENT_TIME, currentTime)
                    .apply()
            }
            lastEventTime = currentTime

            android.util.Log.d("AccessibilityService", "앱 전환: $packageName / $className")

            // PopupActivity가 화면에 표시되는지 확인
            val isPopupActivity = className.contains("PopupActivity")

            if (isPopupActivity) {
                // 팝업이 표시됨
                isPopupShowing = true
                // shouldShowPopupAgain은 그대로 유지 (사용자가 답변을 완료할 때만 false로 변경)
            } else {
                // 다른 앱으로 전환
                isPopupShowing = false

                // 팝업을 다시 띄워야 하는 상황이면 팝업 재실행
                if (shouldShowPopupAgain) {
                    android.util.Log.d("AccessibilityService", "팝업 다시 띄우기")
                    val intent = Intent(this, PopupActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                }
            }
        }
    }

    override fun onInterrupt() {
        android.util.Log.d("AccessibilityService", "서비스 중단됨")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        android.util.Log.d("AccessibilityService", "서비스 종료됨")
    }
}
