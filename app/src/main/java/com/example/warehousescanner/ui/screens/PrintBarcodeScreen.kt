package com.example.warehousescanner.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.warehousescanner.printer.LabelPrinter
import kotlinx.coroutines.launch

@Composable
fun PrintBarcodeScreen(
    initialBarcode: String = "",
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var barcode by rememberSaveable { mutableStateOf(initialBarcode) }
    var printer by remember { mutableStateOf(LabelPrinter.restoreLastPrinter(ctx)) }
    var showPicker by remember { mutableStateOf(printer == null) }
    var isPrinting by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    val requestBt = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    val canPrint = barcode.isNotBlank() &&
            printer != null &&
            !isPrinting

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text("Печать ШК", style = MaterialTheme.typography.h6)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = barcode,
            onValueChange = { barcode = it },
            label = { Text("ШК товара") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (printer == null) {
                Button(onClick = {
                    if (Build.VERSION.SDK_INT >= 31 && !hasBtConnectPermissionBarcode(ctx)) {
                        requestBt.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    }
                    showPicker = true
                }) { Text("Выбрать принтер") }
            } else {
                Text(
                    text = safeDeviceLabelBarcode(ctx, printer!!),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {
                    if (Build.VERSION.SDK_INT >= 31 && !hasBtConnectPermissionBarcode(ctx)) {
                        requestBt.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    }
                    showPicker = true
                }) { Text("Сменить") }
            }
        }

        status?.let {
            Spacer(Modifier.height(12.dp))
            Text(it)
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) { Text("Назад") }

            Button(
                enabled = canPrint,
                onClick = {
                    val value = barcode.trim()
                    if (value.isEmpty()) {
                        status = "Введите ШК"
                        return@Button
                    }

                    if (Build.VERSION.SDK_INT >= 31 && !hasBtConnectPermissionBarcode(ctx)) {
                        requestBt.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        status = "Нужно разрешение на Bluetooth"
                        return@Button
                    }

                    val d = printer ?: return@Button.also {
                        status = "Принтер не выбран"
                    }

                    isPrinting = true
                    status = null
                    scope.launch {
                        runCatching {
                            LabelPrinter.printTsplFixedSmall(
                                ctx,
                                d,
                                value,
                                value
                            )
                        }.onSuccess {
                            status = "Отправлено на печать"
                        }.onFailure { e ->
                            status = "Ошибка печати: ${e.localizedMessage}"
                        }
                        isPrinting = false
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isPrinting) "Печать…" else "Печать")
            }
        }
    }
    if (showPicker) {
        PickPrinterDialog(
            onPick = { d ->
                printer = d
                LabelPrinter.saveLastPrinter(ctx, d)
                showPicker = false
            },
            onCancel = { showPicker = false }
        )
    }
}

private fun hasBtConnectPermissionBarcode(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= 31) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun safeDeviceLabelBarcode(context: Context, device: android.bluetooth.BluetoothDevice): String {
    if (Build.VERSION.SDK_INT >= 31 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
        != PackageManager.PERMISSION_GRANTED
    ) return "BT-устройство (нет разрешения)"
    val name = try { device.name } catch (_: SecurityException) { null }
    val addr = try { device.address } catch (_: SecurityException) { null }
    return "${name ?: "BT"} (${addr ?: "??:??:??:??:??:??"})"
}
