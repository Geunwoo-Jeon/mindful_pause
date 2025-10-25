package com.geunwoo.jun.mindfulquestion

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import com.geunwoo.jun.mindfulquestion.ui.theme.*
import com.geunwoo.jun.mindfulquestion.ui.GoalSetupActivity
import com.geunwoo.jun.mindfulquestion.ui.GoalEditActivity
import com.geunwoo.jun.mindfulquestion.ui.AnswerHistoryActivity
import com.geunwoo.jun.mindfulquestion.utils.PermissionHelper
import com.geunwoo.jun.mindfulquestion.services.MonitoringService
import com.geunwoo.jun.mindfulquestion.services.AppUsageAccessibilityService
import com.geunwoo.jun.mindfulquestion.data.AppDatabase
import com.geunwoo.jun.mindfulquestion.data.Goal
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : ComponentActivity() {

    private var overlayPermissionGranted by mutableStateOf(false)
    private var notificationPermissionGranted by mutableStateOf(false)
    private var accessibilityServiceEnabled by mutableStateOf(false)
    private var usageStatsPermissionGranted by mutableStateOf(false)
    private var currentGoal by mutableStateOf<Goal?>(null)
    private var isServicePaused by mutableStateOf(false)
    private var remainingPauseTime by mutableStateOf(0L)
    private var selectedInterval by mutableStateOf(10)
    private var isGoalLoaded by mutableStateOf(false)
    private var todayScreenTimeMinutes by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 저장된 간격 불러오기
        selectedInterval = MonitoringService.getInterval(this)

        checkPermissions()
        loadGoal()

        setContent {
            MindfulQuestionTheme {
                MainScreen(
                    overlayPermissionGranted = overlayPermissionGranted,
                    notificationPermissionGranted = notificationPermissionGranted,
                    accessibilityServiceEnabled = accessibilityServiceEnabled,
                    usageStatsPermissionGranted = usageStatsPermissionGranted,
                    currentGoal = currentGoal,
                    isServicePaused = isServicePaused,
                    remainingPauseTime = remainingPauseTime,
                    selectedInterval = selectedInterval,
                    todayScreenTimeMinutes = todayScreenTimeMinutes,
                    onRequestOverlayPermission = {
                        PermissionHelper.requestOverlayPermission(this)
                    },
                    onRequestNotificationPermission = {
                        PermissionHelper.requestNotificationPermission(this)
                    },
                    onRequestAccessibilitySettings = {
                        PermissionHelper.openAccessibilitySettings(this)
                    },
                    onRequestUsageStatsPermission = {
                        PermissionHelper.requestUsageStatsPermission(this)
                    },
                    onPauseService = { durationMinutes ->
                        MonitoringService.pauseService(this, durationMinutes)
                        updatePauseStatus()
                    },
                    onResumeService = {
                        MonitoringService.resumeService(this)
                        updatePauseStatus()
                    },
                    onEditGoal = { goalType ->
                        val intent = Intent(this, GoalEditActivity::class.java)
                        intent.putExtra(GoalEditActivity.EXTRA_GOAL_TYPE, goalType)
                        startActivity(intent)
                    },
                    onIntervalChange = { interval ->
                        selectedInterval = interval
                        MonitoringService.updateInterval(this, interval)
                    },
                    onAnswerHistory = {
                        startActivity(Intent(this, AnswerHistoryActivity::class.java))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
        updatePauseStatus()
        updateScreenTime()

        // 목표 로드 및 설정 화면 이동 (한 번만)
        lifecycleScope.launch {
            val database = AppDatabase.getDatabase(this@MainActivity)
            val hasGoal = database.goalDao().hasGoal()
            isGoalLoaded = true

            // 모든 권한이 허용되었고 목표가 없으면 목표 설정 화면으로 이동
            if (overlayPermissionGranted && notificationPermissionGranted && accessibilityServiceEnabled && usageStatsPermissionGranted && !hasGoal) {
                startActivity(Intent(this@MainActivity, GoalSetupActivity::class.java))
            }
        }
    }

    private fun checkPermissions() {
        overlayPermissionGranted = PermissionHelper.hasOverlayPermission(this)
        notificationPermissionGranted = PermissionHelper.hasNotificationPermission(this)
        accessibilityServiceEnabled = AppUsageAccessibilityService.isServiceEnabled()
        usageStatsPermissionGranted = PermissionHelper.hasUsageStatsPermission(this)

        // 권한이 모두 허용되면 자동으로 서비스 시작
        if (overlayPermissionGranted && notificationPermissionGranted) {
            MonitoringService.startService(this)
        }
    }

    private fun updatePauseStatus() {
        isServicePaused = MonitoringService.isPaused()
        remainingPauseTime = MonitoringService.getRemainingPauseTime()
    }

    private fun updateScreenTime() {
        todayScreenTimeMinutes = getTodayScreenTimeFromSystem()
    }

    /**
     * UsageStatsManager를 통해 시스템의 오늘 스크린타임 가져오기
     */
    private fun getTodayScreenTimeFromSystem(): Int {
        if (!PermissionHelper.hasUsageStatsPermission(this)) {
            return 0
        }

        try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

            // 오늘 자정부터 현재까지의 시간 범위
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startTime = calendar.timeInMillis
            val endTime = System.currentTimeMillis()

            // 오늘의 사용 통계 가져오기
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

            // 모든 앱의 사용 시간 합산 (밀리초 -> 분)
            var totalTimeInMillis = 0L
            stats?.forEach { usageStats ->
                totalTimeInMillis += usageStats.totalTimeInForeground
            }

            return (totalTimeInMillis / 1000 / 60).toInt()
        } catch (e: Exception) {
            e.printStackTrace()
            return 0
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

    private fun loadGoal() {
        lifecycleScope.launch {
            val database = AppDatabase.getDatabase(this@MainActivity)
            database.goalDao().getGoal().collect { goal ->
                currentGoal = goal
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    overlayPermissionGranted: Boolean,
    notificationPermissionGranted: Boolean,
    accessibilityServiceEnabled: Boolean,
    usageStatsPermissionGranted: Boolean,
    currentGoal: Goal?,
    isServicePaused: Boolean,
    remainingPauseTime: Long,
    selectedInterval: Int,
    todayScreenTimeMinutes: Int,
    onRequestOverlayPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestAccessibilitySettings: () -> Unit,
    onRequestUsageStatsPermission: () -> Unit,
    onPauseService: (Int) -> Unit,
    onResumeService: () -> Unit,
    onEditGoal: (String) -> Unit,
    onIntervalChange: (Int) -> Unit,
    onAnswerHistory: () -> Unit
) {
    var showPauseDialog by remember { mutableStateOf(false) }
    var showIntervalDialog by remember { mutableStateOf(false) }
    var showIntervalChangedDialog by remember { mutableStateOf(false) }
    var changedIntervalText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = "앱 아이콘",
                            tint = androidx.compose.ui.graphics.Color.Unspecified,
                            modifier = Modifier
                                .size(60.dp)
                                .offset(x = (-8).dp)
                        )
                        Text(
                            text = "잠시, 멈춤",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.offset(x = (-12).dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        MainScreenContent(
            modifier = Modifier.padding(paddingValues),
            overlayPermissionGranted = overlayPermissionGranted,
            notificationPermissionGranted = notificationPermissionGranted,
            accessibilityServiceEnabled = accessibilityServiceEnabled,
            usageStatsPermissionGranted = usageStatsPermissionGranted,
            currentGoal = currentGoal,
            isServicePaused = isServicePaused,
            remainingPauseTime = remainingPauseTime,
            selectedInterval = selectedInterval,
            todayScreenTimeMinutes = todayScreenTimeMinutes,
            onRequestOverlayPermission = onRequestOverlayPermission,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onRequestAccessibilitySettings = onRequestAccessibilitySettings,
            onRequestUsageStatsPermission = onRequestUsageStatsPermission,
            onPauseService = onPauseService,
            onResumeService = onResumeService,
            onEditGoal = onEditGoal,
            onIntervalChange = onIntervalChange,
            onAnswerHistory = onAnswerHistory,
            showPauseDialog = showPauseDialog,
            onShowPauseDialog = { showPauseDialog = it },
            showIntervalDialog = showIntervalDialog,
            onShowIntervalDialog = { showIntervalDialog = it },
            showIntervalChangedDialog = showIntervalChangedDialog,
            onShowIntervalChangedDialog = { showIntervalChangedDialog = it },
            changedIntervalText = changedIntervalText,
            onChangedIntervalText = { changedIntervalText = it }
        )
    }
}

@Composable
fun MainScreenContent(
    modifier: Modifier = Modifier,
    overlayPermissionGranted: Boolean,
    notificationPermissionGranted: Boolean,
    accessibilityServiceEnabled: Boolean,
    usageStatsPermissionGranted: Boolean,
    currentGoal: Goal?,
    isServicePaused: Boolean,
    remainingPauseTime: Long,
    selectedInterval: Int,
    todayScreenTimeMinutes: Int,
    onRequestOverlayPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestAccessibilitySettings: () -> Unit,
    onRequestUsageStatsPermission: () -> Unit,
    onPauseService: (Int) -> Unit,
    onResumeService: () -> Unit,
    onEditGoal: (String) -> Unit,
    onIntervalChange: (Int) -> Unit,
    onAnswerHistory: () -> Unit,
    showPauseDialog: Boolean,
    onShowPauseDialog: (Boolean) -> Unit,
    showIntervalDialog: Boolean,
    onShowIntervalDialog: (Boolean) -> Unit,
    showIntervalChangedDialog: Boolean,
    onShowIntervalChangedDialog: (Boolean) -> Unit,
    changedIntervalText: String,
    onChangedIntervalText: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 8.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        // 권한이 모두 허용되지 않았을 때만 권한 카드 표시
        val allPermissionsGranted = overlayPermissionGranted && notificationPermissionGranted && accessibilityServiceEnabled && usageStatsPermissionGranted

        // 모든 권한이 허용된 경우에만 상단 정보 카드 표시
        if (allPermissionsGranted) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 리마인드 간격 카드
                val intervalText = when (selectedInterval) {
                    0 -> "30초"
                    else -> "${selectedInterval}분"
                }
                InfoCard(
                    icon = Icons.Default.Notifications,
                    label = "리마인드 간격",
                    value = intervalText,
                    modifier = Modifier.weight(1f),
                    onClick = { onShowIntervalDialog(true) }
                )

                // 오늘의 스크린타임 카드
                InfoCard(
                    icon = Icons.Default.Info,
                    label = "스크린타임",
                    value = "${todayScreenTimeMinutes}분",
                    modifier = Modifier.weight(1f),
                    onClick = null
                )
            }
        }

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
                    description = "설정 > 접근성 > 설치된 서비스 > 잠시, 멈춤 > 사용으로 변경해주세요.",
                    isGranted = accessibilityServiceEnabled,
                    onRequestPermission = onRequestAccessibilitySettings
                )
            }

            if (!usageStatsPermissionGranted) {
                PermissionCard(
                    title = "사용 접근 권한",
                    description = "스크린타임 정보를 표시하기 위해 필요합니다.",
                    isGranted = usageStatsPermissionGranted,
                    onRequestPermission = onRequestUsageStatsPermission
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // 모든 권한이 허용된 경우에만 목표 표시
        if (allPermissionsGranted) {
            // 목표 표시
            if (currentGoal != null) {
                Text(
                    text = "나의 목표",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                GoalCard(
                    title = "5년 내 목표",
                    goal = currentGoal.fiveYearGoal,
                    icon = Icons.Default.FavoriteBorder,
                    onClick = { onEditGoal(GoalEditActivity.GOAL_TYPE_FIVE_YEAR) }
                )
                GoalCard(
                    title = "1년 내 목표",
                    goal = currentGoal.oneYearGoal,
                    icon = Icons.Default.Star,
                    onClick = { onEditGoal(GoalEditActivity.GOAL_TYPE_ONE_YEAR) }
                )
                GoalCard(
                    title = "3개월 내 목표",
                    goal = currentGoal.threeMonthGoal,
                    icon = Icons.Default.Check,
                    onClick = { onEditGoal(GoalEditActivity.GOAL_TYPE_THREE_MONTH) }
                )
            } else {
                Text(
                    text = "목표를 설정해주세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 답변 기록 버튼
            OutlinedButton(
                onClick = onAnswerHistory,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = GreenPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "답변 기록",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "답변 기록 보기",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        // 일시중지 버튼
        if (allPermissionsGranted && overlayPermissionGranted && notificationPermissionGranted) {
            Spacer(modifier = Modifier.height(4.dp))
            if (isServicePaused) {
                // 일시중지 중 - 남은 시간 표시 및 재개 버튼
                var currentRemainingTime by remember { mutableStateOf(remainingPauseTime) }

                // 실시간 업데이트
                LaunchedEffect(isServicePaused) {
                    while (isServicePaused && currentRemainingTime > 0) {
                        delay(1000L)
                        currentRemainingTime = MonitoringService.getRemainingPauseTime()
                    }
                }

                val totalMinutes = (currentRemainingTime / 1000 / 60).toInt()

                val timeText = if (totalMinutes >= 60) {
                    val hours = totalMinutes / 60
                    val minutes = totalMinutes % 60
                    if (minutes > 0) {
                        "${hours}시간 ${minutes}분"
                    } else {
                        "${hours}시간"
                    }
                } else {
                    "${totalMinutes}분"
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = GoalAccent.copy(alpha = 0.1f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "서비스 일시중지 중",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = "남은 시간: $timeText",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Button(
                            onClick = onResumeService,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GreenPrimary
                            )
                        ) {
                            Text("지금 재개하기")
                        }
                    }
                }
            } else {
                // 일시중지 버튼
                Button(
                    onClick = { onShowPauseDialog(true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonBackground,
                        contentColor = ButtonText
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("팝업 일시 중지")
                }
            }
        }
    }

    // 리마인드 간격 선택 다이얼로그
    if (showIntervalDialog) {
        AlertDialog(
            onDismissRequest = { onShowIntervalDialog(false) },
            title = { Text("리마인드 간격 선택") },
            text = {
                Column {
                    Text("얼마나 자주 질문을 받으시겠습니까?")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            onIntervalChange(0)
                            onChangedIntervalText("30초로 (테스트용)")
                            onShowIntervalDialog(false)
                            onShowIntervalChangedDialog(true)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("30초 (테스트용)")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            onIntervalChange(10)
                            onChangedIntervalText("10분으로")
                            onShowIntervalDialog(false)
                            onShowIntervalChangedDialog(true)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("10분")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            onIntervalChange(30)
                            onChangedIntervalText("30분으로")
                            onShowIntervalDialog(false)
                            onShowIntervalChangedDialog(true)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("30분")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            onIntervalChange(60)
                            onChangedIntervalText("1시간으로")
                            onShowIntervalDialog(false)
                            onShowIntervalChangedDialog(true)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("1시간")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { onShowIntervalDialog(false) }) {
                    Text("취소")
                }
            }
        )
    }

    // 리마인드 간격 변경 완료 다이얼로그
    if (showIntervalChangedDialog) {
        AlertDialog(
            onDismissRequest = { onShowIntervalChangedDialog(false) },
            text = { Text("리마인드 간격을 ${changedIntervalText} 변경하였습니다.") },
            confirmButton = {
                TextButton(onClick = { onShowIntervalChangedDialog(false) }) {
                    Text("확인")
                }
            }
        )
    }

    // 일시중지 시간 선택 다이얼로그
    if (showPauseDialog) {
        AlertDialog(
            onDismissRequest = { onShowPauseDialog(false) },
            title = { Text("일시중지") },
            text = {
                Column {
                    Text("얼마나 일시중지하시겠습니까?")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            onPauseService(60)
                            onShowPauseDialog(false)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("1시간")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            onPauseService(180)
                            onShowPauseDialog(false)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("3시간")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            // 내일 오전 7시까지 일시중지
                            val calendar = Calendar.getInstance()
                            calendar.add(Calendar.DAY_OF_YEAR, 1)
                            calendar.set(Calendar.HOUR_OF_DAY, 7)
                            calendar.set(Calendar.MINUTE, 0)
                            calendar.set(Calendar.SECOND, 0)
                            val now = System.currentTimeMillis()
                            val pauseMinutes = ((calendar.timeInMillis - now) / 1000 / 60).toInt()
                            onPauseService(pauseMinutes)
                            onShowPauseDialog(false)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("내일 오전 7시까지")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { onShowPauseDialog(false) }) {
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
                        Text("허용하기")
                    }
                }
            }
        }
    }
}

@Composable
fun InfoCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick ?: {},
        enabled = onClick != null
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = TextPrimary
            )
        }
    }
}

@Composable
fun GoalCard(
    title: String,
    goal: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    // 모든 목표에 동일한 그린 톤 적용
    val accentColor = GoalAccent
    val backgroundColor = CardBackground

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 좌측 그린 색상 바
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(70.dp)
                    .background(accentColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Text(
                    text = goal,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = TextPrimary
                )
            }

            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "수정",
                modifier = Modifier
                    .size(18.dp)
                    .padding(end = 16.dp),
                tint = TextTertiary
            )
        }
    }
}
