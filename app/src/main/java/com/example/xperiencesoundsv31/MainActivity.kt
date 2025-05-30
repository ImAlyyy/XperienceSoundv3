package com.example.xperiencesoundsv31

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var timePicker: TimePicker
    private lateinit var spinnerAudios: Spinner
    private lateinit var btnSetAlarm: Button
    private lateinit var listViewAlarms: ListView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var alarmsAdapter: ArrayAdapter<String>
    private val alarmList = mutableListOf<String>()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        timePicker = findViewById(R.id.timePicker)
        spinnerAudios = findViewById(R.id.spinnerAudios)
        btnSetAlarm = findViewById(R.id.btnSetAlarm)
        listViewAlarms = findViewById(R.id.listViewAlarms)
        sharedPreferences = getSharedPreferences("AlarmPrefs", MODE_PRIVATE)

        val audioList = listOf(
            "Bandilla Amarilla", "Bandilla Aqua", "Bandilla Azul", "Bandilla Blanca",
            "Bandilla Cafe", "Bandilla Dorada", "Bandilla Gris", "Bandilla Morada",
            "Bandilla Naranja", "Bandilla Roja", "Bandilla Rosa", "Bandilla Verde"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, audioList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAudios.adapter = adapter

        alarmsAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, alarmList)
        listViewAlarms.adapter = alarmsAdapter

        loadSavedAlarms()

        // Verificar y solicitar permisos al inicio
        checkAndRequestPermissions()

        btnSetAlarm.setOnClickListener {
            val hour = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) timePicker.hour else timePicker.currentHour
            val minute = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) timePicker.minute else timePicker.currentMinute

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
            }

            val selectedAudio = spinnerAudios.selectedItem as String
            val timeString = String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
            val alarmEntry = "$timeString - $selectedAudio"

            if (!checkExactAlarmPermission()) {
                requestExactAlarmPermission()
                return@setOnClickListener
            }

            setRepeatingAlarm(calendar.timeInMillis, selectedAudio)

            alarmList.add(alarmEntry)
            alarmsAdapter.notifyDataSetChanged()

            val savedAlarms = sharedPreferences.getStringSet("alarmList", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            savedAlarms.add(alarmEntry)
            sharedPreferences.edit().putStringSet("alarmList", savedAlarms).apply()

            Toast.makeText(this, "Audio programado a las $timeString (se repetirá diariamente)", Toast.LENGTH_SHORT).show()
        }

        listViewAlarms.setOnItemClickListener { _, _, position, _ ->
            val selectedAlarm = alarmList[position]
            AlertDialog.Builder(this)
                .setTitle("Eliminar Audio")
                .setMessage("¿Eliminar este audio programado?\n\n$selectedAlarm")
                .setPositiveButton("Sí") { _, _ ->
                    alarmList.removeAt(position)
                    alarmsAdapter.notifyDataSetChanged()

                    val savedAlarms = sharedPreferences.getStringSet("alarmList", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                    savedAlarms.remove(selectedAlarm)
                    sharedPreferences.edit().putStringSet("alarmList", savedAlarms).apply()

                    Toast.makeText(this, "Audio eliminado", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WAKE_LOCK) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.WAKE_LOCK)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 100)
        }

        // Verificar permiso de alarmas exactas
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }
    }

    private fun checkExactAlarmPermission(): Boolean {
        val alarmManager = getSystemService(AlarmManager::class.java)
        return android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    }

    private fun requestExactAlarmPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            startActivity(intent)
        }
    }

    private fun setRepeatingAlarm(timeInMillis: Long, audioName: String) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AudioAlarmReceiver::class.java).apply {
            putExtra("audioName", audioName)
            putExtra("originalTime", timeInMillis) // ✅ clave correcta para repetir
        }
        val requestCode = (timeInMillis.toString() + audioName).hashCode()
        val pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
    }


    private fun loadSavedAlarms() {
        val savedAlarms = sharedPreferences.getStringSet("alarmList", mutableSetOf())
        alarmList.clear()
        alarmList.addAll(savedAlarms ?: emptySet())
        alarmsAdapter.notifyDataSetChanged()
    }
}