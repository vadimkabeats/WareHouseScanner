package com.example.warehousescanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun LinkEditScreen(
    barcode: String,
    initialLink: String,
    onSave: (String) -> Unit
) {
    var link by remember { mutableStateOf(initialLink) }
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Штрих-код: $barcode", style = MaterialTheme.typography.h6)
        Text("ссылки на товар нет в таблице", color = Color.Red)
        OutlinedTextField(
            value = link,
            onValueChange = { link = it },
            label = { Text("Ссылка на товар") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = { onSave(link) },
            enabled = link.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сохранить в базу")
        }
    }
}
