package com.example.warehousescanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun DefectScreen(
    // ДОБАВИЛИ strongPackaging в коллбек
    onDone: (hasDefect: Boolean, desc: String, quantity: Int, strongPackaging: Boolean) -> Unit
) {
    var hasDefect by rememberSaveable { mutableStateOf(false) }
    var defectDesc by rememberSaveable { mutableStateOf("") }

    var qtyText by rememberSaveable { mutableStateOf("1") }

    // НОВОЕ: обязательный выбор "усиленной упаковки"
    // null — не выбран, true — да, false — нет
    var strongPackaging by rememberSaveable { mutableStateOf<Boolean?>(null) }

    fun qtyInt(): Int = qtyText.toIntOrNull()?.coerceAtLeast(1) ?: 1

    // Кнопка "Далее" активна только если выбрана упаковка
    val canContinue = qtyText.isNotBlank() && strongPackaging != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Сведения о состоянии", style = MaterialTheme.typography.h6)

        Row(verticalAlignment = Alignment.CenterVertically) {
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
                if (txt.isEmpty() || txt.all { it.isDigit() }) {
                    qtyText = txt
                }
            },
            label = { Text("Количество товаров") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // ---------- НОВЫЙ БЛОК: усиленная упаковка ----------
        Divider()
        Text("Нужна усиленная упаковка?", style = MaterialTheme.typography.subtitle1)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = strongPackaging == true,
                    onClick = { strongPackaging = true }
                )
                Text("Да")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = strongPackaging == false,
                    onClick = { strongPackaging = false }
                )
                Text("Нет")
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                val pack = strongPackaging ?: return@Button
                onDone(hasDefect, defectDesc, qtyInt(), pack)
            },
            enabled = canContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Далее")
        }
    }
}
