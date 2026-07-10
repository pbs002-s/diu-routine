package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_logs")
data class StudyLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,             // Format: "YYYY-MM-DD"
    val durationMinutes: Int,     // Logged study duration
    val description: String,       // Topic e.g. "Compiler Design Revision"
    val type: String              // "STUDY" or "CLASS"
)
