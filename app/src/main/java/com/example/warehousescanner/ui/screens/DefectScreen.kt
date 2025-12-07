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
    onDone: (
        hasDefect: Boolean,
        desc: String,
        quantity: Int,
        strongPackaging: Boolean,
        toUtil: Boolean
    ) -> Unit
) {
    var hasDefect by rememberSaveable { mutableStateOf(false) }
    var defectDesc by rememberSaveable { mutableStateOf("") }
    var qtyText by rememberSaveable { mutableStateOf("1") }
    var strongPackaging by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var toUtil by rememberSaveable { mutableStateOf(false) }
    fun qtyInt(): Int = qtyText.toIntOrNull()?.coerceAtLeast(1) ?: 1
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

        Divider()
        Text("В утиль", style = MaterialTheme.typography.subtitle1)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = !toUtil,
                    onClick = { toUtil = false } // НЕТ
                )
                Text("Нет")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = toUtil,
                    onClick = { toUtil = true } // ДА
                )
                Text("Да")
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                val pack = strongPackaging ?: return@Button
                onDone(hasDefect, defectDesc, qtyInt(), pack, toUtil)
            },
            enabled = canContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Далее")
        }
    }
}
