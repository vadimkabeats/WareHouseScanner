package com.example.warehousescanner.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.warehousescanner.data.ReconcileItem

// ui/screens/ReconcileDoneScreen.kt
@Composable
fun ReconcileDoneScreen(
    passed: List<ReconcileItem>,     // ← отсканировано И с галочкой
    notPassed: List<ReconcileItem>,  // ← отсканировано И без галочки
    onBackHome: () -> Unit
) {
    var tab by remember { mutableStateOf(0) }
    val expanded = remember { mutableStateMapOf<String, Boolean>() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Результаты сверки", style = MaterialTheme.typography.h6)
        Spacer(Modifier.height(8.dp))

        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }) { Text("Передано (${passed.size})") }
            Tab(selected = tab == 1, onClick = { tab = 1 }) { Text("Не передано (${notPassed.size})") }
        }

        Spacer(Modifier.height(8.dp))

        // 2-колоночная шапка: Трек-номер | Название
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            Text("Трек-номер", modifier = Modifier.weight(1.6f))
            Text("Название",   modifier = Modifier.weight(2.4f))
        }
        Divider()

        val data = if (tab == 0) passed else notPassed

        LazyColumn(Modifier.weight(1f)) {
            items(data) { item ->
                ReconcileRow2Col(
                    item = item,
                    expanded = expanded[item.track] == true,
                    onToggle = { expanded[item.track] = !(expanded[item.track] ?: false) }
                )
                Divider()
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBackHome, modifier = Modifier.fillMaxWidth()) {
            Text("В главное меню")
        }
    }
}


@Composable
private fun ReconcileRow2Col(
    item: ReconcileItem,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(
                text = item.track.ifBlank { "—" },
                modifier = Modifier.weight(1.6f).clickable { onToggle() },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colors.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = item.name.ifBlank { "—" },
                modifier = Modifier.weight(2.4f).clickable { onToggle() },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colors.primary
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(Modifier.fillMaxWidth().padding(top = 6.dp, start = 4.dp, end = 4.dp)) {
                DetailRow(label = "ШК товара",  value = item.barcode.ifBlank { "—" })
                DetailRow(label = "Служба",     value = item.carrier.ifBlank { "—" })
                LinkRow(url = item.url)  // ← НОВОЕ
            }
        }
    }
}

@Composable
fun LinkRow(url: String) {
    if (url.isBlank()) return
    val handler = LocalUriHandler.current
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("Ссылка: ", style = MaterialTheme.typography.caption)
        Text(
            text = url,
            color = MaterialTheme.colors.primary,
            textDecoration = TextDecoration.Underline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable { runCatching { handler.openUri(url) } }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("$label: ", style = MaterialTheme.typography.caption)
        Text(value, style = MaterialTheme.typography.body2)
    }
}