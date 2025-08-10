package com.example.warehousescanner.data

import retrofit2.http.*

data class LookupResponse(val found: Boolean, val link: String?)
data class SaveRequest(val mode: String = "baseSave", val barcode: String, val link: String)
data class SaveResponse(val ok: Boolean?)

data class AfterUploadRequest(
    val mode: String = "afterUpload",
    val barcode: String,
    val baseLink: String,
    val status: String,
    val newLink: String,
    val qty: Int,
    val defects: String,
    val photos: List<String>
)

interface GoogleSheetApi {
    @GET
    suspend fun lookup(
        @Url scriptUrl: String,
        @Query("barcode") barcode: String,
        @Query("key") key: String
    ): LookupResponse

    @POST
    suspend fun save(
        @Url scriptUrl: String,
        @Query("key") key: String,
        @Body body: SaveRequest
    ): SaveResponse

    @POST
    suspend fun saveAfterUpload(
        @Url scriptUrl: String,
        @Query("key") key: String,
        @Body body: AfterUploadRequest
    ): SaveResponse
}
