package com.example.warehousescanner.data

import okhttp3.OkHttpClient
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.GsonBuilder
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

private class RetryOnTimeoutInterceptor(private val retries: Int = 1) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var tryCount = 0
        var lastEx: IOException? = null
        while (tryCount <= retries) {
            try { return chain.proceed(chain.request()) }
            catch (e: IOException) {
                lastEx = e
                if (e is SocketTimeoutException && tryCount < retries) { tryCount++; continue }
                throw e
            }
        }
        throw lastEx ?: IOException("Unknown network error")
    }
}

object GoogleSheetClient {
    private lateinit var fastApi: FastApi
    private lateinit var gasApi: GoogleSheetApi

    private var fastApiUrl: String = "http://158.160.87.160:8000/api"
    private var gasUrl: String = "https://script.google.com/macros/s/AKfycbwDnWWw3GS2C5cBX3u-G_7NEhFiZXSnhUX7LeysLgnq7ZJjmxiwhQrWjjMGlGdFOSQ/exec"
    private var apiKey: String = "SECRET_KEY"

    fun init(fastApiUrl: String, gasExecUrl: String, key: String) {
        this.fastApiUrl = fastApiUrl.trimEnd('/')
        this.gasUrl     = gasExecUrl
        this.apiKey     = key

        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val ok = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(RetryOnTimeoutInterceptor(retries = 1))
            .retryOnConnectionFailure(true)
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.SECONDS)
            .build()

        val gson = GsonBuilder().setLenient().create()

        fun buildRetrofit(): Retrofit = Retrofit.Builder()
            .baseUrl("https://localhost/")
            .client(ok)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        fastApi = buildRetrofit().create(FastApi::class.java)
        gasApi  = buildRetrofit().create(GoogleSheetApi::class.java)
    }

    suspend fun lookup(barcode: String) =
        fastApi.lookup(fastApiUrl, barcode)

    suspend fun saveAfterUpload(req: AfterUploadRequest) =
        fastApi.saveAfterUpload(fastApiUrl, req)

    suspend fun putAway(itemBarcode: String, cellBarcode: String, durationSec: Int, userFio: String) =
        fastApi.putAway(
            fastApiUrl,
            PutAwayRequest(user = userFio, barcode = itemBarcode, cell = cellBarcode, durationSec = durationSec)
        )

    suspend fun auth(first: String, last: String, password: String) =
        fastApi.auth(
            fastApiUrl,
            AuthRequest(firstName = first, lastName = last, password = password)
        )

    suspend fun scanExists(barcode: String): Boolean {
        val resp = fastApi.scanExists(fastApiUrl, ScanExistsRequest(barcode = barcode))
        return resp.ok && (resp.exists == true)
    }

    suspend fun lostItems(user: String): LostItemsResponse =
        fastApi.lostItems(
            fastApiUrl,
            LostItemsRequest(user = user)
        )

    suspend fun dailyStats(dateIso: String?, user: String?): DailyStatsResponse =
        fastApi.dailyStats(fastApiUrl, DailyStatsRequest(date = dateIso, user = user))

    suspend fun lookupTrack(barcode: String, gid: Long = 522894316L) =
        gasApi.lookupTrack(gasUrl, apiKey, TrackLookupRequest(barcode = barcode, gid = gid))

    suspend fun returnLookup(dispatchNumber: String): ReturnLookupResponse =
        gasApi.returnLookup(gasUrl, apiKey, ReturnLookupRequest(dispatchNumber = dispatchNumber))

    suspend fun saveReturn(
        user: String,
        dispatchNumber: String,
        barcode: String,
        defectDesc: String,
        photoLinks: List<String>,
        decision: String
    ): SaveResponse {
        val body = SaveReturnRequest(
            user = user,
            dispatchNumber = dispatchNumber,
            barcode = barcode,
            defectDesc = defectDesc,
            photos = photoLinks,
            decision = decision
        )
        return gasApi.saveReturn(gasUrl, apiKey, body)
    }

    suspend fun returnIntake(
        trackNumber: String,
        photoLinks: List<String>,
        comment: String? = null
    ): SimpleOkResponse {
        val body = ReturnIntakeRequest(
            trackNumber = trackNumber,
            photos = photoLinks,
            comment = comment
        )
        return gasApi.returnIntake(gasUrl, apiKey, body)
    }


    suspend fun reconcileInit(): List<ReconcileItem> {
        val resp = gasApi.reconcileInit(gasUrl, apiKey, ReconcileInitRequest())
        if (!resp.ok) error(resp.error ?: "reconcileInit failed")
        return resp.items
    }

    suspend fun labelPrinted(
        trackFull: String?,
        trackShort: String?,
        printedAtMs: Long? = null
    ): LabelPrintedResponse =
        gasApi.labelPrinted(
            gasUrl,
            apiKey,
            LabelPrintedRequest(
                track_full = trackFull,
                track_short = trackShort,
                printed_at_ms = printedAtMs ?: System.currentTimeMillis()
            )
        )

    suspend fun returnProcessLookup(trackNumber: String): ReturnProcessLookupResponse =
        gasApi.returnProcessLookup(
            gasUrl,
            apiKey,
            ReturnProcessLookupRequest(trackNumber = trackNumber)
        )

    suspend fun returnProcessDone(trackNumber: String): ReturnProcessDoneResponse =
        gasApi.returnProcessDone(
            gasUrl,
            apiKey,
            ReturnProcessDoneRequest(trackNumber = trackNumber)
        )
}
