package com.example.mad_cc

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medicineName = intent.getStringExtra("MED_NAME") ?: "Medicine"
        val medicineId = intent.getStringExtra("MED_ID")
        val channelId = "medicine_channel"

        Log.d("ReminderReceiver", "Alarm received for: $medicineName, ID: $medicineId")

        if (medicineId != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("medicines").document(medicineId).get()
                .addOnSuccessListener { document ->
                    val medicine = document.toObject(Medicine::class.java)
                    if (medicine != null) {
                        val calendar = Calendar.getInstance()
                        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                        Log.d("ReminderReceiver", "Today is day $dayOfWeek. Scheduled days: ${medicine.weekdays}")

                        if (medicine.weekdays.contains(dayOfWeek)) {
                            showNotification(context, medicineName, channelId)
                        }
                        
                        rescheduleAlarm(context, medicine)
                    }
                }
                .addOnFailureListener {
                    Log.e("ReminderReceiver", "Error fetching medicine: ${it.message}")
                }
        } else {
            // Fallback if ID is missing (for older alarms)
            showNotification(context, medicineName, channelId)
        }
    }

    private fun showNotification(context: Context, name: String, channelId: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Medicine Reminder", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Medicine Reminder")
            .setContentText("It is time to take your $name")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun rescheduleAlarm(context: Context, medicine: Medicine) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        intent.putExtra("MED_NAME", medicine.name)
        intent.putExtra("MED_ID", medicine.id)

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, medicine.hour)
        calendar.set(Calendar.MINUTE, medicine.minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.add(Calendar.DAY_OF_YEAR, 1)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
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
}
