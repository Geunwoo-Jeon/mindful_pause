package com.geunwoo.jun.mindfulquestion.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "answers")
data class Answer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,  // 답변 작성 시각
    val question1: String,  // "안녕하세요. 현재 당신은 무엇을 하고 있으며, 왜 그 일을 하고 계신가요?"
    val answer1: String,
    val question2: String,  // "당신은 지금부터 무슨 일을 할 것이며, 왜 그 일을 하려고 하시나요?"
    val answer2: String
)
