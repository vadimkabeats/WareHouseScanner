package com.example.warehousescanner.ui.screens

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.warehousescanner.data.*
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun ResultScreen(
    context: Context,
    barcode: String,
    scanUrl: String,
    checkResult: Triple<String, String, String>, // status, comment, newLink
    photos: List<Uri>,
    defectResult: Pair<Boolean, String>,
    quantity: Int,
    userFullName: String,
    scanStartMs: Long,
    oauthToken: String
) {
    LaunchedEffect(oauthToken) { YandexDiskClient.init(oauthToken) }

    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    val statusText = when (checkResult.first) {
        "match"    -> "Совпадает"
        "mismatch" -> "Не совпадает"
        else       -> checkResult.first
    }
    val newLink = checkResult.third

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Обзор данных", style = MaterialTheme.typography.h6)
        Text("Штрих-код: $barcode")
        Text("URL: $scanUrl")
        Text("Статус проверки: $statusText")
        if (newLink.isNotBlank()) Text("Новая ссылка: $newLink")
        if (checkResult.second.isNotBlank()) Text("Комментарий: ${checkResult.second}")
        Text("Количество: $quantity")
        Text("Фото: ${photos.size} шт.")
        Text("Дефект: ${if (defectResult.first) "Есть" else "Нет"}")
        if (defectResult.first) Text("Описание дефекта: ${defectResult.second}")

        Spacer(Modifier.weight(1f))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        message = ""
                        try {
                            val nowMs = System.currentTimeMillis()
                            val durationSec = max(0, ((nowMs - scanStartMs) / 1000.0).roundToInt())

                            val meta = mapOf(
                                "barcode" to barcode,
                                "url" to scanUrl,
                                "status" to statusText,
                                "newLink" to newLink,
                                "comment" to checkResult.second,
                                "quantity" to quantity,
                                "photosCount" to photos.size,
                                "defect" to defectResult.first,
                                "defectDescription" to defectResult.second,
                                "user" to userFullName,
                                "durationSec" to durationSec,
                                "photoFiles" to (1..photos.size).map { i -> "${barcode}_$i.jpg" }
                            )

                            val yd = YandexDiskClient.uploadItemBundleJson(
                                context = context,
                                barcode = barcode,
                                metadata = meta,
                                photos = photos
                            )

                            val req = AfterUploadRequest(
                                user      = userFullName,
                                barcode   = barcode,
                                baseLink  = scanUrl,
                                status    = statusText,
                                newLink   = newLink,
                                qty       = quantity,
                                durationSec = durationSec,
                                defects   = if (defectResult.first) defectResult.second else "",
                                photos    = yd.publicPhotoUrls.take(6)
                            )
                            GoogleSheetClient.saveAfterUpload(req)

                            message = "Отправлено: ${yd.folder}"
                        } catch (e: Exception) {
                            message = "Ошибка: ${e.localizedMessage}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Отправить на Яндекс.Диск") }
        }

        if (message.isNotBlank()) Text(message, color = MaterialTheme.colors.primary)
    }
}
