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

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    // --------- Сценарий «Добавить товар» ---------
    private fun todayWarehouseFolder(): String = "Warehouse/${today()}"

    private suspend fun ensureItemFolder(barcode: String): String {
        val dateRoot = todayWarehouseFolder()
        ensureFolder("Warehouse")
        ensureFolder(dateRoot)
        val itemFolder = "$dateRoot/${barcode}_folder"
        ensureFolder(itemFolder)
        return itemFolder
    }

    suspend fun uploadItemBundleJson(
        context: Context,
        barcode: String,
        metadata: Any,
        photos: List<Uri>
    ): YDUploadResult {
        val folder = ensureItemFolder(barcode)

        val json = Gson().toJson(metadata).toByteArray(Charsets.UTF_8)
        uploadBytes("$folder/metadata.json", json)

        val publicLinks = mutableListOf<String>()
        photos.forEachIndexed { i, uri ->
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                val photoName = "${barcode}_${i + 1}.jpg"
                val path = "$folder/$photoName"
                uploadBytes(path, bytes)
                runCatching { api.publish(authHeader, path) }
                val meta = runCatching { api.getResource(authHeader, path) }.getOrNull()
                meta?.publicUrl?.let { publicLinks.add(it) }
            }
        }
        return YDUploadResult(folder, publicLinks)
    }

    // --------- Сценарий «Возвраты» ---------
    private fun todayReturnsFolder(): String = "Возвраты/${today()}"

    private suspend fun ensureReturnItemFolder(barcode: String): String {
        val dateRoot = todayReturnsFolder()
        ensureFolder("Возвраты")
        ensureFolder(dateRoot)
        val itemFolder = "$dateRoot/${barcode}_folder"
        ensureFolder(itemFolder)
        return itemFolder
    }

    /**
     * Загрузка пакета для возврата:
     * - metadata_return.json
     * - фото <barcode>_1.jpg ... (_до 6)
     * Папка: Возвраты/yyyy-MM-dd/<barcode>_folder
     */
    suspend fun uploadReturnBundleJson(
        context: Context,
        barcode: String,
        metadata: Any,
        photos: List<Uri>
    ): YDUploadResult {
        val folder = ensureReturnItemFolder(barcode)

        val json = Gson().toJson(metadata).toByteArray(Charsets.UTF_8)
        uploadBytes("$folder/metadata_return.json", json)

        val publicLinks = mutableListOf<String>()
        photos.forEachIndexed { i, uri ->
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                val photoName = "${barcode}_${i + 1}.jpg"
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
