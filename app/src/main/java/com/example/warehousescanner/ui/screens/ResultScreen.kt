package com.example.warehousescanner.ui.screens

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.warehousescanner.data.YandexDiskClient
import kotlinx.coroutines.launch

@Composable
fun ResultScreen(
    context: Context,
    barcode: String,
    scanUrl: String,
    checkResult: Pair<String, String>,
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

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Обзор данных", style = MaterialTheme.typography.h6)
        Text("Штрих-код: $barcode")
        Text("URL: $scanUrl")
        Text("Статус проверки: $statusText")
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
                            val folder = YandexDiskClient.uploadImagesDated(
                                photos = photos,
                                context = context,
                                barcode = barcode
                            )

                            val safeName = scanUrl
                                .replace(Regex("[^A-Za-z0-9]"), "_")
                                .let { if (it.isEmpty()) "file" else it }
                                .take(100) + ".txt"

                            val textContent = buildString {
                                appendLine("Штрих-код: $barcode")
                                appendLine("URL: $scanUrl")
                                appendLine("Статус: $statusText")
                                if (checkResult.second.isNotBlank())
                                    appendLine("Комментарий: ${checkResult.second}")
                                appendLine("Фото: ${photos.size}")
                                appendLine("Дефект: ${if (defectResult.first) "Да" else "Нет"}")
                                if (defectResult.first)
                                    appendLine("Описание дефекта: ${defectResult.second}")
                            }
                            YandexDiskClient.uploadTextFile("$folder/$safeName", textContent)

                            message = "Отправлено на Яндекс.Диск: $folder"
                        } catch (e: Exception) {
                            message = "Ошибка: ${e.localizedMessage}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Отправить на Яндекс.Диск")
            }
        }

        if (message.isNotBlank()) {
            Text(message, color = MaterialTheme.colors.primary)
        }
    }
}
