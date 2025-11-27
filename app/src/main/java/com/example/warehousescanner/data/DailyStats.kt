// data/DailyStats.kt
package com.example.warehousescanner.data

data class DailyStatsRequest(
    val mode: String = "dailyStats",
    val date: String? = null,   // пока не используем
    val user: String? = null
)

/**
 * Ответ от backend (FastAPI /api, mode=dailyStats).
 * JSON:
 * {
 *   "ok": true,
 *   "nlo": 5,
 *   "nonNlo": 40,
 *   "total": 45,
 *   "identified": 30,          // ЛИЧНО: идентифицировано
 *   "identifiedPut": 28,       // ЛИЧНО: из них дошло до полки
 *   "identifiedNotPut": 2,     // ЛИЧНО: из них НЕ дошло до полки
 *   "totalIdentified": 60,     // СКЛАД: всего идентифицировано за сегодня
 *   "totalPutAway": 55,        // СКЛАД: всего дошло до полки за сегодня
 *   "error": null
 * }
 */
data class DailyStatsResponse(
    val ok: Boolean,
    val nlo: Int = 0,
    val nonNlo: Int = 0,
    val total: Int = 0,

    // ЛИЧНАЯ тройка
    val identified: Int = 0,
    val identifiedPut: Int = 0,
    val identifiedNotPut: Int = 0,

    // СУММАРНО по складу
    val totalIdentified: Int = 0,
    val totalPutAway: Int = 0,

    val error: String? = null
)
