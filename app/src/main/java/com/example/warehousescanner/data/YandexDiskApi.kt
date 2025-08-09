package com.example.warehousescanner.data

import com.google.gson.annotations.SerializedName
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.*

data class UploadResponse(
    @SerializedName("href") val href: String
)

interface YandexDiskApi {
    /** Запрашивает ссылку для загрузки файла по пути path */
    @GET("v1/disk/resources/upload")
    suspend fun getUploadLink(
        @Header("Authorization") auth: String,
        @Query("path") path: String,
        @Query("overwrite") overwrite: Boolean = true
    ): UploadResponse

    /** Загружает байты по предварительно полученному href */
    @PUT
    @Headers("Content-Type: application/octet-stream")
    suspend fun uploadFile(
        @Url href: String,
        @Body body: RequestBody
    ): ResponseBody

    /** Создаёт папку по пути path (если уже существует — бросает исключение) */
    @PUT("v1/disk/resources")
    suspend fun createFolder(
        @Header("Authorization") auth: String,
        @Query("path") path: String
    )
}
