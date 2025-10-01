package com.example.warehousescanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
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
    onReceiveReturn: () -> Unit,
    // НОВОЕ:
    torchOn: Boolean,
    onToggleTorch: (Boolean) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Главное меню", style = MaterialTheme.typography.h6)
        Spacer(Modifier.height(24.dp))

        // Переключатель "Темно" (фонарик при сканировании)
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
                    Text(
                        "При сканировании включать фонарик",
                        style = MaterialTheme.typography.caption
                    )
                }
                Switch(checked = torchOn, onCheckedChange = onToggleTorch)
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(onClick = onAddItem, modifier = Modifier.fillMaxWidth()) {
            Text("Идентифицировать товар")
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

        Button(onClick = onReceiveReturn, modifier = Modifier.fillMaxWidth()) {
            Text("Принять возврат")
        }
    }
}
