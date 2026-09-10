package com.rosseti.alert.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Репозиторий — единственный источник данных для ViewModel.
 * Загружает все записи, фильтрует по адресу и находит ближайшее отключение.
 */
class OutageRepository(
    private val api: RossetiApi = RossetiApi()
) {

    /** Загрузить список регионов */
    suspend fun getRegions(): List<Region> = withContext(Dispatchers.IO) {
        api.fetchRegions()
    }

    /**
     * Получить уникальные районы для выбранного региона из ВСЕХ записей.
     * Используется для построения выпадающего списка.
     */
    suspend fun getRaionsForRegion(regionCode: String): List<String> =
        withContext(Dispatchers.IO) {
            val all = api.fetchAllOutages()
            all.filter { it.region == regionCode }
                .map { it.raion }
                .distinct()
                .sorted()
        }

    /**
     * Получить населённые пункты для региона + района.
     */
    suspend fun getGorodsFor(regionCode: String, raion: String): List<String> =
        withContext(Dispatchers.IO) {
            val all = api.fetchAllOutages()
            all.filter { it.region == regionCode && it.raion.contains(raion, ignoreCase = true) }
                .map { it.gorod }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        }

    /**
     * Получить улицы для региона + района + нас. пункта.
     */
    suspend fun getStreetsFor(regionCode: String, raion: String, gorod: String): List<String> =
        withContext(Dispatchers.IO) {
            val all = api.fetchAllOutages()
            all.filter {
                it.region == regionCode &&
                it.raion.contains(raion, ignoreCase = true) &&
                it.gorod == gorod
            }
                .map { it.street }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        }

    /**
     * Найти ближайшее отключение для заданного фильтра.
     * Работает в фоновом потоке (IO).
     */
    suspend fun findNextOutage(filter: OutageFilter): OutageResult =
        withContext(Dispatchers.IO) {
            try {
                val all = api.fetchAllOutages()
                val filtered = all
                    // 1. Только плановые отключения
                    .filter { it.fOtkl == "1" }
                    // 2. Фильтр по региону (сравниваем КОД)
                    .filter { filter.regionCode.isBlank() || it.region == filter.regionCode }
                    // 3. Фильтр по району
                    .filter { filter.raion.isBlank() || it.raion.contains(filter.raion, ignoreCase = true) }
                    // 4. Фильтр по городу
                    .filter { filter.gorod.isBlank() || it.gorod.contains(filter.gorod, ignoreCase = true) }
                    // 5. Фильтр по улице
                    .filter { filter.street.isBlank() || it.street.contains(filter.street, ignoreCase = true) }
                    // 6. Убираем только полностью прошедшие
                    .filter { !it.isPast() }
                    // 7. Сортируем по началу (ближайшие — первые)
                    .sortedBy { it.nextOutageKey() }

                OutageResult.Success(
                    next = filtered.firstOrNull(),
                    totalMatching = filtered.size
                )
            } catch (e: Exception) {
                OutageResult.Error(
                    message = when {
                        e.message?.contains("HTTP 403") == true ||
                        e.message?.contains("HTTP 451") == true ->
                            "Сайт недоступен из вашей страны (ошибка ${e.message})"
                        e.message?.contains("timeout") == true ->
                            "Сервер не ответил вовремя. Попробуйте позже."
                        e.message?.contains("Unable to resolve host") == true ->
                            "Нет интернет-соединения"
                        else -> "Ошибка загрузки: ${e.message ?: "неизвестная ошибка"}"
                    }
                )
            }
        }
}

sealed class OutageResult {
    data class Success(val next: Outage?, val totalMatching: Int) : OutageResult()
    data class Error(val message: String) : OutageResult()
}