package com.example.warehousescanner.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class YDUploadResult(val folder: String, val publicPhotoUrls: List<String>)

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

    private fun todayFolder(): String {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return "Warehouse/$date"
    }

    private suspend fun ensureItemFolder(barcode: String): String {
        val dateRoot = todayFolder()
        ensureFolder("Warehouse")
        ensureFolder(dateRoot)
        val itemFolder = "$dateRoot/${barcode}_folder"
        ensureFolder(itemFolder)
        return itemFolder
    }

    /** Загружает metadata.json и фото в папку Warehouse/<date>/<BARCODE>_folder/ */
    suspend fun uploadItemBundleJson(
        context: Context,
        barcode: String,
        metadata: Any,
        photos: List<Uri>
    ): YDUploadResult {
        val folder = ensureItemFolder(barcode)

        // 1) metadata.json
        val json = Gson().toJson(metadata).toByteArray(Charsets.UTF_8)
        uploadBytes("$folder/metadata.json", json)

        // 2) фото: BARCODE_#.jpg + публикация
        val publicLinks = mutableListOf<String>()
        photos.forEachIndexed { i, uri ->
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                val photoName = "${barcode}_${i + 1}.jpg"
                val path = "$folder/$photoName"
                uploadBytes(path, bytes)
                // опубликовать и получить public_url (не обязательно, но полезно)
                runCatching { api.publish(authHeader, path) }
                val meta = runCatching { api.getResource(authHeader, path) }.getOrNull()
                meta?.publicUrl?.let { publicLinks.add(it) }
            }
        }
        return YDUploadResult(folder, publicLinks)
    }
}
