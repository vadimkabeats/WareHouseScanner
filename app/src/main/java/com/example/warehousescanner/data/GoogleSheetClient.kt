package com.example.warehousescanner.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.GsonBuilder

object GoogleSheetClient {
    private lateinit var api: GoogleSheetApi
    private var scriptUrl: String = ""
    private var apiKey: String = ""

    fun init(scriptExecUrl: String, key: String) {
        scriptUrl = scriptExecUrl
        apiKey = key
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val ok = OkHttpClient.Builder().addInterceptor(logging).build()
        val gson = GsonBuilder().setLenient().create()
        api = Retrofit.Builder()
            .baseUrl("https://script.google.com/")
            .client(ok)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(GoogleSheetApi::class.java)
    }

    suspend fun lookup(barcode: String) = api.lookup(scriptUrl, barcode, apiKey)
    suspend fun save(barcode: String, link: String, user: String) =
        api.save(scriptUrl, apiKey, SaveRequest(barcode = barcode, link = link, user = user))
    suspend fun saveAfterUpload(req: AfterUploadRequest) =
        api.saveAfterUpload(scriptUrl, apiKey, req)
}
