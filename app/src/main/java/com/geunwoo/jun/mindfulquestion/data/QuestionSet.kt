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
     * @param minLength 최소 답변 길이
     * @param displayLabel 답변 기록에 표시될 라벨
     */
    data class InputStep(
        val questionText: String,
        val hintText: String,
        val minLength: Int,
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
                QuestionStep.MessageStep(
                    message = "지금 잠시 멈추면\n이 순간의 행복을 더 느낄 수 있습니다",
                    buttonText = "좋습니다",
                    delaySeconds = 7
                ),
                QuestionStep.InputStep(
                    questionText = "안녕하세요. 지금 무슨 감정을 느끼고 계신가요?",
                    hintText = "평온합니다",
                    minLength = 3,
                    displayLabel = "현재의 감정:"
                ),
                QuestionStep.InputStep(
                    questionText = "그렇군요! 왜 그런 감정을 느끼셨나요?",
                    hintText = "",
                    minLength = 5,
                    displayLabel = "그 이유:"
                ),
                QuestionStep.InputStep(
                    questionText = "알겠습니다. 지금 이 순간에 무엇을 하고 싶으신가요?",
                    hintText = "",
                    minLength = 5,
                    displayLabel = "지금부터 하고 싶은 일:"
                ),
                QuestionStep.InputStep(
                    questionText = "좋습니다! 왜 그 일을 하고 싶나요?",
                    hintText = "",
                    minLength = 5,
                    displayLabel = "그 이유:"
                ),
                QuestionStep.MessageStep(
                    message = "당신의 일상에 평화와 행복이 가득하길 염원합니다",
                    buttonText = "감사합니다",
                    delaySeconds = 0
                )
            )
        )
    }
}
