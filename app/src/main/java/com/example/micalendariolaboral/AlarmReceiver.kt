package com.example.micalendariolaboral

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val mensaje = intent.getStringExtra("MENSAJE") ?: "Recordatorio de trabajo"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "canal_agenda_laboral"

        // 1. Creamos el canal de comunicación (obligatorio en Android moderno)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notificaciones Agenda",
                NotificationManager.IMPORTANCE_HIGH // Máxima importancia para que suene
            )
            channel.description = "Canal para las alarmas del calendario laboral"
            notificationManager.createNotificationChannel(channel)
        }

        // 2. Construimos la notificación con sonido y vibración por defecto
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Mi Agenda Laboral")
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Activa SONIDO y VIBRACIÓN
            .setAutoCancel(true)
            .build()

        // 3. Lanzamos la notificación
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}