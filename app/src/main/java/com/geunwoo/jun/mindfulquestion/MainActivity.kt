package com.geunwoo.jun.mindfulquestion

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.geunwoo.jun.mindfulquestion.ui.theme.MindfulQuestionTheme
import com.geunwoo.jun.mindfulquestion.utils.PermissionHelper
import com.geunwoo.jun.mindfulquestion.services.MonitoringService
import com.geunwoo.jun.mindfulquestion.services.AppUsageAccessibilityService
import com.geunwoo.jun.mindfulquestion.data.AppDatabase
import com.geunwoo.jun.mindfulquestion.data.Answer
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var overlayPermissionGranted by mutableStateOf(false)
    private var notificationPermissionGranted by mutableStateOf(false)
    private var accessibilityServiceEnabled by mutableStateOf(false)
    private var answerList by mutableStateOf<List<Answer>>(emptyList())
    private var isServicePaused by mutableStateOf(false)
    private var remainingPauseTime by mutableStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkPermissions()
        loadAnswers()

        setContent {
            MindfulQuestionTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        overlayPermissionGranted = overlayPermissionGranted,
                        notificationPermissionGranted = notificationPermissionGranted,
                        accessibilityServiceEnabled = accessibilityServiceEnabled,
                        answerList = answerList,
                        isServicePaused = isServicePaused,
                        remainingPauseTime = remainingPauseTime,
                        onRequestOverlayPermission = {
                            PermissionHelper.requestOverlayPermission(this)
                        },
                        onRequestNotificationPermission = {
                            PermissionHelper.requestNotificationPermission(this)
                        },
                        onRequestAccessibilitySettings = {
                            PermissionHelper.openAccessibilitySettings(this)
                        },
                        onPauseService = { durationMinutes ->
                            MonitoringService.pauseService(this, durationMinutes)
                            updatePauseStatus()
                        },
                        onResumeService = {
                            MonitoringService.resumeService(this)
                            updatePauseStatus()
                        },
                        onDeleteAnswer = { answer ->
                            lifecycleScope.launch {
                                val database = AppDatabase.getDatabase(this@MainActivity)
                                database.answerDao().delete(answer)
                                loadAnswers()
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
        loadAnswers()
        updatePauseStatus()
    }

    private fun checkPermissions() {
        overlayPermissionGranted = PermissionHelper.hasOverlayPermission(this)
        notificationPermissionGranted = PermissionHelper.hasNotificationPermission(this)
        accessibilityServiceEnabled = AppUsageAccessibilityService.isServiceEnabled()

        // 권한이 모두 허용되면 자동으로 서비스 시작
        if (overlayPermissionGranted && notificationPermissionGranted) {
            MonitoringService.startService(this)
        }
    }

    private fun updatePauseStatus() {
        isServicePaused = MonitoringService.isPaused()
        remainingPauseTime = MonitoringService.getRemainingPauseTime()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        checkPermissions()
    }

    private fun loadAnswers() {
        lifecycleScope.launch {
            val database = AppDatabase.getDatabase(this@MainActivity)
            answerList = database.answerDao().getAllAnswers()
        }
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    overlayPermissionGranted: Boolean,
    notificationPermissionGranted: Boolean,
    accessibilityServiceEnabled: Boolean,
    answerList: List<Answer>,
    isServicePaused: Boolean,
    remainingPauseTime: Long,
    onRequestOverlayPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestAccessibilitySettings: () -> Unit,
    onPauseService: (Int) -> Unit,
    onResumeService: () -> Unit,
    onDeleteAnswer: (Answer) -> Unit
) {
    var showPauseDialog by remember { mutableStateOf(false) }
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

        Spacer(modifier = Modifier.height(16.dp))

        // 권한이 모두 허용되지 않았을 때만 권한 카드 표시
        val allPermissionsGranted = overlayPermissionGranted && notificationPermissionGranted && accessibilityServiceEnabled

        if (!allPermissionsGranted) {
            // 권한 상태 카드들
            if (!overlayPermissionGranted) {
                PermissionCard(
                    title = "오버레이 권한",
                    description = "다른 앱 위에 팝업을 표시하기 위해 필요합니다.",
                    isGranted = overlayPermissionGranted,
                    onRequestPermission = onRequestOverlayPermission
                )
            }

            if (!notificationPermissionGranted) {
                PermissionCard(
                    title = "알림 권한",
                    description = "서비스 실행 알림을 위해 필요합니다.",
                    isGranted = notificationPermissionGranted,
                    onRequestPermission = onRequestNotificationPermission
                )
            }

            if (!accessibilityServiceEnabled) {
                PermissionCard(
                    title = "접근성 서비스",
                    description = "설정 > 접근성 > 설치된 서비스 > 마음챙김 질문 > 사용으로 변경해주세요.",
                    isGranted = accessibilityServiceEnabled,
                    onRequestPermission = onRequestAccessibilitySettings
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 답변 기록 섹션
        Text(
            text = "답변 기록 (${answerList.size}개)",
            style = MaterialTheme.typography.titleLarge
        )

        // 답변 목록
        if (answerList.isEmpty()) {
            Text(
                text = "아직 답변 기록이 없습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(answerList) { answer ->
                    AnswerCard(
                        answer = answer,
                        onDelete = { onDeleteAnswer(answer) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 일시중지 버튼
        if (overlayPermissionGranted && notificationPermissionGranted) {
            if (isServicePaused) {
                // 일시중지 중 - 남은 시간 표시 및 재개 버튼
                val remainingMinutes = (remainingPauseTime / 1000 / 60).toInt()
                val remainingSeconds = ((remainingPauseTime / 1000) % 60).toInt()

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "서비스 일시중지 중",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "남은 시간: ${remainingMinutes}분 ${remainingSeconds}초",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Button(
                            onClick = onResumeService,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            Text("지금 재개하기")
                        }
                    }
                }
            } else {
                // 일시중지 버튼
                Button(
                    onClick = { showPauseDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("일시중지")
                }
            }
        }
    }

    // 일시중지 시간 선택 다이얼로그
    if (showPauseDialog) {
        AlertDialog(
            onDismissRequest = { showPauseDialog = false },
            title = { Text("일시중지") },
            text = {
                Column {
                    Text("얼마나 일시중지하시겠습니까?")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            onPauseService(30)
                            showPauseDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("30분")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            onPauseService(60)
                            showPauseDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("1시간")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            onPauseService(120)
                            showPauseDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("2시간")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPauseDialog = false }) {
                    Text("취소")
                }
            }
        )
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

@Composable
fun AnswerCard(
    answer: Answer,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(answer.timestamp))

    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "삭제",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "현재 하고 있는 일:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = answer.answer1,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "앞으로 할 일:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = answer.answer2,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }

    // 삭제 확인 다이얼로그
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("답변 삭제") },
            text = { Text("이 답변을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}