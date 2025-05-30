package com.example.xperiencesoundsv31

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

class AudioAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val audioName = intent?.getStringExtra("audioName") ?: return
        val originalTime = intent.getLongExtra("originalTime", -1L)

        Log.d("AudioAlarmReceiver", "Alarma activada para: $audioName")

        val serviceIntent = Intent(context, AudioService::class.java).apply {
            putExtra("audioName", audioName)
        }

        if (context.packageManager.resolveService(serviceIntent, 0) != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                Log.e("AudioAlarmReceiver", "Error al iniciar AudioService", e)
            }
        } else {
            Log.e("AudioAlarmReceiver", "AudioService no está registrado en AndroidManifest.xml")
        }

        // Reprogramar la alarma para el mismo tiempo del día siguiente
        if (originalTime != -1L) {
            reprogramAlarm(context, audioName, originalTime)
        }
    }

    private fun reprogramAlarm(context: Context, audioName: String, originalTime: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nextTimeInMillis = originalTime + AlarmManager.INTERVAL_DAY

        val intent = Intent(context, AudioAlarmReceiver::class.java).apply {
            putExtra("audioName", audioName)
            putExtra("originalTime", nextTimeInMillis)
        }

        val requestCode = audioName.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTimeInMillis, pendingIntent)
        } else {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, nextTimeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent)
        }

        Log.d("AudioAlarmReceiver", "Alarma reprogramada para: $nextTimeInMillis")
    }
}
