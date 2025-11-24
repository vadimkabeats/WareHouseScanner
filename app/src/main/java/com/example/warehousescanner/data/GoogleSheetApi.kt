package com.example.warehousescanner.data

import retrofit2.http.*

data class LookupResponse(val found: Boolean, val link: String?)
data class SaveResponse(val ok: Boolean?, val updated: Boolean? = null, val error: String? = null)

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
    val photos: List<String>,
    val strongPackaging: String
)

data class TrackLookupRequest(
    val mode: String = "track",
    val barcode: String,
    val gid: Long = 522894316L
)

data class TrackLookupResponse(
    val ok: Boolean,
    val found: Boolean,
    val track: String?,
    val full: String?,
    val multi: Boolean?,
    val qty_ship: Int?,
    val qty_total: Int?,
    val strong_pack: Boolean?,
    val label_url: String?      // ← НОВОЕ
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

data class ScanExistsRequest(val mode: String = "scanExists", val barcode: String)
data class ScanExistsResponse(val ok: Boolean, val exists: Boolean? = null, val error: String? = null)

data class ReturnLookupRequest(
    val mode: String = "returnLookup",
    val dispatchNumber: String
)
data class ReturnLookupResponse(
    val ok: Boolean,
    val found: Boolean,
    val barcode: String? = null,
    val reason: String? = null,
    val url: String? = null,
    val error: String? = null
)

/** НОВОЕ: добавили decision */
data class SaveReturnRequest(
    val mode: String = "saveReturn",
    val user: String,
    val dispatchNumber: String,
    val barcode: String,
    val defectDesc: String,
    val photos: List<String>,
    val decision: String
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
    suspend fun lookupTrack(
        @Url url: String,
        @Query("key") apiKey: String,
        @Body req: TrackLookupRequest
    ): TrackLookupResponse

    @POST
    @Headers("Content-Type: application/json", "Accept: application/json")
    suspend fun returnLookup(
        @Url scriptUrl: String,
        @Query("key") key: String,
        @Body body: ReturnLookupRequest
    ): ReturnLookupResponse

    @POST
    @Headers("Content-Type: application/json", "Accept: application/json")
    suspend fun saveReturn(
        @Url scriptUrl: String,
        @Query("key") key: String,
        @Body body: SaveReturnRequest
    ): SaveResponse

    @POST
    @Headers("Content-Type: application/json", "Accept: application/json")
    suspend fun reconcileInit(
        @Url scriptUrl: String,
        @Query("key") key: String,
        @Body body: ReconcileInitRequest = ReconcileInitRequest()
    ): ReconcileInitResponse

    @POST
    @Headers("Content-Type: application/json", "Accept: application/json")
    suspend fun dailyStats(
        @Url scriptUrl: String,
        @Query("key") key: String,
        @Body body: DailyStatsRequest
    ): DailyStatsResponse

    @POST
    suspend fun labelPrinted(
        @Url url: String,
        @Query("key") key: String,
        @Body body: LabelPrintedRequest
    ): LabelPrintedResponse
}
