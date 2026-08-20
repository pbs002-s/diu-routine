package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val classId = intent.getIntExtra("class_id", 0)
        val subjectCode = intent.getStringExtra("subject_code") ?: "Class"
        val subjectName = intent.getStringExtra("subject_name") ?: ""
        val roomNo = intent.getStringExtra("room_no") ?: "TBA"
        val timeStart = intent.getStringExtra("time_start") ?: ""
        val isExam = intent.getBooleanExtra("is_exam", false)
        val examDate = intent.getStringExtra("exam_date") ?: ""

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = if (isExam) "exam_reminders" else "class_reminders"
        val channelName = if (isExam) "DIU Exam Reminders" else "DIU Class Reminders"

        // Create the notification channel if on API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = if (isExam) "Reminders for scheduled exams" else "Reminders sent 20 minutes before each class starts"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Open app on clicking the notification
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            classId,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val titleText = if (isExam) "Upcoming Exam: $subjectCode" else "Upcoming Class: $subjectCode"
        val notificationText = if (isExam) {
            "$subjectName in Room $roomNo starts at $timeStart on $examDate"
        } else if (roomNo.isNotEmpty()) {
            "$subjectName in Room $roomNo starts at $timeStart"
        } else {
            "$subjectName starts at $timeStart"
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Safe fallback icon
            .setContentTitle(titleText)
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(classId, notification)
    }
}
