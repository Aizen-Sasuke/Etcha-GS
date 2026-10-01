package com.example.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object ReminderManager {
    fun scheduleReminder(context: Context, timeStr: String) {
        try {
            val parts = timeStr.split(":")
            if (parts.size != 2) return
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()

            var nextTime = LocalDateTime.now().withHour(hour).withMinute(minute).withSecond(0).withNano(0)
            if (nextTime.isBefore(LocalDateTime.now())) {
                nextTime = nextTime.plusDays(1)
            }

            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("timeStr", timeStr)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val timeMills = nextTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMills, pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelReminder(context: Context) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
}
