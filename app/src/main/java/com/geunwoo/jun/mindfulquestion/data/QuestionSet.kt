package com.geunwoo.jun.mindfulquestion.data

/**
 * 질문 세트의 각 단계를 나타내는 sealed class
 */
sealed class QuestionStep {
    /**
     * 멘트만 표시하는 단계
     * @param message 표시할 멘트
     * @param buttonText 버튼 텍스트
     * @param delaySeconds 버튼 활성화까지 대기 시간 (초)
     */
    data class MessageStep(
        val message: String,
        val buttonText: String,
        val delaySeconds: Int
    ) : QuestionStep()

    /**
     * 질문 + 답변 입력 단계
     * @param questionText 질문 텍스트
     * @param hintText 입력 필드 힌트
     * @param displayLabel 답변 기록에 표시될 라벨
     */
    data class InputStep(
        val questionText: String,
        val hintText: String,
        val displayLabel: String
    ) : QuestionStep()
}

/**
 * 전체 질문 세트
 */
data class QuestionSet(
    val version: String,
    val steps: List<QuestionStep>
) {
    companion object {
        /**
         * v2 기본 질문 세트
         */
        val DEFAULT_V2 = QuestionSet(
            version = "v2",
            steps = listOf(
                QuestionStep.InputStep(
                    questionText = "지금 무엇을 하고 있나요?",
                    hintText = "예: 유튜브 보기, SNS 보기 등",
                    displayLabel = "하고 있던 일:"
                ),
                QuestionStep.InputStep(
                    questionText = "지금 하고 있는 일은 당신의 목표 달성에 도움이 되나요?",
                    hintText = "",
                    displayLabel = "목표와의 연관성:"
                ),
                QuestionStep.InputStep(
                    questionText = "지금부터는 무엇을 하시겠습니까?",
                    hintText = "",
                    displayLabel = "다음 행동:"
                )
            )
        )
    }
}
