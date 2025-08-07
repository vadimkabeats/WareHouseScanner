package com.example.warehousescanner.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@Composable
fun CheckScreen(
    url: String,
    onAction: (status: String, comment: String) -> Unit
) {
    var comment by remember { mutableStateOf("") }
    var awaitingComment by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Проверьте ссылку:")
        Text(
            text = url,
            style = MaterialTheme.typography.body1.copy(
                color = MaterialTheme.colors.primary,
                textDecoration = TextDecoration.Underline
            ),
            modifier = Modifier
                .clickable { uriHandler.openUri(url) }
                .padding(4.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onAction("match", "") }) {
                Text("Совпадает")
            }
            Button(onClick = { awaitingComment = true }) {
                Text("Не совпадает")
            }
        }
        if (awaitingComment) {
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Комментарий") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onAction("mismatch", comment) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Отправить")
            }
        }
    }
}
