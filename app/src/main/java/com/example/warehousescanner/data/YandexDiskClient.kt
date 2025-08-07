// YandexDiskClient.kt
package com.example.warehousescanner.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object YandexDiskClient {
    private const val BASE_URL = "https://cloud-api.yandex.net/"
    private lateinit var token: String
    private val api by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YandexDiskApi::class.java)
    }

    fun init(oauthToken: String) {
        token = oauthToken
    }

    private suspend fun uploadBytes(path: String, bytes: ByteArray) {
        val link = api.getUploadLink("OAuth $token", path)
        val body = bytes.toRequestBody("application/octet-stream".toMediaType())
        api.uploadFile(link.href, body)
    }

    suspend fun uploadMetadata(path: String, metadata: Any) {
        val json = Gson().toJson(metadata)
        uploadBytes(path, json.toByteArray(Charsets.UTF_8))
    }

    suspend fun uploadImages(folder: String, photos: List<Uri>, context: Context) {
        photos.forEachIndexed { index, uri ->
            val input = context.contentResolver.openInputStream(uri)
            val bytes = input?.use { it.readBytes() } ?: return@forEachIndexed
            val fileName = "$folder/photo_${index + 1}.jpg"
            uploadBytes(fileName, bytes)
        }
    }
}
