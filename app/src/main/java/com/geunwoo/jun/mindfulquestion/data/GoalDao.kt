package com.geunwoo.jun.mindfulquestion.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE id = 1")
    fun getGoal(): Flow<Goal?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(goal: Goal)

    @Query("SELECT EXISTS(SELECT 1 FROM goals WHERE id = 1)")
    suspend fun hasGoal(): Boolean
}
