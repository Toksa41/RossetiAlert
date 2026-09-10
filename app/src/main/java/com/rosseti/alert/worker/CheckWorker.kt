package com.rosseti.alert.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rosseti.alert.data.OutageFilter
import com.rosseti.alert.data.OutageRepository
import com.rosseti.alert.data.OutageResult
import com.rosseti.alert.data.SettingsStore
import com.rosseti.alert.notification.NotificationHelper
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * WorkManager-воркер — выполняет проверку раз в сутки.
 *
 * PeriodicWorkRequest (24h) с initialDelay для срабатывания утром.
 * Constraints: требуется интернет (не метрика).
 *
 * @see scheduleDailyCheck перепланировка после загрузки устройства/установки
 */
class CheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "daily_outage_check"
        private const val TAG = "CheckWorker"

        /**
         * Запланировать ежедневную проверку.
         * Вызывается при первом запуске и после перезагрузки.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Период — 24 часа, начальная задержка — до 8:00 утра
            // Для точности до минуты нужен AlarmManager, но с WorkManager батарея не страдает
            val request = PeriodicWorkRequestBuilder<CheckWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.i(TAG, "Ежедневная проверка запланирована")
        }
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "Запуск проверки отключений")

        // 1. Загружаем настройки
        val settingsStore = SettingsStore(applicationContext)
        val filter = settingsStore.filterFlow.first()

        if (!filter.isComplete()) {
            Log.w(TAG, "Настройки не заполнены — пропускаем")
            return Result.success()
        }

        // 2. Ищем ближайшее отключение
        val repository = OutageRepository()
        return when (val result = repository.findNextOutage(filter)) {
            is OutageResult.Success -> {
                if (result.next != null) {
                    Log.i(TAG, "Найдено отключение: ${result.next.dateStart} ${result.next.gorod}")
                    // 3. Отправляем уведомление
                    NotificationHelper(applicationContext).showOutageNotification(result.next)
                } else {
                    Log.i(TAG, "Отключений по адресу не найдено")
                }
                Result.success()
            }
            is OutageResult.Error -> {
                Log.e(TAG, "Ошибка: ${result.message}")
                if (result.message.contains("нет интернет", ignoreCase = true)) {
                    Result.retry()   // Повторить при следующем окне WorkManager
                } else {
                    Result.success() // Серверная ошибка — не перезапускать, подождём до завтра
                }
            }
        }
    }
}