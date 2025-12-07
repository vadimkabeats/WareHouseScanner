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
    val identified: Int = 0,
    val identifiedPut: Int = 0,
    val identifiedNotPut: Int = 0,
    val totalIdentified: Int = 0,
    val totalPutAway: Int = 0,
    val error: String? = null
)
