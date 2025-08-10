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

    private fun safeNameFromUrl(url: String, maxLen: Int = 80): String =
        url.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "_").trim('_').let {
            if (it.isEmpty()) "item" else it
        }.take(maxLen)

    private fun todayFolder(): String {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return "Warehouse/$date"
    }

    private suspend fun ensureDateUrlFolder(url: String): String {
        val rootDate = todayFolder()
        val urlFolder = safeNameFromUrl(url)
        ensureFolder("Warehouse")
        ensureFolder(rootDate)
        val full = "$rootDate/$urlFolder"
        ensureFolder(full)
        return full
    }

    suspend fun uploadItemBundle(
        context: Context,
        url: String,
        barcode: String,
        textContent: String,
        photos: List<Uri>
    ): YDUploadResult {
        val folder = ensureDateUrlFolder(url)

        val txtName = "${safeNameFromUrl(url)}.txt"
        uploadBytes("$folder/$txtName", textContent.toByteArray(Charsets.UTF_8))

        val publicLinks = mutableListOf<String>()
        photos.forEachIndexed { index, uri ->
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                val photoName = "${barcode}_${index + 1}.jpg"
                val path = "$folder/$photoName"
                uploadBytes(path, bytes)
                      runCatching { api.publish(authHeader, path) }
                val meta = runCatching { api.getResource(authHeader, path) }.getOrNull()
                meta?.publicUrl?.let { publicLinks.add(it) }
            }
        }
        return YDUploadResult(folder, publicLinks)
    }
}
