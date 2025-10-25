package com.geunwoo.jun.mindfulquestion.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "answer_records")
data class AnswerRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long, // 답변 시간
    val question1: String, // "지금 무엇을 하고 있나요?"
    val answer1: String,
    val question2: String, // "지금 하고 있는 일은 당신의 목표 달성에 도움이 되나요?"
    val answer2: String,
    val question3: String, // "지금부터는 무엇을 하시겠습니까?"
    val answer3: String
)
