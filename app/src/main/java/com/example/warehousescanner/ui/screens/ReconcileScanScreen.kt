package com.example.warehousescanner.ui.screens

import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.warehousescanner.viewmodel.ReconcileViewModel

@Composable
fun ReconcileScanScreen(
    vm: ReconcileViewModel,
    torchOn: Boolean,
    onNextItem: () -> Unit,
    onFinish: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showChoice by remember { mutableStateOf<String?>(null) }
    var scanKey by remember { mutableStateOf(0) }
    var resetting by remember { mutableStateOf(false) }

    if (!resetting) {
        ScanScreen(
            instanceKey = "reconcile_$scanKey",
            allowManualInput = true,
            inputHint = "Сканируйте трек-номер",
            validator = { null },
            torchOn = torchOn,
            manualAllowSpaces = true
        ) { scanned ->
            vm.addScannedTrack(scanned)
            showChoice = scanned
        }
    } else {
        Box(Modifier.fillMaxSize()) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }

    if (showChoice != null) {
        AlertDialog(
            onDismissRequest = { showChoice = null },
            title = { Text("Добавлено к сверке") },
            text  = { Text(showChoice!!) },
            confirmButton = {
                Button(onClick = {
                    showChoice = null
                    scope.launch {
                        resetting = true
                        delay(350)
                        scanKey++
                        resetting = false
                        onNextItem()
                    }
                }) { Text("Следующий товар") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showChoice = null
                    onFinish()
                }) { Text("Завершить сверку") }
            }
        )
    }
}
