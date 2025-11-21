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
            try {
                return chain.proceed(chain.request())
            } catch (e: IOException) {
                lastEx = e
                if (e is SocketTimeoutException && tryCount < retries) {
                    tryCount++
                    continue
                }
                throw e
            }
        }
        throw lastEx ?: IOException("Unknown network error")
    }
}

/**
 * Теперь ВСЁ через Apps Script:
 * - auth / scanExists / lookup / afterUpload / putAway / dailyStats
 * - lookupTrack / returnLookup / saveReturn / reconcileInit / labelPrinted
 */
object GoogleSheetClient {
    private lateinit var gasApi: GoogleSheetApi

    private var gasUrl: String = ""
    private var apiKey: String = ""

    /**
     * Инициализация ТОЛЬКО для Apps Script.
     * fastApi нам больше не нужен.
     */
    fun init(gasExecUrl: String, key: String) {
        this.gasUrl = gasExecUrl
        this.apiKey = key

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

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

        val retrofit = Retrofit.Builder()
            // Базовый URL-заглушка, реальный URL всегда передаём через @Url
            .baseUrl("https://script.google.com/")
            .client(ok)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        gasApi = retrofit.create(GoogleSheetApi::class.java)
    }

    /* ---------------- БАЗА/СКАНИРОВКА/ХРАНЕНИЕ/АККАУНТЫ ---------------- */

    // Поиск ссылки по ШК (GET /exec?barcode=...&key=...)
    suspend fun lookup(barcode: String): LookupResponse =
        gasApi.lookup(gasUrl, barcode, apiKey)

    // Добавление/обновление строки в листе "Сканировка" (mode = afterUpload)
    suspend fun saveAfterUpload(req: AfterUploadRequest): SaveResponse =
        gasApi.saveAfterUpload(gasUrl, apiKey, req)

    // Хранение (лист "Хранение", mode = putAway)
    suspend fun putAway(
        itemBarcode: String,
        cellBarcode: String,
        durationSec: Int,
        userFio: String
    ): SaveResponse =
        gasApi.putAway(
            gasUrl,
            apiKey,
            PutAwayRequest(
                user = userFio,
                barcode = itemBarcode,
                cell = cellBarcode,
                durationSec = durationSec
            )
        )

    // Логин по фамилии/имени/паролю (лист "Аккаунты", mode = auth)
    suspend fun auth(first: String, last: String, password: String): AuthResponse =
        gasApi.auth(
            gasUrl,
            apiKey,
            AuthRequest(firstName = first, lastName = last, password = password)
        )

    // Проверка дубля в "Сканировка" (mode = scanExists)
    suspend fun scanExists(barcode: String): Boolean {
        val resp = gasApi.scanExists(
            gasUrl,
            apiKey,
            ScanExistsRequest(barcode = barcode)
        )
        return resp.ok && (resp.exists == true)
    }

    // Статистика за день (mode = dailyStats)
    suspend fun dailyStats(dateIso: String?, user: String?): DailyStatsResponse =
        gasApi.dailyStats(
            gasUrl,
            apiKey,
            DailyStatsRequest(date = dateIso, user = user)
        )

    /* ---------------- ЭТИКЕТКИ / ВОЗВРАТЫ / СВЕРКА ---------------- */

    suspend fun lookupTrack(barcode: String, gid: Long = 522894316L): TrackLookupResponse =
        gasApi.lookupTrack(
            gasUrl,
            apiKey,
            TrackLookupRequest(barcode = barcode, gid = gid)
        )

    suspend fun returnLookup(dispatchNumber: String): ReturnLookupResponse =
        gasApi.returnLookup(
            gasUrl,
            apiKey,
            ReturnLookupRequest(dispatchNumber = dispatchNumber)
        )

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

    suspend fun reconcileInit(): List<ReconcileItem> {
        val resp = gasApi.reconcileInit(
            gasUrl,
            apiKey,
            ReconcileInitRequest()
        )
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
}
