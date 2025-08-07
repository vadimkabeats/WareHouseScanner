package com.example.warehousescanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DefectScreen(
    onNext: (hasDefect: Boolean, defectDescription: String) -> Unit
) {
    var hasDefect by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Есть дефект?", style = MaterialTheme.typography.h6)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = !hasDefect,
                onClick = { hasDefect = false }
            )
            Text("Нет", modifier = Modifier.padding(end = 16.dp))
            RadioButton(
                selected = hasDefect,
                onClick = { hasDefect = true }
            )
            Text("Да")
        }
        if (hasDefect) {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Описание дефекта") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = { onNext(hasDefect, description) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Далее")
        }
    }
}
