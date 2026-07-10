package com.example.notification

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.ClassSchedule
import com.example.data.ExamSchedule
import java.util.Calendar

object RoutineAlarmScheduler {

    private const val TAG = "RoutineAlarmScheduler"

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleAlarmForClass(context: Context, classSchedule: ClassSchedule) {
        if (!classSchedule.notificationEnabled) {
            cancelAlarmForClass(context, classSchedule)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        
        // Parse start time (e.g., "08:30" -> hour=8, minute=30)
        val timeParts = classSchedule.timeStart.split(":")
        if (timeParts.size != 2) return
        val startHour = timeParts[0].toIntOrNull() ?: return
        val startMinute = timeParts[1].toIntOrNull() ?: return

        // Subtract customizable minutes for reminder
        val sharedPrefs = context.getSharedPreferences("diu_routine_settings", Context.MODE_PRIVATE)
        val alarmOffsetMinutes = sharedPrefs.getInt("alarm_offset_minutes", 20)

        var alarmHour = startHour
        var alarmMinute = startMinute - alarmOffsetMinutes
        if (alarmMinute < 0) {
            alarmMinute += 60
            alarmHour -= 1
            if (alarmHour < 0) {
                alarmHour += 24
            }
        }

        val dayOfWeekInt = getDayOfWeekConstant(classSchedule.dayOfWeek) ?: return

        // Set up Calendar
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarmHour)
            set(Calendar.MINUTE, alarmMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Align calendar to the target day of the week
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        var daysDiff = dayOfWeekInt - currentDayOfWeek
        if (daysDiff < 0 || (daysDiff == 0 && calendar.timeInMillis < System.currentTimeMillis())) {
            daysDiff += 7 // If in past or today past alarm time, set to next week's day
        }
        calendar.add(Calendar.DAY_OF_YEAR, daysDiff)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("class_id", classSchedule.id)
            putExtra("subject_code", classSchedule.subjectCode)
            putExtra("subject_name", classSchedule.subjectName)
            putExtra("room_no", classSchedule.roomNo)
            putExtra("time_start", classSchedule.timeStart)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            classSchedule.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // Schedule repeating alarm weekly (every 7 days)
            val interval = AlarmManager.INTERVAL_DAY * 7
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled exact alarm for ${classSchedule.subjectCode} at ${calendar.time}")
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled standard alarm (exact permission missing) for ${classSchedule.subjectCode}")
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled exact alarm for ${classSchedule.subjectCode}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm", e)
        }
    }

    fun cancelAlarmForClass(context: Context, classSchedule: ClassSchedule) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            classSchedule.id,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled alarm for class ID: ${classSchedule.id}")
        }
    }

    fun rescheduleAllAlarms(context: Context, classes: List<ClassSchedule>) {
        classes.forEach { classSchedule ->
            if (classSchedule.id > 0) { // Only schedule if actually saved with valid ID
                scheduleAlarmForClass(context, classSchedule)
            }
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleAlarmForExam(context: Context, examSchedule: ExamSchedule) {
        if (!examSchedule.notificationEnabled || examSchedule.id <= 0) {
            cancelAlarmForExam(context, examSchedule)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        
        // Parse exam date (e.g., "2026-07-20") and start time (e.g., "10:00")
        val dateParts = examSchedule.date.split("-")
        val timeParts = examSchedule.timeStart.split(":")
        if (dateParts.size != 3 || timeParts.size != 2) return

        val year = dateParts[0].toIntOrNull() ?: return
        val month = dateParts[1].toIntOrNull() ?: return
        val day = dateParts[2].toIntOrNull() ?: return
        val hour = timeParts[0].toIntOrNull() ?: return
        val minute = timeParts[1].toIntOrNull() ?: return

        // Set up Calendar
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Subtract global reminder minutes (default is 60 mins for exams, or use customized setting if preferred)
        val sharedPrefs = context.getSharedPreferences("diu_routine_settings", Context.MODE_PRIVATE)
        val alarmOffsetMinutes = sharedPrefs.getInt("alarm_offset_minutes", 20)
        
        // For exams, we can give a 60 min reminder or use the class alarmOffsetMinutes
        val examOffset = if (alarmOffsetMinutes == 20) 60 else alarmOffsetMinutes
        calendar.add(Calendar.MINUTE, -examOffset)

        // Only schedule if in future
        if (calendar.timeInMillis < System.currentTimeMillis()) {
            Log.d(TAG, "Exam is in the past: ${examSchedule.subjectCode}")
            return
        }

        val examAlarmId = examSchedule.id + 100000 // offset to avoid collision with class schedule IDs

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("class_id", examAlarmId)
            putExtra("subject_code", examSchedule.subjectCode)
            putExtra("subject_name", examSchedule.subjectName)
            putExtra("room_no", examSchedule.roomNo)
            putExtra("time_start", examSchedule.timeStart)
            putExtra("is_exam", true)
            putExtra("exam_date", examSchedule.date)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            examAlarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled exact exam alarm for ${examSchedule.subjectCode} at ${calendar.time}")
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled standard exam alarm (exact permission missing)")
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled exact exam alarm for ${examSchedule.subjectCode}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule exam alarm", e)
        }
    }

    fun cancelAlarmForExam(context: Context, examSchedule: ExamSchedule) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val examAlarmId = examSchedule.id + 100000
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            examAlarmId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled alarm for exam ID: ${examSchedule.id}")
        }
    }

    fun rescheduleAllExams(context: Context, exams: List<ExamSchedule>) {
        exams.forEach { exam ->
            if (exam.id > 0) {
                scheduleAlarmForExam(context, exam)
            }
        }
    }

    private fun getDayOfWeekConstant(dayName: String): Int? {
        return when (dayName.lowercase().trim()) {
            "sunday" -> Calendar.SUNDAY
            "monday" -> Calendar.MONDAY
            "tuesday" -> Calendar.TUESDAY
            "wednesday" -> Calendar.WEDNESDAY
            "thursday" -> Calendar.THURSDAY
            "friday" -> Calendar.FRIDAY
            "saturday" -> Calendar.SATURDAY
            else -> null
        }
    }
}
