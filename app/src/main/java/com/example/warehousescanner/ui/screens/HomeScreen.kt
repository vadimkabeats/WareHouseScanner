package com.example.warehousescanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onAddItem: () -> Unit,
    onPutAway: () -> Unit,
    onPrintLabel: () -> Unit,
    onReceiveReturn: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Главное меню", style = MaterialTheme.typography.h6)
        Spacer(Modifier.height(24.dp))

        Button(onClick = onAddItem, modifier = Modifier.fillMaxWidth()) {
            Text("Добавить товар")
        }
        Spacer(Modifier.height(12.dp))

        Button(onClick = onPutAway, modifier = Modifier.fillMaxWidth()) {
            Text("Положить товар")
        }
        Spacer(Modifier.height(12.dp))

        Button(onClick = onPrintLabel, modifier = Modifier.fillMaxWidth()) {
            Text("Печать этикетки")
        }
        Spacer(Modifier.height(12.dp))

        // НОВАЯ КНОПКА
        Button(onClick = onReceiveReturn, modifier = Modifier.fillMaxWidth()) {
            Text("Принять возврат")
        }
    }
}
