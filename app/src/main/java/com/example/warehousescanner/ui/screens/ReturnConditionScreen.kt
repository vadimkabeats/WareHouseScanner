package com.example.warehousescanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReturnConditionScreen(
    dispatchNumber: String,
    printBarcode: String,
    hasDefectInit: Boolean,
    defectDescInit: String,
    photosCount: Int,
    onChangeState: (Boolean, String) -> Unit,
    onOpenPhotos: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var hasDefect by remember { mutableStateOf(hasDefectInit) }
    var defectDesc by remember { mutableStateOf(defectDescInit) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Принять возврат", style = MaterialTheme.typography.h6)
        Text("Dispatch №: ${dispatchNumber.ifBlank { "—" }}")
        Text("ШК для печати: ${printBarcode.ifBlank { "—" }}")

        Divider()

        Text("Состояние товара", style = MaterialTheme.typography.subtitle1)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    hasDefect = false
                    defectDesc = ""
                    onChangeState(false, "")
                }
            ) { Text("Нет дефектов") }

            OutlinedButton(
                onClick = {
                    hasDefect = true
                    onChangeState(true, defectDesc)
                }
            ) { Text("Есть дефекты") }
        }

        if (hasDefect) {
            OutlinedTextField(
                value = defectDesc,
                onValueChange = {
                    defectDesc = it
                    onChangeState(true, it)
                },
                label = { Text("Описание дефекта") },
                modifier = Modifier.fillMaxWidth()
            )

            // Кнопка "Фото" доступна и видна ТОЛЬКО при наличии дефекта
            OutlinedButton(
                onClick = onOpenPhotos,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Фото (${photosCount}/6)")
            }
        }

        Spacer(Modifier.weight(1f))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Назад") }
            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f),
                enabled = printBarcode.isNotBlank() // не даём идти дальше, если не нашли barcode
            ) { Text("Далее") }
        }
    }
}
