package com.geunwoo.jun.mindfulquestion.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "screen_time")
data class ScreenTime(
    @PrimaryKey
    val date: String,  // "yyyy-MM-dd" 형식
    val totalMinutes: Int  // 오늘 하루 총 스크린타임 (분)
)
