package com.example.warehousescanner.data

import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object YandexDiskClient {
    private lateinit var api: YandexDiskApi
    private lateinit var authHeader: String

    fun init(oauthToken: String) {
        authHeader = "OAuth $oauthToken"
        api = Retrofit.Builder()
            .baseUrl("https://cloud-api.yandex.net/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YandexDiskApi::class.java)
    }

    suspend fun ensureFolder(path: String) {
        try { api.createFolder(authHeader, path) } catch (_: Exception) { }
    }

    private suspend fun uploadBytes(path: String, bytes: ByteArray) {
        val href = api.getUploadLink(authHeader, path).href
        val body: RequestBody = bytes.toRequestBody("application/octet-stream".toMediaType())
        api.uploadFile(href, body)
    }

    suspend fun uploadTextFile(path: String, text: String) {
        uploadBytes(path, text.toByteArray(Charsets.UTF_8))
    }

    suspend fun uploadImagesDated(
        photos: List<Uri>,
        context: Context,
        barcode: String
    ): String {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val root = "Warehouse"
        val folder = "$root/$date"
        ensureFolder(root)
        ensureFolder(folder)
        photos.forEachIndexed { index, uri ->
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                val name = "${barcode}_${index + 1}.jpg"
                uploadBytes("$folder/$name", bytes)
            }
        }
        return folder
    }
}
