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
    scanUrl: String,
    checkResult: Pair<String,String>,
    photos: List<Uri>,
    defectResult: Pair<Boolean,String>,
    oauthToken: String
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(oauthToken) {
        YandexDiskClient.init(oauthToken)
    }

    // Человекочитаемый статус
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
        Text("URL товара: $scanUrl")
        Text("Статус проверки: $statusText")
        checkResult.second.takeIf { it.isNotBlank() }?.let { Text("Комментарий: $it") }
        Text("Фото: ${photos.size} шт.")
        Text("Дефект: ${if (defectResult.first) "Есть" else "Нет"}")
        defectResult.second.takeIf { it.isNotBlank() }?.let { Text("Описание дефекта: $it") }

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
                            // Создаём папку по текущему времени
                            val folder = "Warehouse/${System.currentTimeMillis()}"

                            // 1) Загружаем и публикуем фото — получаем публичные ссылки
                            val photoUrls = YandexDiskClient
                                .uploadAndPublishImages(folder, photos, context)

                            // 2) Формируем единую структуру метаданных
                            val metadata = mapOf(
                                "url"               to scanUrl,
                                "status"            to statusText,
                                "comment"           to checkResult.second,
                                "photos"            to photoUrls,
                                "hasDefect"         to defectResult.first,
                                "defectDescription" to defectResult.second
                            )

                            // 3) Загружаем JSON с метаданными
                            YandexDiskClient.uploadMetadata("$folder/metadata.json", metadata)

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
                Text("Отправить на Яндекс.Диск")
            }
        }

        if (message.isNotBlank()) {
            Text(message, color = if (message.startsWith("Ошибка")) MaterialTheme.colors.error else MaterialTheme.colors.primary)
        }
    }
}
