package com.example.test.medicines

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.Calendar

object AlarmScheduler {

    // Helper to parse "08:00" string into hour/minute
    private fun parseTime(timeStr: String): Pair<Int, Int>? {
        return try {
            val parts = timeStr.split(":")
            Pair(parts[0].toInt(), parts[1].toInt())
        } catch (e: Exception) {
            null
        }
    }

    fun scheduleAlarmsForMedicine(context: Context, medicine: Medicine) {
        val daysToSchedule = if (medicine.selectedDays.isEmpty()) {
            listOf(1, 2, 3, 4, 5, 6, 7) // Sunday to Saturday
        } else {
            medicine.selectedDays
        }

        for (dayOfWeek in daysToSchedule) {
            scheduleNextAlarm(context, medicine, dayOfWeek)
        }
    }

    // Schedules a SINGLE exact alarm for the next occurrence of this day/time
    fun scheduleNextAlarm(context: Context, medicine: Medicine, dayOfWeek: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val (hour, minute) = parseTime(medicine.time) ?: return

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_WEEK, dayOfWeek)
        }

        // If this time has already passed this week, schedule for next week
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
        }

        val intent = Intent(context, MedicineAlarmReceiver::class.java).apply {
            // Pass data needed to show notification AND reschedule
            putExtra("MEDICINE_ID", medicine.id)
            putExtra("MEDICINE_NAME", medicine.name)
            putExtra("MEDICINE_DOSAGE", medicine.dosage)
            putExtra("MEDICINE_TIME", medicine.time)
            putExtra("DAY_OF_WEEK", dayOfWeek)
        }

        // Unique RequestCode: ID hash + day makes it unique per medicine per day
        val requestCode = (medicine.id.hashCode() + dayOfWeek)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // Use setExactAndAllowWhileIdle for reliability in Doze mode
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Log.d("AlarmScheduler", "Scheduled ${medicine.name} for ${calendar.time}")
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "Permission missing for exact alarm", e)
        }
    }

    fun cancelAlarms(context: Context, medicine: Medicine) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val daysToCheck = if (medicine.selectedDays.isEmpty()) (1..7).toList() else medicine.selectedDays

        for (day in daysToCheck) {
            val intent = Intent(context, MedicineAlarmReceiver::class.java)
            val requestCode = (medicine.id.hashCode() + day)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}