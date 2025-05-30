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

        // **Reprogramar la alarma para ejecutarse al día siguiente**
        reprogramAlarm(context, audioName)
    }

    private fun reprogramAlarm(context: Context, audioName: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AudioAlarmReceiver::class.java).apply {
            putExtra("audioName", audioName) // Mantiene el mismo audio programado
        }

        // **Corregir PendingIntent con FLAG_IMMUTABLE**
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Configurar la próxima alarma para el día siguiente a la misma hora
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1) // Reprogramar para el día siguiente
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        } else {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent)
        }

        Log.d("AudioAlarmReceiver", "Alarma reprogramada para el día siguiente")
    }
}