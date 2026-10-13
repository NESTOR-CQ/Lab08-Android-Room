package com.example.lab08

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = false,
    @ColumnInfo(name = "priority") val priority: String = "Media", // Alta, Media, Baja
    @ColumnInfo(name = "category") val category: String = "Personal", // Personal, Trabajo, Estudio, Hogar, Otro
    @ColumnInfo(name = "recurrence") val recurrence: String = "Ninguna", // Ninguna, Diaria, Semanal, Mensual
    @ColumnInfo(name = "has_reminder") val hasReminder: Boolean = false,
    @ColumnInfo(name = "reminder_time") val reminderTime: String = "" // ej. "09:00 AM"
)
