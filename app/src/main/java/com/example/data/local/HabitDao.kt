package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY sortOrder ASC, id ASC")
    fun observeActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE isArchived = 1 ORDER BY sortOrder ASC, id ASC")
    fun observeArchivedHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY sortOrder ASC, id ASC")
    fun observeAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habit_logs")
    fun observeAllLogs(): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getHabitById(id: Long): HabitEntity?

    @Query("SELECT * FROM habits ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllHabitsOnce(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY sortOrder ASC, id ASC")
    suspend fun getActiveHabitsOnce(): List<HabitEntity>

    @Query("SELECT * FROM habit_logs")
    suspend fun getAllLogsOnce(): List<HabitLogEntity>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND epochDay = :epochDay LIMIT 1")
    suspend fun getLogForDay(habitId: Long, epochDay: Long): HabitLogEntity?

    @Query("SELECT * FROM habit_logs WHERE epochDay = :epochDay")
    suspend fun getLogsForDayOnce(epochDay: Long): List<HabitLogEntity>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM habits WHERE isArchived = 0")
    suspend fun getMaxSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("UPDATE habits SET sortOrder = :sortOrder WHERE id = :habitId")
    suspend fun updateSortOrder(habitId: Long, sortOrder: Int)

    @Query("UPDATE habits SET isArchived = :archived WHERE id = :habitId")
    suspend fun setArchived(habitId: Long, archived: Boolean)

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabitById(habitId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLog(log: HabitLogEntity)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND epochDay = :epochDay")
    suspend fun deleteLog(habitId: Long, epochDay: Long)

    @Query("DELETE FROM habit_logs")
    suspend fun deleteAllLogs()

    @Query("DELETE FROM habits")
    suspend fun deleteAllHabits()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabits(habits: List<HabitEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<HabitLogEntity>)

    @Transaction
    suspend fun updateSortOrders(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id ->
            updateSortOrder(id, index)
        }
    }

    @Transaction
    suspend fun replaceAllData(habits: List<HabitEntity>, logs: List<HabitLogEntity>) {
        deleteAllLogs()
        deleteAllHabits()
        if (habits.isNotEmpty()) {
            insertHabits(habits)
        }
        if (logs.isNotEmpty()) {
            insertLogs(logs)
        }
    }
}
