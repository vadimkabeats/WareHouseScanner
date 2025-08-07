package com.example.warehousescanner.ui.screens

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.warehousescanner.data.YandexDiskClient
import kotlinx.coroutines.launch

@Composable
fun ResultScreen(
    context: Context,
    scanUrl: String,
    checkResult: Pair<String, String>,
    photos: List<Uri>,
    defectResult: Pair<Boolean, String>,
    oauthToken: String
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(oauthToken) {
        YandexDiskClient.init(oauthToken)
    }

    // Преобразуем статус в человекочитаемую строку
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
        Text("URL: $scanUrl")
        Text("Статус проверки: $statusText")
        if (checkResult.second.isNotBlank()) {
            Text("Комментарий: ${checkResult.second}")
        }
        Text("Фото: ${photos.size} шт.")
        Text("Дефект: ${if (defectResult.first) "Есть" else "Нет"}")
        if (defectResult.first) {
            Text("Описание дефекта: ${defectResult.second}")
        }

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
                            val folder = "Warehouse/${System.currentTimeMillis()}"
                            val metadata = mapOf(
                                "url"               to scanUrl,
                                "status"            to statusText,
                                "comment"           to checkResult.second,
                                "hasDefect"         to defectResult.first,
                                "defectDescription" to defectResult.second
                            )
                            YandexDiskClient.uploadMetadata("$folder/metadata.json", metadata)
                            YandexDiskClient.uploadImages(folder, photos, context)
                            message = "Данные успешно отправлены"
                        } catch (e: Exception) {
                            message = "Ошибка: ${e.localizedMessage}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Отправить")
            }
        }

        if (message.isNotBlank()) {
            Text(message)
        }
    }
}
