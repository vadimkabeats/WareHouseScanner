package com.example.warehousescanner.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@Composable
fun CheckScreen(
    url: String,
    onAction: (status: String, comment: String, newLink: String) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var status by remember { mutableStateOf<String?>(null) }
    var comment by remember { mutableStateOf("") }
    var newLink by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Проверьте товар", style = MaterialTheme.typography.h6)

        if (url.isNotBlank()) {
            Text(
                text = url,
                color = MaterialTheme.colors.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { runCatching { uriHandler.openUri(url) } }
            )
        } else {
            Text("Ссылка отсутствует")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = status == "match", onClick = { status = "match"; newLink = "" })
            Text("Совпадает", modifier = Modifier.padding(end = 24.dp))
            RadioButton(selected = status == "mismatch", onClick = { status = "mismatch" })
            Text("Не совпадает")
        }

        if (status == "mismatch") {
            OutlinedTextField(
                value = newLink,
                onValueChange = { newLink = it },
                label = { Text("Новая ссылка на товар") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Комментарий") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = { onAction(status!!, comment, if (status == "mismatch") newLink else "") },
            enabled = status != null && (status == "match" || newLink.isNotBlank()),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Далее") }
    }
}
