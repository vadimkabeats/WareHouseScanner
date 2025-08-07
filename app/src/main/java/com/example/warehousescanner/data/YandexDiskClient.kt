package com.example.warehousescanner.data

import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaType
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

    /** Инициализация токена OAuth */
    fun init(oauthToken: String) {
        token = oauthToken
    }

    /** Загружает байты по path */
    private suspend fun uploadBytes(path: String, bytes: ByteArray) {
        val link = api.getUploadLink("OAuth $token", path)
        api.uploadFile(link.href, bytes.toRequestBody("application/octet-stream".toMediaType()))
    }

    /**
     * Загружает фото, публикует их и возвращает список публичных URL
     */
    suspend fun uploadAndPublishImages(
        folder: String,
        photos: List<Uri>,
        context: Context
    ): List<String> {
        val urls = mutableListOf<String>()
        photos.forEachIndexed { index, uri ->
            val input = context.contentResolver.openInputStream(uri)
            val bytes = input!!.use { it.readBytes() }
            val filePath = "$folder/photo_${index + 1}.jpg"
            // 1) загружаем файл
            uploadBytes(filePath, bytes)
            // 2) публикуем и получаем публичный URL
            val pub = api.publishResource("OAuth $token", filePath)
            urls += pub.publicUrl
        }
        return urls
    }

    /**
     * Загружает JSON-метаданные по path
     */
    suspend fun uploadMetadata(path: String, metadata: Any) {
        val json = com.google.gson.Gson().toJson(metadata)
        uploadBytes(path, json.toByteArray(Charsets.UTF_8))
    }
}
