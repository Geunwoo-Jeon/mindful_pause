package com.geunwoo.jun.mindfulquestion.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey
    val id: Int = 1,  // 항상 단일 레코드만 유지
    val fiveYearGoal: String,  // 5년 내 목표
    val oneYearGoal: String,   // 1년 내 목표
    val threeMonthGoal: String // 3개월 내 목표
)
