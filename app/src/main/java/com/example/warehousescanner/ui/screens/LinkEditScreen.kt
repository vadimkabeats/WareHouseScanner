package com.example.warehousescanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun LinkEditScreen(
    barcode: String,
    initialLink: String,
    onSave: (String) -> Unit
) {
    var rawInput by remember { mutableStateOf(initialLink) }
    val extracted = remember(rawInput) { extractAndNormalizeUrl(rawInput) }
    val isValid = extracted != null

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Ссылка не найдена в таблице", color = MaterialTheme.colors.error)
        if (barcode.isNotBlank()) {
            Text(
                "Штрих-код: $barcode",
                color = MaterialTheme.colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        OutlinedTextField(
            value = rawInput,
            onValueChange = { txt ->

                val url = extractAndNormalizeUrl(txt)

                rawInput = when {
                    url != null && (txt.contains('\n') || txt.contains('\r') || txt.contains(' ') || txt != url) -> url
                    else -> txt
                }
            },
            label = { Text("Вставьте ссылку на товар") },
            placeholder = { Text("https://ozon.ru/t/…") },
            isError = rawInput.isNotBlank() && !isValid,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth()
        )

        if (rawInput.isNotBlank() && !isValid) {
            Text(
                "Нет ссылки на товар. Добавьте её",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.error
            )
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { onSave(extracted!!) },
            enabled = isValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Продолжить")
        }
    }
}

private fun extractAndNormalizeUrl(text: String): String? {
    val t = text.trim()

    val urlInText = Regex("""(?i)\b(https?://[^\s]+|www\.[^\s]+)\b""")
    urlInText.find(t)?.value?.let { return normalizeScheme(it) }

    val bareUrl = Regex("""^(?i)([A-Za-z0-9-]+\.)+[A-Za-z]{2,}(/[^\s]*)?$""")
    if (bareUrl.matches(t)) return "https://$t"

    return null
}

private fun normalizeScheme(u: String): String =
    if (u.startsWith("http://", true) || u.startsWith("https://", true)) u
    else "https://$u"