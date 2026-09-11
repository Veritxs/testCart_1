package com.mindfulscreen.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLimitDao {
    @Query("SELECT * FROM app_limits ORDER BY label")
    fun observeAll(): Flow<List<AppLimitEntity>>

    @Query("SELECT * FROM app_limits")
    suspend fun getAll(): List<AppLimitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(limit: AppLimitEntity)

    @Delete
    suspend fun delete(limit: AppLimitEntity)
}

@Dao
interface DailyScoreDao {
    @Query("SELECT * FROM daily_scores ORDER BY epochDay DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<DailyScoreEntity>>

    @Query("SELECT * FROM daily_scores WHERE epochDay = :epochDay")
    suspend fun getForDay(epochDay: Long): DailyScoreEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(score: DailyScoreEntity)
}
