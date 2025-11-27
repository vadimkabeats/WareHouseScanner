package com.example.warehousescanner.data

data class LostItemsRequest(
    val mode: String = "lostItems",
    val user: String
)

data class LostItem(
    val id: Long,
    val createdAt: String? = null,
    val barcode: String,
    val link: String? = null,
    val photo1: String? = null,
    val photo2: String? = null,
    val photo3: String? = null,
    val photo4: String? = null,
    val photo5: String? = null,
    val photo6: String? = null
) {
    val photos: List<String>
        get() = listOfNotNull(
            photo1?.takeIf { it.isNotBlank() },
            photo2?.takeIf { it.isNotBlank() },
            photo3?.takeIf { it.isNotBlank() },
            photo4?.takeIf { it.isNotBlank() },
            photo5?.takeIf { it.isNotBlank() },
            photo6?.takeIf { it.isNotBlank() },
        )
}

data class LostItemsResponse(
    val ok: Boolean,
    val items: List<LostItem> = emptyList(),
    val error: String? = null
)
