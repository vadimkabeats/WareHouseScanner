package com.example.warehousescanner.data

import retrofit2.http.*

data class LookupResponse(val found: Boolean, val link: String?)
data class SaveResponse(val ok: Boolean?, val updated: Boolean? = null)

/* ---------- AfterUpload ---------- */
data class AfterUploadRequest(
    val mode: String = "afterUpload",
    val user: String,
    val barcode: String,
    val baseLink: String,
    val status: String,
    val newLink: String,
    val qty: Int,
    val durationSec: Int,
    val defects: String,
    val photos: List<String>
)

/* ---------- Track lookup ---------- */
data class TrackLookupRequest(
    val mode: String = "track",
    val barcode: String,
    // ВАЖНО: корректный GID листа «Этикетки»
    val gid: Long = 522894316L
)

data class TrackLookupResponse(
    val ok: Boolean,
    val found: Boolean,
    val track: String? = null,
    val full: String? = null,
    val multi: Boolean? = null,
    val error: String? = null
)


data class PutAwayRequest(
    val mode: String = "putAway",
    val user: String,
    val barcode: String,
    val cell: String,
    val durationSec: Int
)


data class AuthRequest(
    val mode: String = "auth",
    val firstName: String,
    val lastName: String,
    val password: String
)
data class AuthResponse(val ok: Boolean, val fio: String? = null, val error: String? = null)


data class ScanExistsRequest(
    val mode: String = "scanExists",
    val barcode: String
)
data class ScanExistsResponse(
    val ok: Boolean,
    val exists: Boolean? = null,
    val error: String? = null
)

interface GoogleSheetApi {
    @GET
    @Headers("Accept: application/json")
    suspend fun lookup(
        @Url scriptUrl: String,
        @Query("barcode") barcode: String,
        @Query("key") key: String
    ): LookupResponse

    @POST
    @Headers("Content-Type: application/json", "Accept: application/json")
    suspend fun saveAfterUpload(
        @Url scriptUrl: String,
        @Query("key") key: String,
        @Body body: AfterUploadRequest
    ): SaveResponse

    @POST
    @Headers("Content-Type: application/json", "Accept: application/json")
    suspend fun putAway(
        @Url scriptUrl: String,
        @Query("key") key: String,
        @Body body: PutAwayRequest
    ): SaveResponse

    @POST
    @Headers("Content-Type: application/json", "Accept: application/json")
    suspend fun auth(
        @Url scriptUrl: String,
        @Query("key") key: String,
        @Body body: AuthRequest
    ): AuthResponse

    @POST
    @Headers("Content-Type: application/json", "Accept: application/json")
    suspend fun scanExists(
        @Url scriptUrl: String,
        @Query("key") key: String,
        @Body body: ScanExistsRequest
    ): ScanExistsResponse

    @POST
    @Headers("Content-Type: application/json", "Accept: application/json")
    suspend fun lookupTrack(
        @Url url: String,
        @Query("key") apiKey: String,
        @Body req: TrackLookupRequest
    ): TrackLookupResponse
}
