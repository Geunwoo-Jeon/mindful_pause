package com.geunwoo.jun.mindfulquestion.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.geunwoo.jun.mindfulquestion.ui.theme.*
import com.geunwoo.jun.mindfulquestion.services.AppUsageAccessibilityService
import com.geunwoo.jun.mindfulquestion.services.MonitoringService
import com.geunwoo.jun.mindfulquestion.data.QuestionSet
import com.geunwoo.jun.mindfulquestion.data.QuestionStep
import com.geunwoo.jun.mindfulquestion.data.AppDatabase
import com.geunwoo.jun.mindfulquestion.data.Goal
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PopupActivity : ComponentActivity() {

    private var currentGoal by mutableStateOf<Goal?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 팝업이 표시됨을 알림
        android.util.Log.d("PopupActivity", "팝업 시작 - 스크린타임 카운트 중지")
        AppUsageAccessibilityService.setPopupShowing(true)
        AppUsageAccessibilityService.setShouldShowPopupAgain(true)

        // 목표 불러오기
        lifecycleScope.launch {
            val database = AppDatabase.getDatabase(this@PopupActivity)
            database.goalDao().getGoal().collect { goal ->
                currentGoal = goal
            }
        }

        // 키보드가 화면을 밀어올리도록 설정
        window.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        setContent {
            MindfulQuestionTheme {
                QuestionSetFlow(
                    questionSet = QuestionSet.DEFAULT_V2,
                    currentGoal = currentGoal,
                    onComplete = { questions, answers ->
                        // 답변 저장
                        lifecycleScope.launch {
                            val database = AppDatabase.getDatabase(this@PopupActivity)
                            val answerRecord = com.geunwoo.jun.mindfulquestion.data.AnswerRecord(
                                timestamp = System.currentTimeMillis(),
                                question1 = questions.getOrNull(0) ?: "",
                                answer1 = answers.getOrNull(0) ?: "",
                                question2 = questions.getOrNull(1) ?: "",
                                answer2 = answers.getOrNull(1) ?: "",
                                question3 = questions.getOrNull(2) ?: "",
                                answer3 = answers.getOrNull(2) ?: ""
                            )
                            database.answerRecordDao().insert(answerRecord)
                        }

                        // 팝업 완료됨을 알림
                        AppUsageAccessibilityService.setPopupShowing(false)
                        AppUsageAccessibilityService.setShouldShowPopupAgain(false)
                        MonitoringService.getInstance()?.notifyPopupCompleted()

                        android.util.Log.d("PopupActivity", "질문 완료 및 답변 저장")
                        finish() // 팝업 닫기
                    }
                )
            }
        }
    }

    // 뒤로가기 버튼 무효화
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // 아무것도 하지 않음 - 뒤로가기 버튼 무시
    }
}

@Composable
fun QuestionSetFlow(
    questionSet: QuestionSet,
    currentGoal: Goal?,
    onComplete: (questions: List<String>, answers: List<String>) -> Unit
) {
    var currentStepIndex by remember { mutableStateOf(0) }

    // 답변 저장을 위한 상태
    val answers = remember { mutableStateListOf<String>() }
    val questions = remember { mutableStateListOf<String>() }

    // 답변 단계 인덱스 계산
    val inputSteps = questionSet.steps.filterIsInstance<QuestionStep.InputStep>()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AnimatedContent(
            targetState = currentStepIndex,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith
                        fadeOut(animationSpec = tween(300))
            },
            label = "step_transition"
        ) { stepIndex ->
            when (val step = questionSet.steps[stepIndex]) {
                is QuestionStep.MessageStep -> {
                    MessageStepScreen(
                        message = step.message,
                        buttonText = step.buttonText,
                        delaySeconds = step.delaySeconds,
                        onNext = {
                            if (stepIndex < questionSet.steps.size - 1) {
                                currentStepIndex++
                            } else {
                                onComplete(questions.toList(), answers.toList())
                            }
                        }
                    )
                }
                is QuestionStep.InputStep -> {
                    val answerIndex = inputSteps.indexOf(step)
                    InputStepScreen(
                        questionText = step.questionText,
                        hintText = step.hintText,
                        stepNumber = answerIndex + 1,
                        totalSteps = inputSteps.size,
                        currentGoal = currentGoal,
                        onNext = { answer ->
                            // 답변 저장
                            questions.add(step.questionText)
                            answers.add(answer)

                            if (stepIndex < questionSet.steps.size - 1) {
                                currentStepIndex++
                            } else {
                                onComplete(questions.toList(), answers.toList())
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MessageStepScreen(
    message: String,
    buttonText: String,
    delaySeconds: Int,
    onNext: () -> Unit
) {
    var isButtonEnabled by remember { mutableStateOf(delaySeconds == 0) }

    LaunchedEffect(Unit) {
        if (delaySeconds > 0) {
            delay(delaySeconds * 1000L)
            isButtonEnabled = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        Button(
            onClick = onNext,
            enabled = isButtonEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isButtonEnabled) {
                    GreenPrimary
                } else {
                    CardBackgroundTint
                },
                contentColor = if (isButtonEnabled) {
                    CardBackground
                } else {
                    TextTertiary
                }
            )
        ) {
            Text(
                text = if (isButtonEnabled) buttonText else "잠시 호흡해보세요",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
fun InputStepScreen(
    questionText: String,
    hintText: String,
    stepNumber: Int,
    totalSteps: Int,
    currentGoal: Goal?,
    onNext: (answer: String) -> Unit
) {
    var answer by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val topPadding = screenHeight * 0.15f

    val isAnswerValid = answer.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(topPadding))

        // 진행 상황
        Text(
            text = "질문 $stepNumber/$totalSteps",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 두 번째, 세 번째 질문일 때 목표 표시
        if (stepNumber >= 2 && currentGoal != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = GoalAccent.copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "3개월 내 목표",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                    Text(
                        text = currentGoal.threeMonthGoal,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = TextPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 질문
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = CardBackground
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = questionText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp),
                    minLines = 4,
                    maxLines = 8,
                    placeholder = {
                        Text(
                            text = hintText,
                            color = TextTertiary
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = BorderLight,
                        focusedContainerColor = CardBackground,
                        unfocusedContainerColor = CardBackground
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 다음 버튼
        Button(
            onClick = { onNext(answer) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = isAnswerValid,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isAnswerValid) {
                    GreenPrimary
                } else {
                    CardBackgroundTint
                },
                contentColor = if (isAnswerValid) {
                    CardBackground
                } else {
                    TextTertiary
                }
            )
        ) {
            Text(
                text = if (stepNumber == totalSteps) "제출 및 저장" else "다음 질문으로",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}
