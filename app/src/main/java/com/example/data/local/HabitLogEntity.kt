package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "habit_logs",
    primaryKeys = ["habitId", "epochDay"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["epochDay"]), Index(value = ["habitId"])]
)
data class HabitLogEntity(
    val habitId: Long,
    val epochDay: Long,
    val completedParts: Int,
    val updatedAtMillis: Long = System.currentTimeMillis()
)
