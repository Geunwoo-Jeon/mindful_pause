package com.geunwoo.jun.mindfulquestion.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.geunwoo.jun.mindfulquestion.services.MonitoringService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // 부팅 완료 시 서비스 자동 시작
            MonitoringService.startService(context)
            android.util.Log.d("BootReceiver", "부팅 완료 - 서비스 자동 시작")
        }
    }
}
