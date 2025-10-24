package com.geunwoo.jun.mindfulquestion.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "answers")
data class Answer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,  // 답변 작성 시각
    val questionSetVersion: String,  // "v2", "v3", ...
    val questionLabelsJson: String,  // JSON: ["현재의 감정:", "그 이유:", ...]
    val answersJson: String  // JSON: ["평온합니다", "모든 일이 잘 되고 있습니다", ...]
)
