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
                            val textContent = buildString {
                                appendLine("Штрих-код: $barcode")
                                appendLine("URL: $scanUrl")
                                appendLine("Статус: $statusText")
                                if (newLink.isNotBlank()) appendLine("Новая ссылка: $newLink")
                                if (checkResult.second.isNotBlank()) appendLine("Комментарий: ${checkResult.second}")
                                appendLine("Фото: ${photos.size}")
                                appendLine("Дефект: ${if (defectResult.first) "Да" else "Нет"}")
                                if (defectResult.first) appendLine("Описание дефекта: ${defectResult.second}")
                            }

                                                        val result = YandexDiskClient.uploadItemBundle(
                                context = context,
                                url = if (newLink.isNotBlank()) newLink else scanUrl,
                                barcode = barcode,
                                textContent = textContent,
                                photos = photos
                            )


                            val req = AfterUploadRequest(
                                barcode  = barcode,
                                baseLink = scanUrl,
                                status   = statusText,
                                newLink  = newLink,
                                qty      = photos.size,
                                defects  = if (defectResult.first) defectResult.second else "",
                                photos   = result.publicPhotoUrls.take(6)
                            )
                            GoogleSheetClient.saveAfterUpload(req)

                            message = "Отправлено. Папка: ${result.folder}"
                        } catch (e: Exception) {
                            message = "Ошибка: ${e.localizedMessage}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Отправить") }
        }

        if (message.isNotBlank()) Text(message, color = MaterialTheme.colors.primary)
    }
}
