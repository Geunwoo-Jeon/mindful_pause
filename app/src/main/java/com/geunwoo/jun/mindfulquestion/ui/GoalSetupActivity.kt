package com.geunwoo.jun.mindfulquestion.ui

import android.content.Intent
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.geunwoo.jun.mindfulquestion.R
import com.geunwoo.jun.mindfulquestion.MainActivity
import com.geunwoo.jun.mindfulquestion.ui.theme.*
import com.geunwoo.jun.mindfulquestion.data.AppDatabase
import com.geunwoo.jun.mindfulquestion.data.Goal
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class GoalSetupActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 키보드가 화면을 밀어올리도록 설정
        window.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        setContent {
            MindfulQuestionTheme {
                GoalSetupFlow(
                    onComplete = { fiveYear, oneYear, threeMonth ->
                        saveGoals(fiveYear, oneYear, threeMonth)
                    }
                )
            }
        }
    }

    private fun saveGoals(fiveYear: String, oneYear: String, threeMonth: String) {
        lifecycleScope.launch {
            val database = AppDatabase.getDatabase(this@GoalSetupActivity)
            val goal = Goal(
                fiveYearGoal = fiveYear,
                oneYearGoal = oneYear,
                threeMonthGoal = threeMonth
            )

            database.goalDao().insertOrUpdate(goal)
            android.util.Log.d("GoalSetupActivity", "목표 저장 완료")

            // MainActivity로 이동
            startActivity(Intent(this@GoalSetupActivity, MainActivity::class.java))
            finish()
        }
    }

    // 뒤로가기 버튼 무효화
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // 아무것도 하지 않음 - 뒤로가기 버튼 무시
    }
}

@Composable
fun GoalSetupFlow(
    onComplete: (fiveYear: String, oneYear: String, threeMonth: String) -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    var fiveYearGoal by remember { mutableStateOf("") }
    var oneYearGoal by remember { mutableStateOf("") }
    var threeMonthGoal by remember { mutableStateOf("") }

    val questions = listOf(
        stringResource(R.string.five_year_goal_question),
        stringResource(R.string.one_year_goal_question),
        stringResource(R.string.three_month_goal_question)
    )

    val hints = listOf(
        stringResource(R.string.five_year_goal_hint),
        stringResource(R.string.one_year_goal_hint),
        stringResource(R.string.three_month_goal_hint)
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith
                        fadeOut(animationSpec = tween(300))
            },
            label = "goal_setup_transition"
        ) { step ->
            when (step) {
                0 -> GoalInputScreen(
                    questionText = questions[0],
                    hintText = hints[0],
                    initialGoal = fiveYearGoal,
                    stepNumber = 1,
                    totalSteps = 3,
                    onNext = { goal ->
                        fiveYearGoal = goal
                        currentStep++
                    }
                )
                1 -> GoalInputScreen(
                    questionText = questions[1],
                    hintText = hints[1],
                    initialGoal = oneYearGoal,
                    stepNumber = 2,
                    totalSteps = 3,
                    onNext = { goal ->
                        oneYearGoal = goal
                        currentStep++
                    }
                )
                2 -> GoalInputScreen(
                    questionText = questions[2],
                    hintText = hints[2],
                    initialGoal = threeMonthGoal,
                    stepNumber = 3,
                    totalSteps = 3,
                    onNext = { goal ->
                        threeMonthGoal = goal
                        onComplete(fiveYearGoal, oneYearGoal, threeMonthGoal)
                    }
                )
            }
        }
    }
}

@Composable
fun GoalInputScreen(
    questionText: String,
    hintText: String,
    initialGoal: String,
    stepNumber: Int,
    totalSteps: Int,
    onNext: (String) -> Unit
) {
    var goal by remember { mutableStateOf(initialGoal) }
    val scrollState = rememberScrollState()

    val isGoalValid = goal.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // 진행 상황
        Text(
            text = stringResource(R.string.goal_setup_progress, stepNumber, totalSteps),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(R.string.set_your_goal),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

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
                    value = goal,
                    onValueChange = { goal = it },
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
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.goal_tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 다음 버튼
        Button(
            onClick = { onNext(goal) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = isGoalValid,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isGoalValid) {
                    GreenPrimary
                } else {
                    CardBackgroundTint
                },
                contentColor = if (isGoalValid) {
                    CardBackground
                } else {
                    TextTertiary
                }
            )
        ) {
            Text(
                text = if (stepNumber == totalSteps) stringResource(R.string.complete) else stringResource(R.string.next),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}
