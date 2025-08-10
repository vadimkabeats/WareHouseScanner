package com.example.warehousescanner.data

import com.google.gson.annotations.SerializedName
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.*

data class UploadResponse(@SerializedName("href") val href: String)
data class ResourceResponse(@SerializedName("public_url") val publicUrl: String?)

interface YandexDiskApi {
    @GET("v1/disk/resources/upload")
    suspend fun getUploadLink(
        @Header("Authorization") auth: String,
        @Query("path") path: String,
        @Query("overwrite") overwrite: Boolean = true
    ): UploadResponse

    @PUT
    @Headers("Content-Type: application/octet-stream")
    suspend fun uploadFile(@Url href: String, @Body body: RequestBody): ResponseBody

    @PUT("v1/disk/resources")
    suspend fun createFolder(
        @Header("Authorization") auth: String,
        @Query("path") path: String
    )

    @PUT("v1/disk/resources/publish")
    suspend fun publish(
        @Header("Authorization") auth: String,
        @Query("path") path: String
    )

    @GET("v1/disk/resources")
    suspend fun getResource(
        @Header("Authorization") auth: String,
        @Query("path") path: String,
        @Query("fields") fields: String = "public_url"
    ): ResourceResponse
}
