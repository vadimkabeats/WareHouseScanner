package com.example.warehousescanner.data

import com.google.gson.annotations.SerializedName
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.*

interface YandexDiskApi {

    @GET("v1/disk/resources/upload")
    suspend fun getUploadLink(
        @Header("Authorization") auth: String,
        @Query("path") path: String,
        @Query("overwrite") overwrite: Boolean = true
    ): UploadResponse

    @PUT
    @Headers("Content-Type: application/octet-stream")
    suspend fun uploadFile(
        @Url url: String,
        @Body body: RequestBody
    ): ResponseBody

    @PUT("v1/disk/resources/publish")
    suspend fun publishResource(
        @Header("Authorization") auth: String,
        @Query("path") path: String
    ): PublishResponse
}

data class UploadResponse(
    @SerializedName("href") val href: String
)

data class PublishResponse(
    @SerializedName("public_url") val publicUrl: String
)
