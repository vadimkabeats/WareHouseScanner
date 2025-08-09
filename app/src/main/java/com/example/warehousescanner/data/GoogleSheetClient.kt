package com.example.warehousescanner.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object GoogleSheetClient {
    private lateinit var api: GoogleSheetApi
    private var scriptUrl: String = ""
    private var apiKey: String = ""

    fun init(scriptExecUrl: String, key: String) {
        scriptUrl = scriptExecUrl
        apiKey = key
        api = Retrofit.Builder()
            .baseUrl("https://script.google.com/") // базовый домен, сам вызов — с @Url
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleSheetApi::class.java)
    }

    suspend fun lookup(barcode: String) =
        api.lookup(scriptUrl, barcode, apiKey)

    suspend fun save(barcode: String, link: String) =
        api.save(scriptUrl, apiKey, SaveRequest(barcode, link))
}
