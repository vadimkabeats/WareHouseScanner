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

@Composable
fun ResultScreen(
    context: Context,
    barcode: String,
    scanUrl: String,
    checkResult: Triple<String, String, String>,
    photos: List<Uri>,
    defectResult: Pair<Boolean, String>,
    quantity: Int,
    userFullName: String,
    scanStartMs: Long,
    oauthToken: String,
    onBackHome: () -> Unit
) {
    LaunchedEffect(oauthToken) { YandexDiskClient.init(oauthToken) }

    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var sentOk by remember { mutableStateOf(false) }

    val (rawStatus, rawComment, rawNewLink) = checkResult
    val isNlo = rawStatus == "nlo"

    val statusText = when (rawStatus) {
        "match" -> "Совпадает"
        "nlo" -> "НЛО"
        "mismatch_other" -> "Не совпадает (другой товар)"
        "mismatch_nobarcode" -> "Не совпадает (товар без шк)"
        "mismatch" -> "Не совпадает"
        else -> rawStatus
    }


    val photosForUpload = if (isNlo) emptyList() else photos
    val qtyForSheet     = if (isNlo) 0 else quantity
    val defectsForSheet = if (isNlo) "" else if (defectResult.first) defectResult.second else ""
    val baseLinkForSheet= if (isNlo) "" else scanUrl
    val newLinkForSheet = if (isNlo) "" else rawNewLink

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Обзор данных", style = MaterialTheme.typography.h6)
        Text("Штрих-код: $barcode")
        Text("URL: ${baseLinkForSheet.ifBlank { "—" }}")
        Text("Статус проверки: $statusText")
        if (newLinkForSheet.isNotBlank()) Text("Новая ссылка: $newLinkForSheet")
        if (!isNlo && rawComment.isNotBlank()) Text("Комментарий: $rawComment")
        Text("Количество: ${if (isNlo) 0 else qtyForSheet}")
        Text("Фото: ${photosForUpload.size} шт.")
        if (!isNlo) {
            Text("Дефект: ${if (defectResult.first) "Есть" else "Нет"}")
            if (defectResult.first && defectsForSheet.isNotBlank()) Text("Описание дефекта: $defectsForSheet")
        }

        Spacer(Modifier.weight(1f))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            val actionTitle = if (isNlo) {
                if (sentOk) "Уже сохранено" else "Сохранить в таблицу"
            } else {
                if (sentOk) "Уже отправлено" else "Отправить на Яндекс.Диск"
            }

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        message = ""
                        try {
                            val nowMs = System.currentTimeMillis()
                            val durationSec = kotlin.math.max(0, (((nowMs - scanStartMs) / 1000.0)).toInt())

                            val publicUrls: List<String> = if (!isNlo && photosForUpload.isNotEmpty()) {

                                val meta = mapOf(
                                    "barcode" to barcode,
                                    "url" to baseLinkForSheet,
                                    "status" to statusText,
                                    "newLink" to newLinkForSheet,
                                    "comment" to rawComment,
                                    "quantity" to qtyForSheet,
                                    "photosCount" to photosForUpload.size,
                                    "defect" to defectResult.first,
                                    "defectDescription" to defectsForSheet,
                                    "user" to userFullName,
                                    "photoFiles" to (1..photosForUpload.size).map { i -> "${barcode}_$i.jpg" }
                                )
                                val yd = YandexDiskClient.uploadItemBundleJson(
                                    context = context,
                                    barcode = barcode,
                                    metadata = meta,
                                    photos = photosForUpload
                                )
                                message = "Отправлено: ${yd.folder}"
                                yd.publicPhotoUrls.take(6)
                            } else {

                                emptyList()
                            }

                            val req = AfterUploadRequest(
                                user        = userFullName,
                                barcode     = barcode,
                                baseLink    = baseLinkForSheet,
                                status      = statusText,
                                newLink     = newLinkForSheet,
                                qty         = qtyForSheet,
                                durationSec = durationSec,
                                defects     = defectsForSheet,
                                photos      = publicUrls
                            )
                            GoogleSheetClient.saveAfterUpload(req)

                            if (isNlo) {
                                message = "Сохранено в таблицу (НЛО)"
                            }
                            sentOk = true
                        } catch (e: Exception) {
                            message = "Ошибка: ${e.localizedMessage}"
                            sentOk = false
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !sentOk,
                modifier = Modifier.fillMaxWidth()
            ) { Text(actionTitle) }

            if (sentOk) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onBackHome,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Вернуться в главное меню") }
            }
        }

        if (message.isNotBlank()) Text(message, color = MaterialTheme.colors.primary)
    }
}
