package com.example.warehousescanner.data

import retrofit2.http.*

data class LookupResponse(val found: Boolean, val link: String?)
data class SaveRequest(val barcode: String, val link: String)
data class SaveResponse(val ok: Boolean?)

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
}
