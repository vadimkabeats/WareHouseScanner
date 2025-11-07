package com.example.warehousescanner.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@Composable
fun ReturnConditionScreen(
    dispatchNumber: String,
    printBarcode: String,
    reason: String,
    productUrl: String,
    hasDefectInit: Boolean,
    defectDescInit: String,
    photosCount: Int,
    decisionInit: String,                           // НОВОЕ
    onChangeState: (Boolean, String) -> Unit,
    onSelectDecision: (String) -> Unit,             // НОВОЕ
    onOpenPhotos: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var hasDefect by remember { mutableStateOf(hasDefectInit) }
    var defectDesc by remember { mutableStateOf(defectDescInit) }

    val options = listOf("перевыложить без изменений", "изменить и перевыложить", "списать")
    var decision by remember { mutableStateOf(decisionInit) }

    fun normalizeUrl(u: String): String =
        if (u.startsWith("http://", true) || u.startsWith("https://", true)) u else "https://$u"

    val canProceed = printBarcode.isNotBlank() &&
            decision.isNotBlank() &&
            (!hasDefect || photosCount > 0)

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Принять возврат", style = MaterialTheme.typography.h6)
        Text("Dispatch №: ${dispatchNumber.ifBlank { "—" }}")
        Text("ШК для печати: ${printBarcode.ifBlank { "—" }}")

        if (productUrl.isNotBlank()) {
            Divider()
            Text("Ссылка на товар", style = MaterialTheme.typography.subtitle2)
            Text(
                text = productUrl,
                color = MaterialTheme.colors.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable {
                    runCatching { uriHandler.openUri(normalizeUrl(productUrl.trim())) }
                }
            )
        }

        if (reason.isNotBlank()) {
            Divider()
            Text("Причина возврата", style = MaterialTheme.typography.subtitle2)
            Text(reason)
        }

        Divider()
        Text("Состояние товара", style = MaterialTheme.typography.subtitle1)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                hasDefect = false
                defectDesc = ""
                onChangeState(false, "")
            }) { Text("Нет дефектов") }

            OutlinedButton(onClick = {
                hasDefect = true
                onChangeState(true, defectDesc)
            }) { Text("Есть дефекты") }
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

            OutlinedButton(
                onClick = onOpenPhotos,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Фото (${photosCount}/6)") }

            if (photosCount == 0) {
                Text(
                    "Добавьте минимум 1 фото при наличии дефектов",
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption
                )
            }
        }

        Divider()
        Text("Что делаем с возвратом", style = MaterialTheme.typography.subtitle1)
        options.forEach { opt ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                RadioButton(
                    selected = decision == opt,
                    onClick = {
                        decision = opt
                        onSelectDecision(opt)
                    }
                )
                Text(opt)
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
                enabled = canProceed
            ) { Text("Далее") }
        }
    }
}
