package com.example.warehousescanner.data

data class ReconcileItem(
    val barcode: String,
    val name: String,
    val track: String,
    val carrier: String,
    val printedAt: String,
    val processed: Boolean,
    val url: String = ""
)

data class ReconcileInitRequest(val mode: String = "reconcileInit")
data class ReconcileInitResponse(
    val ok: Boolean,
    val items: List<ReconcileItem> = emptyList(),
    val error: String? = null
)
