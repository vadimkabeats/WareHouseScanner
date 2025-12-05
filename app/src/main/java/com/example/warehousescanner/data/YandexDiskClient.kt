package com.example.warehousescanner.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

data class YDUploadResult(val folder: String, val publicPhotoUrls: List<String>)

/** Локальный ретрай только для YandexDisk — чтобы не конфликтовать с GoogleSheetClient */
private class YdRetryOnTimeoutInterceptor(private val retries: Int = 2) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        var tries = 0
        var last: IOException? = null
        while (tries <= retries) {
            try { return chain.proceed(chain.request()) }
            catch (e: IOException) {
                last = e
                if (e is SocketTimeoutException && tries < retries) { tries++; continue }
                throw e
            }
        }
        throw last ?: IOException("Unknown network error")
    }
}

object YandexDiskClient {
    private lateinit var api: YandexDiskApi
    private lateinit var authHeader: String
    private suspend fun loadAndCompressJpeg(
        context: Context,
        uri: Uri,
        maxSide: Int = 1600,   // максимальная сторона в пикселях
        quality: Int = 80      // качество JPEG
    ): ByteArray = withContext(Dispatchers.IO) {
        // 1) Сначала только узнаём размеры
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }

        val origW = bounds.outWidth
        val origH = bounds.outHeight
        if (origW <= 0 || origH <= 0) return@withContext ByteArray(0)

        // 2) Считаем inSampleSize (во сколько раз уменьшать)
        var sample = 1
        val maxOrigSide = maxOf(origW, origH)
        while (maxOrigSide / sample > maxSide) {
            sample *= 2
        }

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }

        val bmp = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return@withContext ByteArray(0)

        // 3) Сжимаем в JPEG
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, out)
        bmp.recycle()
        out.toByteArray()
    }

    fun init(oauthToken: String) {
        authHeader = "OAuth $oauthToken"

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val ok = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(YdRetryOnTimeoutInterceptor(retries = 2))
            .retryOnConnectionFailure(true)
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)  // больше времени на отправку
            .readTimeout(120, TimeUnit.SECONDS)   // больше времени на ответ
            .callTimeout(0, TimeUnit.SECONDS)     // без общего дедлайна
            .build()

        api = Retrofit.Builder()
            .baseUrl("https://cloud-api.yandex.net/")
            .client(ok)
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

    // --------- «Добавить товар» ---------
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
        // 1) Папка под товар на сегодня
        val folder = ensureItemFolder(barcode)

        // 2) metadata.json (один небольшой файл — можно как раньше, синхронно)
        val json = Gson().toJson(metadata).toByteArray(Charsets.UTF_8)
        uploadBytes("$folder/metadata.json", json)

        // 3) Фото — сжимаем и грузим ПАРАЛЛЕЛЬНО
        val publicLinks = coroutineScope {
            photos.mapIndexed { i, uri ->
                async(Dispatchers.IO) {
                    // 3.1) читаем и сжимаем
                    val bytes = loadAndCompressJpeg(context, uri)
                    if (bytes.isEmpty()) return@async null

                    val photoName = "${barcode}_${i + 1}.jpg"
                    val path = "$folder/$photoName"

                    // 3.2) грузим на Диск
                    uploadBytes(path, bytes)

                    // 3.3) публикуем и достаём public_url (как раньше)
                    runCatching {
                        api.publish(authHeader, path)
                        api.getResource(authHeader, path).publicUrl
                    }.getOrNull()
                }
            }.mapNotNull { it.await() }  // оставляем только не-null ссылки
        }

        return YDUploadResult(folder, publicLinks)
    }

    private fun todayReturnsFolder(): String = "Возвраты/${today()}"

    private suspend fun ensureReturnItemFolder(barcode: String): String {
        val dateRoot = todayReturnsFolder()
        ensureFolder("Возвраты")
        ensureFolder(dateRoot)
        val itemFolder = "$dateRoot/${barcode}_folder"
        ensureFolder(itemFolder)
        return itemFolder
    }

    suspend fun uploadReturnBundleJson(
        context: Context,
        barcode: String,
        metadata: Any,
        photos: List<Uri>
    ): YDUploadResult {
        val folder = ensureReturnItemFolder(barcode)

        val json = Gson().toJson(metadata).toByteArray(Charsets.UTF_8)
        uploadBytes("$folder/metadata_return.json", json)

        val publicLinks = coroutineScope {
            photos.mapIndexed { i, uri ->
                async(Dispatchers.IO) {
                    val bytes = loadAndCompressJpeg(context, uri)
                    if (bytes.isEmpty()) return@async null

                    val photoName = "${barcode}_${i + 1}.jpg"
                    val path = "$folder/$photoName"

                    uploadBytes(path, bytes)

                    runCatching {
                        api.publish(authHeader, path)
                        api.getResource(authHeader, path).publicUrl
                    }.getOrNull()
                }
            }.mapNotNull { it.await() }
        }

        return YDUploadResult(folder, publicLinks)
    }
}
