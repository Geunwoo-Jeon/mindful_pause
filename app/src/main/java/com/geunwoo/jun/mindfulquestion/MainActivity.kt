package com.geunwoo.jun.mindfulquestion

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.geunwoo.jun.mindfulquestion.ui.theme.MindfulQuestionTheme
import com.geunwoo.jun.mindfulquestion.utils.PermissionHelper
import com.geunwoo.jun.mindfulquestion.services.MonitoringService
import com.geunwoo.jun.mindfulquestion.services.AppUsageAccessibilityService

class MainActivity : ComponentActivity() {

    private var overlayPermissionGranted by mutableStateOf(false)
    private var notificationPermissionGranted by mutableStateOf(false)
    private var accessibilityServiceEnabled by mutableStateOf(false)
    private var isServiceRunning by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkPermissions()

        setContent {
            MindfulQuestionTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        overlayPermissionGranted = overlayPermissionGranted,
                        notificationPermissionGranted = notificationPermissionGranted,
                        accessibilityServiceEnabled = accessibilityServiceEnabled,
                        isServiceRunning = isServiceRunning,
                        onRequestOverlayPermission = {
                            PermissionHelper.requestOverlayPermission(this)
                        },
                        onRequestNotificationPermission = {
                            PermissionHelper.requestNotificationPermission(this)
                        },
                        onRequestAccessibilitySettings = {
                            PermissionHelper.openAccessibilitySettings(this)
                        },
                        onStartService = {
                            MonitoringService.startService(this)
                            isServiceRunning = true
                        },
                        onStopService = {
                            MonitoringService.stopService(this)
                            isServiceRunning = false
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun checkPermissions() {
        overlayPermissionGranted = PermissionHelper.hasOverlayPermission(this)
        notificationPermissionGranted = PermissionHelper.hasNotificationPermission(this)
        accessibilityServiceEnabled = AppUsageAccessibilityService.isServiceEnabled()

        // 권한이 모두 허용되면 자동으로 서비스 시작
        if (overlayPermissionGranted && notificationPermissionGranted && !isServiceRunning) {
            MonitoringService.startService(this)
            isServiceRunning = true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        checkPermissions()
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    overlayPermissionGranted: Boolean,
    notificationPermissionGranted: Boolean,
    accessibilityServiceEnabled: Boolean,
    isServiceRunning: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestAccessibilitySettings: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "마음챙김 질문",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "10분마다 자기 성찰을 위한 질문을 받습니다.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 권한 상태 카드들
        PermissionCard(
            title = "오버레이 권한",
            description = "다른 앱 위에 팝업을 표시하기 위해 필요합니다.",
            isGranted = overlayPermissionGranted,
            onRequestPermission = onRequestOverlayPermission
        )

        PermissionCard(
            title = "알림 권한",
            description = "서비스 실행 알림을 위해 필요합니다.",
            isGranted = notificationPermissionGranted,
            onRequestPermission = onRequestNotificationPermission
        )

        PermissionCard(
            title = "접근성 서비스",
            description = "설정 > 접근성 > 설치된 서비스 > 마음챙김 질문 > 사용으로 변경해주세요.",
            isGranted = accessibilityServiceEnabled,
            onRequestPermission = onRequestAccessibilitySettings
        )

        Spacer(modifier = Modifier.weight(1f))

        // 서비스 시작/중지 버튼
        if (!isServiceRunning) {
            Button(
                onClick = onStartService,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = overlayPermissionGranted && notificationPermissionGranted
            ) {
                Text("서비스 시작")
            }
        } else {
            Button(
                onClick = onStopService,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("서비스 중지")
            }
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isGranted) {
                    Text(
                        text = "✓",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    TextButton(onClick = onRequestPermission) {
                        Text("허용")
                    }
                }
            }
        }
    }
}