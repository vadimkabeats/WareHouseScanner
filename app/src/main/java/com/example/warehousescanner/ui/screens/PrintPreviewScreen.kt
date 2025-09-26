package com.example.warehousescanner.ui.screens

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.warehousescanner.data.GoogleSheetClient
import com.example.warehousescanner.printer.LabelPrinter
import kotlinx.coroutines.launch

@Composable
fun PrintPreviewScreen(
    code: String,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- грузим трек по штрих-коду ---
    var isLoading by remember { mutableStateOf(true) }
    var trackShort by remember { mutableStateOf<String?>(null) } // "Трекномер" (короткий), для ШК
    var trackFull by remember { mutableStateOf<String?>(null) }  // "Полный трекномер", для подписи и UI
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(code) {
        isLoading = true
        loadError = null
        try {
            val resp = GoogleSheetClient.lookupTrack(code)
            if (resp.found) {
                trackShort = resp.track?.takeIf { it.isNotBlank() }
                trackFull  = (resp.full ?: resp.track)?.takeIf { it.isNotBlank() }
            } else {
                trackShort = null
                trackFull  = null
            }
        } catch (e: Exception) {
            loadError = e.localizedMessage ?: "Ошибка загрузки"
            trackShort = null
            trackFull = null
        }
        isLoading = false
    }

    // --- принтер: запоминаем последний выбранный ---
    var printer by remember { mutableStateOf(LabelPrinter.restoreLastPrinter(ctx)) }
    var showPicker by remember { mutableStateOf(printer == null) }
    var isPrinting by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    val requestBt = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op */ }

    val canPrint = !isLoading &&
            loadError == null &&
            !trackShort.isNullOrBlank() &&
            printer != null &&
            !isPrinting

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        // header
        Text("Печать этикетки", style = MaterialTheme.typography.h6)
        Spacer(Modifier.height(8.dp))
        Text("Штрих-код: $code")
        Spacer(Modifier.height(8.dp))

        // трек — показываем ТОЛЬКО полный
        when {
            isLoading -> Text("Загружаю трек-номер…")
            loadError != null -> Text("Не удалось получить трек: $loadError")
            trackFull.isNullOrBlank() -> Text("Трек-номер не найден")
            else -> Text("Трек-номер: ${trackFull!!}")
        }

        Spacer(Modifier.height(16.dp))

        // выбор/отображение принтера
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (printer == null) {
                Button(onClick = {
                    if (Build.VERSION.SDK_INT >= 31 && !hasBtConnectPermission(ctx)) {
                        requestBt.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    }
                    showPicker = true
                }) { Text("Выбрать принтер") }
            } else {
                Text(
                    text = safeDeviceLabel(ctx, printer!!),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {
                    if (Build.VERSION.SDK_INT >= 31 && !hasBtConnectPermission(ctx)) {
                        requestBt.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    }
                    showPicker = true
                }) { Text("Сменить") }
            }
        }

        // сообщение статуса
        status?.let {
            Spacer(Modifier.height(12.dp))
            Text(it)
        }

        Spacer(Modifier.weight(1f))

        // нижняя панель: Назад + Печать
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
                    if (Build.VERSION.SDK_INT >= 31 && !hasBtConnectPermission(ctx)) {
                        requestBt.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        status = "Нужно разрешение на Bluetooth"
                        return@Button
                    }
                    val d = printer ?: return@Button.also { status = "Принтер не выбран" }
                    val short = trackShort ?: return@Button.also { status = "Нет короткого трека" }
                    val full  = trackFull ?: short

                    isPrinting = true
                    status = null
                    scope.launch {
                        runCatching {
                            // ШК = короткий, подпись = полный
                            LabelPrinter.printTsplFixedSmall(ctx, d, short, full)
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
            onPick = { d: BluetoothDevice ->
                printer = d
                LabelPrinter.saveLastPrinter(ctx, d)
                showPicker = false
            },
            onCancel = { showPicker = false }
        )
    }
}

/* ---------- Диалог выбора принтера ---------- */

@Composable
fun PickPrinterDialog(
    onPick: (BluetoothDevice) -> Unit,
    onCancel: () -> Unit
) {
    val ctx = LocalContext.current
    var btGranted by remember { mutableStateOf(hasBtConnectPermission(ctx)) }
    val requestBt = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        btGranted = granted || hasBtConnectPermission(ctx)
    }

    val devices: List<BluetoothDevice> = remember(btGranted) {
        if (btGranted) LabelPrinter.getPairedDevices(ctx) else emptyList()
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Выберите принтер") },
        text = {
            Column {
                if (!btGranted && Build.VERSION.SDK_INT >= 31) {
                    Text("Чтобы показать спаренные устройства, нужно разрешение BLUETOOTH_CONNECT.")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { requestBt.launch(Manifest.permission.BLUETOOTH_CONNECT) }) {
                        Text("Разрешить Bluetooth")
                    }
                } else if (devices.isEmpty()) {
                    Text("Нет спаренных устройств.\nПодключите принтер в системных настройках Bluetooth.")
                } else {
                    devices.forEach { d: BluetoothDevice ->
                        TextButton(onClick = { onPick(d) }) {
                            Text(safeDeviceLabel(ctx, d))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Отмена") }
        }
    )
}

/* ---------- Вспомогательные функции ---------- */

private fun hasBtConnectPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= 31) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun safeDeviceLabel(context: Context, device: BluetoothDevice): String {
    if (Build.VERSION.SDK_INT >= 31 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
        != PackageManager.PERMISSION_GRANTED
    ) return "BT-устройство (нет разрешения)"
    val name = try { device.name } catch (_: SecurityException) { null }
    val addr = try { device.address } catch (_: SecurityException) { null }
    return "${name ?: "BT"} (${addr ?: "??:??:??:??:??:??"})"
}
