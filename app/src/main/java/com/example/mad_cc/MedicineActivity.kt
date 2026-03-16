package com.example.mad_cc

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class MedicineActivity : AppCompatActivity() {

    private var selectedHour = -1
    private var selectedMinute = -1
    private var editingMedicine: Medicine? = null
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var dayButtons: List<ToggleButton>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medicine)

        val titleText = findViewById<TextView>(R.id.titleText)
        val medicineName = findViewById<EditText>(R.id.medicineName)
        val timeBtn = findViewById<Button>(R.id.timeBtn)
        val saveBtn = findViewById<Button>(R.id.saveBtn)
        val deleteBtn = findViewById<Button>(R.id.deleteBtn)

        dayButtons = listOf(
            findViewById(R.id.btnSun), findViewById(R.id.btnMon),
            findViewById(R.id.btnTue), findViewById(R.id.btnWed),
            findViewById(R.id.btnThu), findViewById(R.id.btnFri),
            findViewById(R.id.btnSat)
        )

        editingMedicine = intent.getSerializableExtra("EDIT_MEDICINE") as? Medicine
        if (editingMedicine != null) {
            titleText.text = "Edit Medicine"
            medicineName.setText(editingMedicine!!.name)
            selectedHour = editingMedicine!!.hour
            selectedMinute = editingMedicine!!.minute
            timeBtn.text = String.format("Time: %02d:%02d", selectedHour, selectedMinute)
            
            for (i in 0..6) {
                dayButtons[i].isChecked = editingMedicine!!.weekdays.contains(i + 1)
            }
            
            deleteBtn.visibility = View.VISIBLE
        }

        checkNotificationPermission()

        timeBtn.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = if (selectedHour != -1) selectedHour else calendar.get(Calendar.HOUR_OF_DAY)
            val minute = if (selectedMinute != -1) selectedMinute else calendar.get(Calendar.MINUTE)

            val picker = TimePickerDialog(this, { _, h, m ->
                selectedHour = h
                selectedMinute = m
                timeBtn.text = String.format("Time: %02d:%02d", h, m)
            }, hour, minute, true)
            picker.show()
        }

        saveBtn.setOnClickListener {
            val name = medicineName.text.toString().trim()
            val selectedDays = mutableListOf<Int>()
            for (i in 0..6) {
                if (dayButtons[i].isChecked) selectedDays.add(i + 1)
            }

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter medicine name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedHour == -1) {
                Toast.makeText(this, "Please select a time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedDays.isEmpty()) {
                Toast.makeText(this, "Please select at least one day", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (checkExactAlarmPermission()) {
                saveToFirestore(name, selectedHour, selectedMinute, selectedDays)
            }
        }

        deleteBtn.setOnClickListener {
            editingMedicine?.id?.let { id ->
                db.collection("medicines").document(id).delete()
                    .addOnSuccessListener {
                        cancelAlarm(editingMedicine!!)
                        Toast.makeText(this, "Reminder Deleted", Toast.LENGTH_SHORT).show()
                        finish()
                    }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun checkExactAlarmPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                AlertDialog.Builder(this)
                    .setTitle("Permission Needed")
                    .setMessage("This app needs permission to set exact alarms for your medicine reminders. Please enable it in the next screen.")
                    .setPositiveButton("Go to Settings") { _, _ ->
                        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                return false
            }
        }
        return true
    }

    private fun saveToFirestore(name: String, hour: Int, minute: Int, days: List<Int>) {
        val userId = auth.currentUser?.uid ?: return
        val id = editingMedicine?.id ?: db.collection("medicines").document().id
        
        val medicine = Medicine(id, name, hour, minute, userId, days)

        db.collection("medicines").document(id).set(medicine)
            .addOnSuccessListener {
                setAlarm(medicine)
                Toast.makeText(this, "Reminder Saved!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setAlarm(medicine: Medicine) {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("MED_NAME", medicine.name)
            putExtra("MED_ID", medicine.id)
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, medicine.hour)
            set(Calendar.MINUTE, medicine.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            medicine.id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    private fun cancelAlarm(medicine: Medicine) {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            medicine.id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.cancel(pendingIntent)
    }
}
