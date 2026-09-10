package com.rosseti.alert.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Модель одного планового отключения.
 * Поля соответствуют JSON-структуре из data.php.
 */
data class Outage(
    val id: String,
    val region: String,        // код региона (19 — Хакасия)
    val raion: String,         // район
    val gorod: String,         // населённый пункт
    val street: String,        // улица
    val dateStart: LocalDate,
    val dateFinish: LocalDate,
    val timeStart: LocalTime,
    val timeFinish: LocalTime,
    val fOtkl: String,         // "1" — плановое отключение
    val res: String            // подразделение (РЭС)
)

/** Вспомогательные функции для парсинга из JSON */
fun parseDate(s: String): LocalDate =
    LocalDate.parse(s, DateTimeFormatter.ofPattern("dd.MM.yyyy"))

fun parseTime(s: String): LocalTime =
    LocalTime.parse(s, DateTimeFormatter.ofPattern("HH:mm"))

/** Проверяет, полностью ли завершилось отключение (вчера или сегодня, но время уже прошло) */
fun Outage.isPast(): Boolean {
    val now = LocalDateTime.now()
    val end = LocalDateTime.of(dateFinish, timeFinish)
    return end.isBefore(now)
}

/** Активно ли отключение прямо сейчас (идёт в данный момент) */
fun Outage.isActive(): Boolean {
    val now = LocalDateTime.now()
    val start = LocalDateTime.of(dateStart, timeStart)
    val end = LocalDateTime.of(dateFinish, timeFinish)
    return !now.isBefore(start) && now.isBefore(end)
}

/** Сортировка по дате начала */
fun Outage.nextOutageKey(): LocalDateTime = LocalDateTime.of(dateStart, timeStart)