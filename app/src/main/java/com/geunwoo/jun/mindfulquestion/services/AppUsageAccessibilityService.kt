package com.geunwoo.jun.mindfulquestion.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.geunwoo.jun.mindfulquestion.ui.PopupActivity

class AppUsageAccessibilityService : AccessibilityService() {

    companion object {
        private var instance: AppUsageAccessibilityService? = null
        private var isPopupShowing = false
        private var shouldShowPopupAgain = false

        fun isServiceEnabled(): Boolean = instance != null

        fun setPopupShowing(showing: Boolean) {
            isPopupShowing = showing
        }

        fun setShouldShowPopupAgain(should: Boolean) {
            shouldShowPopupAgain = should
        }

        fun isPopupCurrentlyShowing(): Boolean = isPopupShowing
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        android.util.Log.d("AccessibilityService", "서비스 연결됨")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            val className = event.className?.toString() ?: return

            android.util.Log.d("AccessibilityService", "앱 전환: $packageName / $className")

            // PopupActivity가 화면에 표시되는지 확인
            val isPopupActivity = className.contains("PopupActivity")

            if (isPopupActivity) {
                // 팝업이 표시됨
                isPopupShowing = true
                shouldShowPopupAgain = false
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
