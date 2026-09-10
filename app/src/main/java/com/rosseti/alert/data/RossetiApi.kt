package com.rosseti.alert.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * HTTP-клиент для API сайта Россети Сибирь.
 *
 * data.php возвращает ВСЕ записи единым JSON-массивом (до 6000+ записей),
 * фильтрация — на клиенте.
 * regions.php возвращает {value: код, title: название} для всех регионов.
 */
class RossetiApi {

    companion object {
        private const val DATA_URL =
            "https://www.rosseti-sib.ru/local/templates/rosseti/components/is/proxy/shutdown_schedule_table/data.php"

        private const val REGIONS_URL =
            "https://www.rosseti-sib.ru/local/templates/rosseti/components/is/proxy/shutdown_schedule_table/regions.php"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /** Загрузить список регионов: [код, название] */
    fun fetchRegions(): List<Region> {
        val request = Request.Builder()
            .url(REGIONS_URL)
            .header("Accept", "application/json, text/plain, */*")
            .header("Referer", "https://www.rosseti-sib.ru/otkluchenie-energii/")
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw RuntimeException("HTTP ${response.code}: ${response.message}")
        }

        val body = response.body?.string() ?: throw RuntimeException("Пустой ответ")
        val jsonArray = JSONArray(body)
        val regions = mutableListOf<Region>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            regions.add(Region(
                code = obj.getString("value"),
                name = obj.getString("title")
            ))
        }
        return regions
    }

    /** Загрузить все записи из data.php */
    fun fetchAllOutages(): List<Outage> {
        val request = Request.Builder()
            .url(DATA_URL)
            .header("Accept", "application/json, text/plain, */*")
            .header("Referer", "https://www.rosseti-sib.ru/otkluchenie-energii/")
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw RuntimeException("HTTP ${response.code}: ${response.message}")
        }

        val body = response.body?.string()
            ?: throw RuntimeException("Пустой ответ сервера")

        val jsonArray = JSONArray(body)
        val outages = mutableListOf<Outage>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            try {
                outages.add(
                    Outage(
                        id = obj.getString("id"),
                        region = obj.optString("region", "").trim(),
                        raion = obj.optString("raion", "").trim(),
                        gorod = obj.optString("gorod", "").trim(),
                        street = obj.optString("street", "").trim(),
                        dateStart = parseDate(obj.getString("date_start")),
                        dateFinish = parseDate(obj.getString("date_finish")),
                        timeStart = parseTime(obj.getString("time_start")),
                        timeFinish = parseTime(obj.getString("time_finish")),
                        fOtkl = obj.optString("f_otkl", "0"),
                        res = obj.optString("res", "").trim()
                    )
                )
            } catch (e: Exception) {
                android.util.Log.w("RossetiApi", "Ошибка парсинга #$i: ${e.message}")
            }
        }

        return outages
    }
}

data class Region(
    val code: String,
    val name: String
)