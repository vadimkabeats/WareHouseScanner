package com.example.warehousescanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun DefectScreen(
    onDone: (hasDefect: Boolean, desc: String, quantity: Int) -> Unit
) {
    var hasDefect by rememberSaveable { mutableStateOf(false) }
    var defectDesc by rememberSaveable { mutableStateOf("") }
    var qtyText by rememberSaveable { mutableStateOf("1") }

    fun qtyInt(): Int = qtyText.toIntOrNull()?.coerceAtLeast(1) ?: 1

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Сведения о состоянии", style = MaterialTheme.typography.h6)

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(checked = hasDefect, onCheckedChange = { hasDefect = it })
            Spacer(Modifier.width(8.dp))
            Text("Есть дефект")
        }

        if (hasDefect) {
            OutlinedTextField(
                value = defectDesc,
                onValueChange = { defectDesc = it },
                label = { Text("Описание дефекта") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        OutlinedTextField(
            value = qtyText,
            onValueChange = { txt ->
                // Разрешаем только цифры
                if (txt.all { it.isDigit() }) qtyText = txt
                if (qtyText.isBlank()) qtyText = "1"
            },
            label = { Text("Количество") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { onDone(hasDefect, defectDesc, qtyInt()) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Далее") }
    }
}
