package com.example.warehousescanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onAddItem: () -> Unit,
    onPutAway: () -> Unit,
    onPrintLabel: () -> Unit,
    onReceiveReturn: () -> Unit,
    onReconcile: () -> Unit,
    // Статистика (НОВОЕ):
    statsDateLabel: String,
    statsNonNlo: Int?,
    statsNlo: Int?,
    statsLoading: Boolean,
    onPickDate: () -> Unit,
    // Фонарик
    torchOn: Boolean,
    onToggleTorch: (Boolean) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        // Верхний левый угол — карточка статистики
        Row(Modifier.fillMaxWidth()) {
            DailyStatsCard(
                title = "Проверено $statsDateLabel",
                nonNlo = statsNonNlo,
                nlo = statsNlo,
                loading = statsLoading,
                onPickDate = onPickDate
            )
            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))

        // Переключатель фонарика
        Card(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Режим «Темно»")
                    Spacer(Modifier.height(2.dp))
                    Text("При сканировании включать фонарик", style = MaterialTheme.typography.caption)
                }
                Switch(checked = torchOn, onCheckedChange = onToggleTorch)
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(onClick = onAddItem, modifier = Modifier.fillMaxWidth()) { Text("Идентифицировать товар") }
        Spacer(Modifier.height(12.dp))

        Button(onClick = onPutAway, modifier = Modifier.fillMaxWidth()) { Text("Положить товар") }
        Spacer(Modifier.height(12.dp))

        Button(onClick = onPrintLabel, modifier = Modifier.fillMaxWidth()) { Text("Печать этикетки") }
        Spacer(Modifier.height(12.dp))

        Button(onClick = onReceiveReturn, modifier = Modifier.fillMaxWidth()) { Text("Принять возврат") }
        Spacer(Modifier.height(12.dp))

        Button(onClick = onReconcile, modifier = Modifier.fillMaxWidth()) { Text("Сверка") }
    }
}

@Composable
private fun DailyStatsCard(
    title: String,
    nonNlo: Int?,
    nlo: Int?,
    loading: Boolean,
    onPickDate: () -> Unit
) {
    Card {
        Column(Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.subtitle1)
                TextButton(onClick = onPickDate) { Text("Сменить дату") }
            }
            if (loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }
            Text("не НЛО: ${nonNlo?.toString() ?: "—"}")
            Text("НЛО: ${nlo?.toString() ?: "—"}")
        }
    }
}
