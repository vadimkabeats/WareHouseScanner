package com.example.warehousescanner.data

import retrofit2.http.*

interface FastApi {
    // GET /api?barcode=...
    @GET
    suspend fun lookup(
        @Url url: String,
        @Query("barcode") barcode: String
    ): LookupResponse

    // POST /api  { mode="afterUpload", ... }
    @POST
    suspend fun saveAfterUpload(
        @Url url: String,
        @Body body: AfterUploadRequest
    ): SaveResponse

    @POST
    suspend fun putAway(
        @Url url: String,
        @Body body: PutAwayRequest
    ): SaveResponse

    @POST
    suspend fun auth(
        @Url url: String,
        @Body body: AuthRequest
    ): AuthResponse

    @POST
    suspend fun scanExists(
        @Url url: String,
        @Body body: ScanExistsRequest
    ): ScanExistsResponse

    @POST
    suspend fun dailyStats(
        @Url url: String,
        @Body body: DailyStatsRequest
    ): DailyStatsResponse

    @POST
    suspend fun lostItems(
        @Url url: String,
        @Body body: LostItemsRequest
    ): LostItemsResponse

}
