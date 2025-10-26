package com.geunwoo.jun.mindfulquestion.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.geunwoo.jun.mindfulquestion.R
import androidx.lifecycle.lifecycleScope
import com.geunwoo.jun.mindfulquestion.data.AppDatabase
import com.geunwoo.jun.mindfulquestion.data.Goal
import com.geunwoo.jun.mindfulquestion.ui.theme.*
import kotlinx.coroutines.launch

class GoalEditActivity : ComponentActivity() {
    companion object {
        const val EXTRA_GOAL_TYPE = "goal_type"
        const val GOAL_TYPE_FIVE_YEAR = "five_year"
        const val GOAL_TYPE_ONE_YEAR = "one_year"
        const val GOAL_TYPE_THREE_MONTH = "three_month"
    }

    private var currentGoal by mutableStateOf<Goal?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val goalType = intent.getStringExtra(EXTRA_GOAL_TYPE) ?: GOAL_TYPE_FIVE_YEAR

        // 현재 목표 로드
        lifecycleScope.launch {
            val database = AppDatabase.getDatabase(this@GoalEditActivity)
            database.goalDao().getGoal().collect { goal ->
                currentGoal = goal
            }
        }

        setContent {
            MindfulQuestionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (currentGoal != null) {
                        GoalEditScreen(
                            goalType = goalType,
                            currentGoal = currentGoal!!,
                            onSave = { newGoalText ->
                                saveGoal(goalType, newGoalText)
                            },
                            onCancel = {
                                finish()
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }

    private fun saveGoal(goalType: String, newGoalText: String) {
        val updatedGoal = when (goalType) {
            GOAL_TYPE_FIVE_YEAR -> currentGoal?.copy(fiveYearGoal = newGoalText)
            GOAL_TYPE_ONE_YEAR -> currentGoal?.copy(oneYearGoal = newGoalText)
            GOAL_TYPE_THREE_MONTH -> currentGoal?.copy(threeMonthGoal = newGoalText)
            else -> currentGoal
        }

        if (updatedGoal != null) {
            lifecycleScope.launch {
                val database = AppDatabase.getDatabase(this@GoalEditActivity)
                database.goalDao().insertOrUpdate(updatedGoal)
                finish()
            }
        }
    }
}

@Composable
fun GoalEditScreen(
    goalType: String,
    currentGoal: Goal,
    onSave: (String) -> Unit,
    onCancel: () -> Unit
) {
    val (title, initialGoalText) = when (goalType) {
        GoalEditActivity.GOAL_TYPE_FIVE_YEAR -> stringResource(R.string.edit_five_year_goal) to currentGoal.fiveYearGoal
        GoalEditActivity.GOAL_TYPE_ONE_YEAR -> stringResource(R.string.edit_one_year_goal) to currentGoal.oneYearGoal
        GoalEditActivity.GOAL_TYPE_THREE_MONTH -> stringResource(R.string.edit_three_month_goal) to currentGoal.threeMonthGoal
        else -> stringResource(R.string.edit_goal) to ""
    }

    var goalText by remember { mutableStateOf(initialGoalText) }
    val isGoalValid = goalText.isNotEmpty()
    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val topPadding = screenHeight * 0.15f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(topPadding))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = goalText,
            onValueChange = { goalText = it },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp, max = 240.dp),
            minLines = 5,
            maxLines = 8,
            placeholder = {
                Text(
                    text = stringResource(R.string.enter_goal),
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

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextPrimary
                )
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Button(
                onClick = { onSave(goalText) },
                enabled = isGoalValid,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
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
                    text = stringResource(R.string.save),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}
