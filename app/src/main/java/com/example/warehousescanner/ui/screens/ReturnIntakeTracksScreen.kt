package com.example.warehousescanner.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.example.warehousescanner.data.GoogleSheetClient
import com.example.warehousescanner.data.YandexDiskClient
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun ReturnIntakeTracksScreen(
    onNext: (String) -> Unit,
    onBack: () -> Unit
) {
    var track1 by remember { mutableStateOf("") }
    var track2 by remember { mutableStateOf("") }
    var track3 by remember { mutableStateOf("") }
    val canContinue = listOf(track1, track2, track3).any { it.isNotBlank() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Приемка возвратов") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = track1,
                onValueChange = { track1 = it },
                label = { Text("Трек-номер 1") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = track2,
                onValueChange = { track2 = it },
                label = { Text("Трек-номер 2") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = track3,
                onValueChange = { track3 = it },
                label = { Text("Трек-номер 3") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val trackNumber = listOf(track1, track2, track3)
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .joinToString("/")
                    onNext(trackNumber)
                },
                enabled = canContinue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Далее")
            }
        }
    }
}

@Composable
fun ReturnIntakePhotosScreen(
    trackNumber: String,
    oauthToken: String,
    onFinished: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        PhotoScreen { uris: List<Uri> ->
            if (uris.isEmpty() || sending) return@PhotoScreen

            scope.launch {
                sending = true
                error = null
                try {
                    YandexDiskClient.init(oauthToken)
                    val uploadResult = YandexDiskClient.uploadReturnBundleJson(
                        context = context,
                        barcode = trackNumber.replace("/", "_"),
                        metadata = mapOf(
                            "trackNumber" to trackNumber,
                            "createdAt" to System.currentTimeMillis()
                        ),
                        photos = uris
                    )
                    val links = uploadResult.publicPhotoUrls
                    val resp = GoogleSheetClient.returnIntake(
                        trackNumber = trackNumber,
                        photoLinks = links
                    )
                    if (!resp.ok) {
                        throw RuntimeException(resp.error ?: "Ошибка записи в таблицу")
                    }
                    onFinished()
                } catch (t: Throwable) {
                    error = t.message ?: "Ошибка отправки"
                } finally {
                    sending = false
                }
            }
        }

        if (sending) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        if (error != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Ошибка: $error")
                Button(
                    onClick = { error = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ОК")
                }
            }
        }
    }
}

