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

private class RetryOnTimeoutInterceptor(
    private val retries: Int = 1
) : Interceptor {
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

object GoogleSheetClient {
    private lateinit var api: GoogleSheetApi
    private var scriptUrl: String = ""
    private var apiKey: String = ""

    fun init(scriptExecUrl: String, key: String) {
        scriptUrl = scriptExecUrl
        apiKey = key

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val ok = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(RetryOnTimeoutInterceptor(retries = 1))
            .retryOnConnectionFailure(true)
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .build()

        val gson = GsonBuilder().setLenient().create()

        api = Retrofit.Builder()
            .baseUrl("https://script.google.com/")
            .client(ok)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(GoogleSheetApi::class.java)
    }

    suspend fun lookup(barcode: String) =
        api.lookup(scriptUrl, barcode, apiKey)

    suspend fun saveAfterUpload(req: AfterUploadRequest) =
        api.saveAfterUpload(scriptUrl, apiKey, req)

    suspend fun putAway(itemBarcode: String, cellBarcode: String, durationSec: Int, userFio: String) =
        api.putAway(scriptUrl, apiKey, PutAwayRequest(user = userFio, barcode = itemBarcode, cell = cellBarcode, durationSec = durationSec))

    suspend fun auth(first: String, last: String, password: String) =
        api.auth(scriptUrl, apiKey, AuthRequest(firstName = first, lastName = last, password = password))

    suspend fun lookupTrack(barcode: String, gid: Long = 522894316L) =
        api.lookupTrack(scriptUrl, apiKey, TrackLookupRequest(barcode = barcode, gid = gid))

    suspend fun scanExists(barcode: String): Boolean {
        val resp = api.scanExists(scriptUrl, apiKey, ScanExistsRequest(barcode = barcode))
        return resp.ok && (resp.exists == true)
    }
}
