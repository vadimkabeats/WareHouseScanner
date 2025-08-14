package com.example.warehousescanner.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun ScanScreen(onNext: (String) -> Unit) {
    val context = LocalContext.current
    val activity = context as Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    var permanentlyDenied by remember { mutableStateOf(false) }
    var requestedOnce by rememberSaveable { mutableStateOf(false) }

    val requestPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        permanentlyDenied = !granted &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
    }

    LaunchedEffect(Unit) {
        if (!hasPermission && !requestedOnce) {
            requestedOnce = true
            requestPermission.launch(Manifest.permission.CAMERA)
        }
    }

    var scannedCode by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        if (hasPermission) {
            AndroidView(factory = { ctx ->
                PreviewView(ctx).also { previewView ->
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().apply {
                            setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val options = BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(
                                Barcode.FORMAT_EAN_13,
                                Barcode.FORMAT_CODE_128
                            ).build()
                        val scanner = BarcodeScanning.getClient(options)

                        val analysis = ImageAnalysis.Builder().build().also { useCase ->
                            @OptIn(ExperimentalGetImage::class)
                            fun analyze(proxy: androidx.camera.core.ImageProxy) {
                                if (scannedCode != null) { proxy.close(); return }
                                val mediaImage = proxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(
                                        mediaImage, proxy.imageInfo.rotationDegrees
                                    )
                                    scanner.process(image)
                                        .addOnSuccessListener { codes ->
                                            codes.firstOrNull()?.rawValue?.let { code ->
                                                scannedCode = code
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("ScanScreen", "Scan failed", e)
                                        }
                                        .addOnCompleteListener { proxy.close() }
                                } else proxy.close()
                            }
                            useCase.setAnalyzer(Executors.newSingleThreadExecutor(), ::analyze)
                        }

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis
                        )
                    }, ContextCompat.getMainExecutor(ctx))
                }
            }, modifier = Modifier.fillMaxSize())

            scannedCode?.let { code ->
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                ) {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
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
        } else {
            // Никаких лишних запросов: если пользователь запретил навсегда — только кнопка настроек
            Column(
                Modifier.fillMaxSize().padding(24.dp),
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
                        )
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }) { Text("Открыть настройки") }
                } else {
                    // Запрос уже ушёл автоматически; покажем подсказку
                    Text("Разрешите доступ в системном диалоге.")
                }
            }
        }
    }
}
