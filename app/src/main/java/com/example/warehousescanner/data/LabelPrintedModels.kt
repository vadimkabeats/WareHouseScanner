package com.example.warehousescanner.data

data class LabelPrintedRequest(
    val mode: String = "labelPrinted",
    val track_full: String?,
    val track_short: String?,
    val printed_at_ms: Long? = null
)

data class LabelPrintedResponse(
    val ok: Boolean,
    val row: Int? = null,
    val time: String? = null,
    val error: String? = null
)
