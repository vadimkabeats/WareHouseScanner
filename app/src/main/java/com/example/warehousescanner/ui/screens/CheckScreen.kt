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
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("URL: $url", style = MaterialTheme.typography.subtitle1)
        Spacer(Modifier.height(12.dp))
        Row {
            Button(onClick = { onAction("match", comment) }) { Text("Совпадает") }
            Spacer(Modifier.width(12.dp))
            Button(onClick = { onAction("mismatch", comment) }) { Text("Не совпадает") }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = comment, onValueChange = { comment = it },
            label = { Text("Комментарий") }, modifier = Modifier.fillMaxWidth()
        )
    }
}

