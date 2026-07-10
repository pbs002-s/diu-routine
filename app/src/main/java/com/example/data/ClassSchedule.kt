package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "class_schedule")
data class ClassSchedule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dayOfWeek: String,       // Sunday, Monday, Tuesday, Wednesday, Thursday, Friday, Saturday
    val subjectCode: String,     // e.g. CSE 322
    val subjectName: String,     // e.g. Software Engineering
    val teacherCode: String,     // e.g. MAM
    val timeStart: String,       // "08:30" (24-hour format HH:mm)
    val timeEnd: String,         // "10:00" (24-hour format HH:mm)
    val roomNo: String,          // e.g. "604 MC"
    val department: String,      // e.g. "CSE", "SE", "EEE", "BBA"
    val section: String,         // e.g. "A", "B", "C"
    val isCompleted: Boolean = false, // Tracked daily for the progress ring
    val notificationEnabled: Boolean = true
)
