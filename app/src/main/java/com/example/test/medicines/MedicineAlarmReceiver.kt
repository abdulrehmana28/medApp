package com.example.test.medicines

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.test.MainActivity
import com.example.test.R

class MedicineAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicineId = intent.getStringExtra("MEDICINE_ID") ?: ""
        val medicineName = intent.getStringExtra("MEDICINE_NAME") ?: "Medicine"
        val dosage = intent.getStringExtra("MEDICINE_DOSAGE") ?: ""
        val time = intent.getStringExtra("MEDICINE_TIME") ?: "09:00"
        val dayOfWeek = intent.getIntExtra("DAY_OF_WEEK", -1)

        // 1. Show the Notification
        showNotification(context, medicineName, dosage)

        // 2. Reschedule for next week
        // Since the alarm just went off, scheduling it again with the same logic
        // will automatically push it to next week (because "now" > "alarm time").
        if (medicineId.isNotEmpty() && dayOfWeek != -1) {
            val medicine = Medicine(id = medicineId, name = medicineName, dosage = dosage, time = time)
            AlarmScheduler.scheduleNextAlarm(context, medicine, dayOfWeek)
        }
    }

    private fun showNotification(context: Context, name: String, dosage: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "med_alarm_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Medicine Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Reminders to take your medicine"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, channelId)
            // IMPORTANT: Use a vector icon here (ic_launcher_foreground), NOT a mipmap/png
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Take $name")
            .setContentText("Dosage: $dosage")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(name.hashCode(), notification)
    }
}