package com.example.test.medicines

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

object AlarmScheduler {

    fun scheduleAlarmsForMedicine(context: Context, medicine: Medicine) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val (hour, minute) = parseTime(medicine.time) ?: return

        // If no specific days selected, assume daily
        val daysToSchedule = if (medicine.selectedDays.isEmpty()) {
            listOf(1, 2, 3, 4, 5, 6, 7) // Sun to Sat
        } else {
            medicine.selectedDays
        }

        for (dayOfWeek in daysToSchedule) {
            scheduleRepeatingAlarm(context, alarmManager, medicine, dayOfWeek, hour, minute)
        }
    }

    private fun scheduleRepeatingAlarm(
        context: Context,
        alarmManager: AlarmManager,
        medicine: Medicine,
        dayOfWeek: Int,
        hour: Int,
        minute: Int
    ) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.DAY_OF_WEEK, dayOfWeek)
        }

        // If the time has already passed for this week, schedule for next week
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 7)
        }

        val intent = Intent(context, MedicineAlarmReceiver::class.java).apply {
            putExtra("MEDICINE_NAME", medicine.name)
            putExtra("MEDICINE_DOSAGE", medicine.dosage)
        }

        // Unique ID for this specific medicine on this specific day
        val requestCode = (medicine.id + dayOfWeek).hashCode()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // Set repeating alarm every 7 days
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY * 7,
                pendingIntent
            )
            Log.d("AlarmScheduler", "Scheduled ${medicine.name} for day $dayOfWeek at $hour:$minute")
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "Permission missing for exact alarm", e)
        }
    }

    // Cancel all alarms for a medicine (e.g., when deleted or updated)
    fun cancelAlarms(context: Context, medicine: Medicine) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val daysToCheck = if (medicine.selectedDays.isEmpty()) (1..7).toList() else medicine.selectedDays

        for (day in daysToCheck) {
            val intent = Intent(context, MedicineAlarmReceiver::class.java)
            val requestCode = (medicine.id + day).hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun parseTime(timeStr: String): Pair<Int, Int>? {
        return try {
            val parts = timeStr.split(":")
            Pair(parts[0].toInt(), parts[1].toInt())
        } catch (e: Exception) {
            null
        }
    }
}