package com.geunwoo.jun.mindfulquestion.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionHelper {

    const val NOTIFICATION_PERMISSION_CODE = 100
    const val OVERLAY_PERMISSION_CODE = 101
    const val ACCESSIBILITY_SETTINGS_CODE = 102

    /**
     * 오버레이 권한 확인
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * 오버레이 권한 요청 화면으로 이동
     */
    fun requestOverlayPermission(activity: Activity) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${activity.packageName}")
        )
        activity.startActivityForResult(intent, OVERLAY_PERMISSION_CODE)
    }

    /**
     * 알림 권한 확인 (Android 13+)
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 13 이하는 자동으로 허용됨
        }
    }

    /**
     * 알림 권한 요청
     */
    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_CODE
            )
        }
    }

    /**
     * Accessibility 서비스 활성화 여부 확인
     */
    fun isAccessibilityServiceEnabled(context: Context, serviceName: String): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(serviceName) == true
    }

    /**
     * Accessibility 설정 화면으로 이동
     */
    fun openAccessibilitySettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        activity.startActivityForResult(intent, ACCESSIBILITY_SETTINGS_CODE)
    }

    /**
     * 모든 필수 권한이 허용되었는지 확인
     */
    fun hasAllPermissions(context: Context): Boolean {
        return hasOverlayPermission(context) && hasNotificationPermission(context)
    }
}
