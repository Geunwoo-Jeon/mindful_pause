package com.geunwoo.jun.mindfulquestion.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AnswerRecordDao {
    @Insert
    suspend fun insert(answerRecord: AnswerRecord)

    @Query("SELECT * FROM answer_records WHERE date(timestamp / 1000, 'unixepoch', 'localtime') = date(:dateInMillis / 1000, 'unixepoch', 'localtime') ORDER BY timestamp DESC")
    fun getRecordsByDate(dateInMillis: Long): Flow<List<AnswerRecord>>

    @Query("SELECT * FROM answer_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<AnswerRecord>>

    @Query("DELETE FROM answer_records WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOldRecords(beforeTimestamp: Long)
}
