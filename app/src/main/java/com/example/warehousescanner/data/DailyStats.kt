// data/DailyStats.kt
package com.example.warehousescanner.data

data class DailyStatsRequest(
    val mode: String = "dailyStats",
    val date: String? = null,
    val user: String? = null
)

data class DailyStatsResponse(
    val ok: Boolean,
    val nlo: Int = 0,
    val nonNlo: Int = 0,
    val total: Int = 0,
    val error: String? = null
)
