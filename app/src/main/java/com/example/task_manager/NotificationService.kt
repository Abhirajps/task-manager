package com.app.mytracker

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.SimpleDateFormat
import java.util.*

class NotificationService {

    companion object {
        const val CHANNEL_ID = "1435"
    }

    fun scheduleNotifications(context: Context) {
        val notificationTimes = listOf("09:00", "13:30", "17:00", "21:00")

        for (time in notificationTimes) {
            scheduleNotification(context, time)
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleNotification(context: Context, time: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val calendar = Calendar.getInstance()

        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val date = dateFormat.parse(time)

        calendar.time = date!!

        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            time.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "My tracker",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Your Notification Channel Description"
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun isAlarmScheduled(context: Context, requestCode: Int): Boolean {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return pendingIntent != null
    }

    private fun scheduleNotificationIfNeeded(context: Context) {
        val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val requestCodeKey = "notification_request_code"

        // Check if notification is already scheduled
        val requestCode = sharedPreferences.getInt(requestCodeKey, -1)
        if (requestCode == -1 || !isAlarmScheduled(context, requestCode)) {
            // Notification is not scheduled or the scheduled one is not found, so schedule it
            val newRequestCode = generateUniqueRequestCode()

            // Schedule the notification using the newRequestCode
            scheduleNotification(context, newRequestCode)

            // Save the newRequestCode to SharedPreferences
            with(sharedPreferences.edit()) {
                putInt(requestCodeKey, newRequestCode)
                apply()
            }
        } else {
            // Notification is already scheduled with the existing requestCode
            // Handle the case accordingly
        }
    }

    private fun scheduleNotification(context: Context, requestCode: Int) {
        // Schedule your notification here using the provided requestCode
        // ... (your existing notification scheduling logic)

        // For example, you can use the existing scheduleNotification function
        // notificationService.scheduleNotification(context, time)
    }

    private fun generateUniqueRequestCode(): Int {
        // You may implement your own logic to generate a unique requestCode
        // For simplicity, you can use the current timestamp as the requestCode
        return System.currentTimeMillis().toInt()
    }


}
