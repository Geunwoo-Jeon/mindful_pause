package com.geunwoo.jun.mindfulquestion.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import androidx.room.Query

@Dao
interface AnswerDao {
    @Insert
    suspend fun insert(answer: Answer)

    @Update
    suspend fun update(answer: Answer)

    @Delete
    suspend fun delete(answer: Answer)

    @Query("SELECT * FROM answers ORDER BY timestamp DESC")
    suspend fun getAllAnswers(): List<Answer>

    @Query("SELECT * FROM answers WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getAnswersByDateRange(startTime: Long, endTime: Long): List<Answer>

    @Query("SELECT COUNT(*) FROM answers")
    suspend fun getAnswerCount(): Int
}
