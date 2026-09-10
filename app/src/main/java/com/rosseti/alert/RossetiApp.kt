package com.rosseti.alert

import android.app.Application
import com.rosseti.alert.notification.NotificationHelper
import com.rosseti.alert.worker.CheckWorker

/**
 * Application — инициализация канала уведомлений и планирование проверки.
 */
class RossetiApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Канал уведомлений (создаётся один раз)
        NotificationHelper.createNotificationChannel(this)

        // Запланировать ежедневную проверку
        // existingPeriodicWork = KEEP — не перезапускает, если уже есть
        CheckWorker.schedule(this)
    }
}