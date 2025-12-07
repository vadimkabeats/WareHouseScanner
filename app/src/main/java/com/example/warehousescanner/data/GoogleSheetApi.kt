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
    val strongPackaging: String,
    val toUtil: String
)

data class TrackLookupRequest(
    val mode: String = "track",
    val barcode: String,
    val gid: Long = 400055422L
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
    val label_url: String?,
    val name: String? = null
)

data class ReturnIntakeRequest(
    val mode: String = "returnIntake",
    val trackNumber: String,
    val photos: List<String>
)

data class SimpleOkResponse(
    val ok: Boolean,
    val error: String?
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
    val error: String? = null,
    val items: List<ReturnLookupItem>? = null
)

data class ReturnLookupItem(
    val barcode: String? = null,
    val title: String? = null,
    val url: String? = null,
    val reason: String? = null
)

data class ReturnProcessLookupRequest(
    val mode: String = "returnsProcessLookup",
    val trackNumber: String
)

data class ReturnProcessItemDto(
    val barcode: String,
    val title: String?,
    val action: String?
)

data class ReturnProcessLookupResponse(
    val ok: Boolean,
    val found: Boolean? = null,
    val items: List<ReturnProcessItemDto>? = null,
    val error: String? = null
)

data class ReturnProcessDoneRequest(
    val mode: String = "returnProcessDone",
    val trackNumber: String
)

data class ReturnProcessDoneResponse(
    val ok: Boolean,
    val updatedRows: Int? = null,
    val error: String? = null
)

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

    @POST
    suspend fun returnIntake(
        @Url url: String,
        @Query("key") key: String,
        @Body body: ReturnIntakeRequest
    ): SimpleOkResponse

    @POST
    suspend fun returnProcessLookup(
        @Url url: String,
        @Query("key") key: String,
        @Body body: ReturnProcessLookupRequest
    ): ReturnProcessLookupResponse

    @POST
    suspend fun returnProcessDone(
        @Url url: String,
        @Query("key") key: String,
        @Body body: ReturnProcessDoneRequest
    ): ReturnProcessDoneResponse

}
