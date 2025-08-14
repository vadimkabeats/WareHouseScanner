package com.example.warehousescanner.data

import retrofit2.http.*

data class LookupResponse(val found: Boolean, val link: String?)
data class SaveRequest(
    val mode: String = "baseSave",
    val barcode: String,
    val link: String,
    val user: String
)
data class SaveResponse(val ok: Boolean?)

data class AfterUploadRequest(
    val mode: String = "afterUpload",
    val user: String,        // ФИО
    val barcode: String,
    val baseLink: String,
    val status: String,
    val newLink: String,
    val qty: Int,
    val durationSec: Int,    // время от сканирования до отправки
    val defects: String,
    val photos: List<String>
)

interface GoogleSheetApi {
    @GET
    @Headers("Accept: application/json")
    suspend fun lookup(@Url scriptUrl: String, @Query("barcode") barcode: String, @Query("key") key: String): LookupResponse

    @POST
    @Headers("Content-Type: application/json", "Accept: application/json")
    suspend fun save(@Url scriptUrl: String, @Query("key") key: String, @Body body: SaveRequest): SaveResponse

    @POST
    @Headers("Content-Type: application/json", "Accept: application/json")
    suspend fun saveAfterUpload(@Url scriptUrl: String, @Query("key") key: String, @Body body: AfterUploadRequest): SaveResponse
}
