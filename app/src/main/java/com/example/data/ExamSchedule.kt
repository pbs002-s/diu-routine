package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exam_schedule")
data class ExamSchedule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,            // e.g. "2026-07-20"
    val dayOfWeek: String,       // Sunday, Monday, etc.
    val subjectCode: String,     // e.g. CSE 322
    val subjectName: String,     // e.g. Software Engineering
    val timeStart: String,       // "10:00" (24-hour format HH:mm)
    val timeEnd: String,         // "12:30" (24-hour format HH:mm)
    val roomNo: String,          // e.g. "604 MC"
    val seatRange: String = "",  // e.g. "Row A - Row C"
    val department: String,      // e.g. "CSE"
    val section: String,         // e.g. "A"
    val notificationEnabled: Boolean = true,
    val isCompleted: Boolean = false
)
