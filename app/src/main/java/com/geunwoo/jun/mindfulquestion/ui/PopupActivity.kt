package com.geunwoo.jun.mindfulquestion.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.geunwoo.jun.mindfulquestion.ui.theme.MindfulQuestionTheme
import com.geunwoo.jun.mindfulquestion.services.AppUsageAccessibilityService
import com.geunwoo.jun.mindfulquestion.services.MonitoringService
import com.geunwoo.jun.mindfulquestion.data.AppDatabase
import com.geunwoo.jun.mindfulquestion.data.Answer
import com.geunwoo.jun.mindfulquestion.data.QuestionSet
import com.geunwoo.jun.mindfulquestion.data.QuestionStep
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray

class PopupActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 팝업이 표시됨을 알림
        android.util.Log.d("PopupActivity", "팝업 시작 - 스크린타임 카운트 중지")
        AppUsageAccessibilityService.setPopupShowing(true)
        AppUsageAccessibilityService.setShouldShowPopupAgain(true)

        // 키보드가 화면을 밀어올리도록 설정
        window.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        setContent {
            MindfulQuestionTheme {
                QuestionSetFlow(
                    questionSet = QuestionSet.DEFAULT_V2,
                    onComplete = { labels, answers ->
                        saveAnswers(labels, answers)
                    }
                )
            }
        }
    }

    private fun saveAnswers(labels: List<String>, answers: List<String>) {
        lifecycleScope.launch {
            val database = AppDatabase.getDatabase(this@PopupActivity)
            val answerEntity = Answer(
                timestamp = System.currentTimeMillis(),
                questionSetVersion = "v2",
                questionLabelsJson = JSONArray(labels).toString(),
                answersJson = JSONArray(answers).toString()
            )

            database.answerDao().insert(answerEntity)
            android.util.Log.d("PopupActivity", "답변 저장 완료: $answers")

            // 팝업 완료됨을 알림
            AppUsageAccessibilityService.setPopupShowing(false)
            AppUsageAccessibilityService.setShouldShowPopupAgain(false)
            MonitoringService.getInstance()?.notifyPopupCompleted()

            finish() // 팝업 닫기
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
    onComplete: (labels: List<String>, answers: List<String>) -> Unit
) {
    var currentStepIndex by remember { mutableStateOf(0) }
    val answers = remember { mutableStateMapOf<Int, String>() }
    val labels = remember { mutableStateListOf<String>() }

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
                                onComplete(labels, answers.values.toList())
                            }
                        }
                    )
                }
                is QuestionStep.InputStep -> {
                    val answerIndex = inputSteps.indexOf(step)
                    InputStepScreen(
                        questionText = step.questionText,
                        hintText = step.hintText,
                        minLength = step.minLength,
                        initialAnswer = answers[answerIndex] ?: "",
                        stepNumber = answerIndex + 1,
                        totalSteps = inputSteps.size,
                        onNext = { answer ->
                            answers[answerIndex] = answer
                            if (answerIndex >= labels.size) {
                                labels.add(step.displayLabel)
                            }
                            currentStepIndex++
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
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        Button(
            onClick = onNext,
            enabled = isButtonEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isButtonEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (isButtonEnabled) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        ) {
            Text(
                text = if (isButtonEnabled) buttonText else "잠시 호흡해보세요",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun InputStepScreen(
    questionText: String,
    hintText: String,
    minLength: Int,
    initialAnswer: String,
    stepNumber: Int,
    totalSteps: Int,
    onNext: (String) -> Unit
) {
    var answer by remember { mutableStateOf(initialAnswer) }
    val scrollState = rememberScrollState()

    val isAnswerValid = answer.length >= minLength

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // 진행 상황
        Text(
            text = "질문 $stepNumber/$totalSteps",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )

        Text(
            text = "잠시 멈춰서 자신을 보살피세요",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 질문
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = questionText,
                    style = MaterialTheme.typography.titleMedium,
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
                    placeholder = { Text(hintText) },
                    supportingText = {
                        Text("${answer.length}자 / 최소 ${minLength}자")
                    },
                    isError = answer.isNotEmpty() && !isAnswerValid
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 안내 메시지
        if (answer.isNotEmpty() && !isAnswerValid) {
            Text(
                text = "답변은 최소 ${minLength}자 이상 작성해주세요",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 다음 버튼
        Button(
            onClick = { onNext(answer) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = isAnswerValid,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isAnswerValid) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (isAnswerValid) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        ) {
            Text(
                text = if (stepNumber == totalSteps) "제출" else "다음 질문으로",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}
