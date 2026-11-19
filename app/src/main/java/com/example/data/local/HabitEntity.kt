package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val iconKey: String = "check",
    val colorKey: String = "emerald",
    val targetParts: Int = 1,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val createdAtEpochDay: Long
)
