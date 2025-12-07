package com.example.warehousescanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.warehousescanner.data.ReconcileItem

@Composable
fun ReconcileHomeScreen(
    isLoading: Boolean,
    error: String?,
    expected: List<ReconcileItem>,
    scannedCount: Int,
    onScan: () -> Unit,
    onBrowse: () -> Unit,
    onViewPassed: () -> Unit,
    onReload: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Сверка с курьером", style = MaterialTheme.typography.h6)
        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        if (!error.isNullOrBlank()) {
            Text("Ошибка: $error", color = MaterialTheme.colors.error)
            OutlinedButton(onClick = onReload) { Text("Повторить загрузку") }
        }
        val total = expected.size
        Text("Всего к сдаче: $total")
        Text("Просканировано: $scannedCount")
        Text("Осталось: ${total - scannedCount}")
        Spacer(Modifier.height(8.dp))

        Button(onClick = onScan, modifier = Modifier.fillMaxWidth(), enabled = total > 0 && !isLoading) {
            Text("Сканировать товар")
        }
        OutlinedButton(
            onClick = onBrowse,
            modifier = Modifier.fillMaxWidth(),
            enabled = total > 0
        ) { Text("Просмотреть товары") }
        OutlinedButton(onClick = onViewPassed, modifier = Modifier.fillMaxWidth(), enabled = scannedCount > 0) {
            Text("Посмотреть прошедшие")
        }
    }
}