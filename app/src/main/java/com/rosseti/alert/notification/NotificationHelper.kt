package com.rosseti.alert.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.rosseti.alert.MainActivity
import com.rosseti.alert.data.Outage

/**
 * Помощник для создания и отправки локальных уведомлений.
 * Канал создаётся один раз в Application.onCreate().
 */
class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "rosseti_outage_channel"
        const val CHANNEL_NAME = "Отключения электроэнергии"
        const val NOTIFICATION_ID = 1001

        fun createNotificationChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH    // Звук + всплывающее
            ).apply {
                description = "Уведомления о плановых отключениях электроэнергии"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    /**
     * Показать уведомление о ближайшем отключении.
     * На Android 13+ проверяет разрешение POST_NOTIFICATIONS.
     */
    fun showOutageNotification(outage: Outage) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return  // Разрешение не выдано — тихо пропускаем
            }
        }

        // Tap — открыть приложение
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "⚠️ Плановое отключение"
        val text = buildString {
            append("${outage.dateStart.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM"))} ")
            append("с ${outage.timeStart} до ${outage.timeFinish}\n")
            append(outage.gorod)
            if (outage.street.isNotBlank()) append(", ${outage.street}")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }
}