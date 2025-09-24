package com.example.warehousescanner.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun ScanScreen(
    instanceKey: String = "",
    allowManualInput: Boolean = true,
    inputHint: String? = null,
    validator: ((String) -> String?)? = null,
    onNext: (String) -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current


    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    var permanentlyDenied by remember { mutableStateOf(false) }
    var requestedOnce by rememberSaveable(instanceKey) { mutableStateOf(false) }
    var scannedCode by remember(instanceKey) { mutableStateOf<String?>(null) }
    var scanError by remember(instanceKey) { mutableStateOf<String?>(null) }
    var initError by remember(instanceKey) { mutableStateOf<String?>(null) }

    val requestPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        permanentlyDenied = !granted && activity?.let {
            !ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
        } ?: false
    }

    LaunchedEffect(Unit) {
        if (!hasPermission && !requestedOnce) {
            requestedOnce = true
            requestPermission.launch(Manifest.permission.CAMERA)
        }
    }

    var showManual by rememberSaveable(instanceKey) { mutableStateOf(false) }
    var manualText by rememberSaveable(instanceKey) { mutableStateOf("") }
    var manualError by rememberSaveable(instanceKey) { mutableStateOf<String?>(null) }

    fun validateManualCommon(s: String): String? {
        val v = s.trim()
        if (v.isEmpty()) return "Введите код"
        if (v.length > 64) return "Слишком длинный код (до 64 символов)"
        return null
    }

    fun openManual() {
        manualText = ""
        manualError = null
        showManual = true
    }

    val previewView = remember(instanceKey) {
        Log.d("ScanScreen", "Creating PreviewView for instanceKey: $instanceKey")
        androidx.camera.view.PreviewView(context).apply {
            implementationMode = androidx.camera.view.PreviewView.ImplementationMode.COMPATIBLE
            scaleType = androidx.camera.view.PreviewView.ScaleType.FILL_CENTER
        }
    }

    val controller = remember(instanceKey) {
        Log.d("ScanScreen", "Creating CameraController for instanceKey: $instanceKey")
        androidx.camera.view.LifecycleCameraController(context)
    }

    val analysisExecutor = remember(instanceKey) {
        Executors.newSingleThreadExecutor()
    }

    val scanner = remember(instanceKey) {
        val opts = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_CODE_128)
            .build()
        BarcodeScanning.getClient(opts)
    }

    LaunchedEffect(hasPermission, instanceKey) {
        if (!hasPermission) return@LaunchedEffect

        initError = null
        Log.d("ScanScreen", "Setting up camera for instanceKey: $instanceKey")

        try {

            runCatching { controller.clearImageAnalysisAnalyzer() }
            runCatching { controller.unbind() }
            runCatching { previewView.controller = null }


            kotlinx.coroutines.delay(1000)


            controller.cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
            controller.setEnabledUseCases(
                androidx.camera.view.CameraController.IMAGE_ANALYSIS
            )


            controller.setImageAnalysisAnalyzer(analysisExecutor) { proxy ->
                try {
                    if (scannedCode != null) {
                        proxy.close()
                        return@setImageAnalysisAnalyzer
                    }

                    val media = proxy.image ?: run {
                        proxy.close(); return@setImageAnalysisAnalyzer
                    }

                    val image = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                    scanner.process(image)
                        .addOnSuccessListener { codes ->
                            codes.firstOrNull()?.rawValue?.let { value ->
                                val err = validator?.invoke(value)
                                if (err == null) {
                                    Log.d("ScanScreen", "Barcode scanned: $value for instanceKey: $instanceKey")
                                    scannedCode = value
                                } else {
                                    scanError = err
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("ScanScreen", "Scan failed for instanceKey: $instanceKey", e)
                        }
                        .addOnCompleteListener { proxy.close() }
                } catch (t: Throwable) {
                    Log.e("ScanScreen", "Analyzer error for instanceKey: $instanceKey", t)
                    proxy.close()
                }
            }

            previewView.controller = controller
            controller.bindToLifecycle(lifecycleOwner)

            Log.d("ScanScreen", "Camera setup completed for instanceKey: $instanceKey")
        } catch (t: Throwable) {
            Log.e("ScanScreen", "Camera setup failed for instanceKey: $instanceKey", t)
            initError = t.localizedMessage ?: "Не удалось инициализировать камеру"
        }
    }

    // Cleanup
    DisposableEffect(instanceKey) {
        onDispose {
            Log.d("ScanScreen", "Cleaning up camera for instanceKey: $instanceKey")
            runCatching { controller.clearImageAnalysisAnalyzer() }
            runCatching { controller.unbind() }
            runCatching { previewView.controller = null }
            runCatching { analysisExecutor.shutdown() }
            runCatching { scanner.close() }
        }
    }

    // ---------- UI ----------
    Box(Modifier.fillMaxSize()) {

        when {
            initError != null -> {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.surface
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Ошибка камеры:\n$initError")
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = {
                            initError = null
                            scannedCode = null
                        }) { Text("Повторить") }

                        if (allowManualInput) {
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { openManual() }) { Text("Ввести вручную") }
                        }
                    }
                }
            }

            !hasPermission -> {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.surface
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Для сканирования требуется доступ к камере.")
                        Spacer(Modifier.height(16.dp))
                        if (permanentlyDenied) {
                            Button(onClick = {
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            }) { Text("Открыть настройки") }
                        } else {
                            Button(onClick = { requestPermission.launch(Manifest.permission.CAMERA) }) {
                                Text("Разрешить камеру")
                            }
                        }

                        if (allowManualInput) {
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(onClick = { openManual() }) { Text("Ввести вручную") }
                        }
                    }
                }
            }

            else -> {

                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(2.dp, Color.Red),
                    factory = {
                        Log.d("ScanScreen", "AndroidView factory for instanceKey: $instanceKey")
                        previewView
                    },
                    update = { view ->
                        Log.d("ScanScreen", "AndroidView update for instanceKey: $instanceKey")
                        if (view.controller == null) view.controller = controller
                    }
                )


                if (scanError != null) {
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colors.error,
                            shape = MaterialTheme.shapes.small,
                            elevation = 4.dp
                        ) {
                            Text(
                                scanError!!,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.caption
                            )
                        }
                    }
                    LaunchedEffect(scanError) {
                        kotlinx.coroutines.delay(1500)
                        scanError = null
                    }
                }


                if (allowManualInput && scannedCode == null) {
                    Button(
                        onClick = { openManual() },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 24.dp)
                    ) { Text("Ввести вручную") }
                }


                scannedCode?.let { code ->
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                    ) {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Считан код:", color = Color.White)
                            Spacer(Modifier.height(8.dp))
                            Text(code, style = MaterialTheme.typography.h5, color = Color.White)
                            Spacer(Modifier.height(24.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Button(onClick = { onNext(code) }) { Text("Подтвердить") }
                                OutlinedButton(onClick = { scannedCode = null }) { Text("Повторить") }
                            }
                        }
                    }
                }
            }
        }

        // Диалог ручного ввода
        if (showManual && allowManualInput) {
            AlertDialog(
                onDismissRequest = { showManual = false },
                title = { Text("Ручной ввод штрих-кода") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = manualText,
                            onValueChange = { txt ->

                                val cleaned = txt.replace(Regex("\\s+"), "")
                                manualText = cleaned
                                manualError = null
                            },
                            singleLine = true,
                            placeholder = { Text("Например: RETS00024185958LPB") },
                            isError = manualError != null,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = manualError ?: (inputHint ?: "Разрешены любые символы без пробелов. До 64 знаков."),
                            color = if (manualError != null)
                                MaterialTheme.colors.error
                            else
                                MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.caption
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        manualError = validateManualCommon(manualText)
                            ?: validator?.invoke(manualText)
                        if (manualError == null) {
                            showManual = false
                            onNext(manualText.trim())
                        }
                    }) { Text("Подтвердить") }
                },
                dismissButton = {
                    TextButton(onClick = { showManual = false }) { Text("Отмена") }
                }
            )
        }
    }
}
