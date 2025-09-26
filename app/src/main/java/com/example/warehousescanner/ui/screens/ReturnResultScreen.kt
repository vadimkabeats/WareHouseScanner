package com.example.warehousescanner.ui.screens

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.warehousescanner.data.GoogleSheetClient
import com.example.warehousescanner.data.YandexDiskClient
import kotlinx.coroutines.launch

@Composable
fun ReturnResultScreen(
    context: Context,
    dispatchNumber: String,
    printBarcode: String,
    hasDefect: Boolean,
    defectDesc: String,
    photos: List<Uri>,
    userFullName: String,
    oauthToken: String,
    onNextToPrint: () -> Unit,
    onBackHome: () -> Unit
) {
    LaunchedEffect(oauthToken) { YandexDiskClient.init(oauthToken) }

    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var sentOk by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Отправка данных по возврату", style = MaterialTheme.typography.h6)
        Text("Dispatch №: $dispatchNumber")
        Text("ШК (печать): $printBarcode")
        Text("Дефект: ${if (hasDefect) "Есть" else "Нет"}")
        if (hasDefect && defectDesc.isNotBlank()) Text("Описание: $defectDesc")
        Text("Фото: ${photos.size} шт.")

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
                            // 1) Грузим в Я.Диск -> Возвраты/yyyy-MM-dd/<barcode>_folder
                            val meta = mapOf(
                                "dispatchNumber" to dispatchNumber,
                                "barcode" to printBarcode,
                                "hasDefect" to hasDefect,
                                "defectDesc" to (if (hasDefect) defectDesc else ""),
                                "photosCount" to photos.size,
                                "user" to userFullName
                            )
                            val yd = com.example.warehousescanner.data.YandexDiskClient.uploadReturnBundleJson(
                                context = context,
                                barcode = printBarcode,
                                metadata = meta,
                                photos = photos
                            )
                            val publicUrls = yd.publicPhotoUrls.take(6)

                            // 2) Пишем в лист «Возвраты»: описание дефекта + ссылки на фото (по dispatchNumber)
                            val res = GoogleSheetClient.saveReturn(
                                user = userFullName,
                                dispatchNumber = dispatchNumber,
                                barcode = printBarcode,
                                defectDesc = if (hasDefect) defectDesc else "",
                                photoLinks = publicUrls
                            )
                            if (res.ok == true) {
                                sentOk = true
                                message = "Сохранено: ${yd.folder}"
                            } else {
                                message = "Не удалось записать в таблицу: ${res.error ?: "ошибка"}"
                                sentOk = false
                            }
                        } catch (e: Exception) {
                            message = "Ошибка: ${e.localizedMessage}"
                            sentOk = false
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !sentOk && printBarcode.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (sentOk) "Уже отправлено" else "Отправить") }

            if (sentOk) {
                Spacer(Modifier.height(8.dp))
                Button(onClick = onNextToPrint, modifier = Modifier.fillMaxWidth()) {
                    Text("Перейти к печати этикетки")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onBackHome, modifier = Modifier.fillMaxWidth()) {
                    Text("В главное меню")
                }
            }
        }

        if (message.isNotBlank()) Text(message, color = MaterialTheme.colors.primary)
    }
}
