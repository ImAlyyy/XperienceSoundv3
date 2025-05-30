package com.example.xperiencesoundsv31

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

class AudioService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var wakeLock: PowerManager.WakeLock
    private val CHANNEL_ID = "AudioServiceChannel"
    private lateinit var audioManager: AudioManager
    private var originalVolume = 0
    private lateinit var audioFocusRequest: AudioFocusRequest

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Adquirir WakeLock
        val powerManager = getSystemService(PowerManager::class.java)
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AudioService::WakeLock")
        wakeLock.acquire(10 * 60 * 1000L)

        // Obtener instancia de AudioManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) // Guarda volumen original
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val audioName = intent?.getStringExtra("audioName") ?: return START_NOT_STICKY
        Log.d("AudioService", "Servicio iniciado con audio: $audioName")

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Reproduciendo Audio")
            .setContentText(audioName)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        startForeground(1, notification)

        // **Solicitar control del audio sin modificar el volumen del dispositivo**
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        restoreVolume()
                        stopSelf()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        // Reducir volumen de otras apps sin pausarlas
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        // Restaurar volumen original de otras apps
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
                    }
                }
            }
            .build()

        val focusResult = audioManager.requestAudioFocus(audioFocusRequest)
        if (focusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.e("AudioService", "No se obtuvo el control del audio")
            stopSelf()
            return START_NOT_STICKY
        }

        val audioResId = getAudioResource(audioName)
        mediaPlayer = MediaPlayer.create(this, audioResId)

        mediaPlayer?.start()
        mediaPlayer?.setOnCompletionListener {
            restoreVolume()
            stopSelf()
        }

        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun restoreVolume() {
        if (::audioFocusRequest.isInitialized) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
        }
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        restoreVolume()
        if (::wakeLock.isInitialized && wakeLock.isHeld) wakeLock.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Audio Service Channel", NotificationManager.IMPORTANCE_HIGH)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun getAudioResource(audioName: String): Int {
        return when (audioName) {
            "Bandilla Amarilla" -> R.raw.amarillo
            "Bandilla Aqua" -> R.raw.aqua
            "Bandilla Azul" -> R.raw.azul
            "Bandilla Blanca" -> R.raw.blanca
            "Bandilla Cafe" -> R.raw.cafe
            "Bandilla Dorada" -> R.raw.dorado
            "Bandilla Gris" -> R.raw.gris
            "Bandilla Morada" -> R.raw.morado
            "Bandilla Naranaja" -> R.raw.naranja
            "Bandilla Roja" -> R.raw.rojo
            "Bandilla Rosa" -> R.raw.rosa
            "Bandilla Verde" -> R.raw.verde
            else -> R.raw.amarillo
        }
    }
}