package com.geunwoo.jun.mindfulquestion.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.geunwoo.jun.mindfulquestion.ui.theme.MindfulQuestionTheme
import com.geunwoo.jun.mindfulquestion.services.AppUsageAccessibilityService
import com.geunwoo.jun.mindfulquestion.services.MonitoringService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
                var currentStep by remember { mutableStateOf(1) }
                var answer1 by remember { mutableStateOf("") }
                var answer2 by remember { mutableStateOf("") }

                when (currentStep) {
                    1 -> QuestionScreen(
                        questionNumber = 1,
                        questionText = "안녕하세요. 현재 당신은 무엇을 하고 있으며, 왜 그 일을 하고 계신가요?",
                        initialAnswer = answer1,
                        onNext = { answer ->
                            answer1 = answer
                            currentStep = 2
                        }
                    )
                    2 -> QuestionScreen(
                        questionNumber = 2,
                        questionText = "당신은 지금부터 무슨 일을 할 것이며, 왜 그 일을 하려고 하시나요?",
                        initialAnswer = answer2,
                        onNext = { answer ->
                            answer2 = answer
                            // TODO: Phase 5에서 데이터베이스에 저장
                            android.util.Log.d("PopupActivity", "답변1: $answer1")
                            android.util.Log.d("PopupActivity", "답변2: $answer2")

                            // 팝업 완료됨을 알림
                            AppUsageAccessibilityService.setPopupShowing(false)
                            AppUsageAccessibilityService.setShouldShowPopupAgain(false)
                            MonitoringService.getInstance()?.notifyPopupCompleted()

                            finish() // 팝업 닫기
                        }
                    )
                }
            }
        }
    }

    // 뒤로가기 버튼 무효화
    override fun onBackPressed() {
        // 아무것도 하지 않음 - 뒤로가기 버튼 무시
    }
}

@Composable
fun QuestionScreen(
    questionNumber: Int,
    questionText: String,
    initialAnswer: String,
    onNext: (String) -> Unit
) {
    var answer by remember { mutableStateOf(initialAnswer) }
    var countdown by remember { mutableStateOf(10) }
    var isButtonEnabled by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // 답변이 최소 15자 이상인지 확인
    val isAnswerValid = answer.length >= 15
    val canSubmit = isButtonEnabled && isAnswerValid

    // 카운트다운 타이머
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            for (i in 10 downTo 0) {
                countdown = i
                if (i == 0) {
                    isButtonEnabled = true
                }
                delay(1000)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding() // 키보드 패딩 추가
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // 제목
            Text(
                text = "마음챙김 질문 ${questionNumber}/2",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )

            Text(
                text = "잠시 멈춰서 자신에게 질문해보세요",
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
                            .heightIn(min = 200.dp),
                        minLines = 5,
                        maxLines = 10,
                        placeholder = { Text("답변을 입력하세요...") },
                        supportingText = {
                            Text("${answer.length} / 최소 15자")
                        },
                        isError = answer.isNotEmpty() && !isAnswerValid
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 카운트다운 표시
            if (!isButtonEnabled) {
                Text(
                    text = "${countdown}초 후 다음 단계로",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (!canSubmit) {
                Text(
                    text = "답변은 최소 15자 이상 작성해주세요",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 다음 버튼
            Button(
                onClick = { onNext(answer) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = canSubmit
            ) {
                Text(
                    text = when {
                        !isButtonEnabled -> "잠시만 기다려주세요... (${countdown}초)"
                        !canSubmit -> "답변을 작성해주세요 (최소 15자)"
                        questionNumber == 1 -> "다음 질문으로"
                        else -> "제출"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
