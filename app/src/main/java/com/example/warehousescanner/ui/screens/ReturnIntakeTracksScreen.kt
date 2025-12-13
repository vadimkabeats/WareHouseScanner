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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.example.warehousescanner.data.GoogleSheetClient
import com.example.warehousescanner.data.YandexDiskClient
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun ReturnIntakeTracksScreen(
    onNextSingle: (track: String, comment: String?) -> Unit,
    onNextMulti: (track: String, qty: Int, comment: String?) -> Unit,
    onBack: () -> Unit
) {
    // Режим: обычный / мультизаказ
    var isMulti by remember { mutableStateOf(false) }

    // Обычный режим: до 3 треков
    var track1 by remember { mutableStateOf("") }
    var track2 by remember { mutableStateOf("") }
    var track3 by remember { mutableStateOf("") }

    // Мультизаказ: до 3 треков + количество товаров
    var multiTrack1 by remember { mutableStateOf("") }
    var multiTrack2 by remember { mutableStateOf("") }
    var multiTrack3 by remember { mutableStateOf("") }
    var multiQtyText by remember { mutableStateOf("") }

    fun joinedTracks(list: List<String>): String =
        list.map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("/")

    val canContinue = if (isMulti) {
        val q = multiQtyText.toIntOrNull()
        val tracks = joinedTracks(listOf(multiTrack1, multiTrack2, multiTrack3))
        tracks.isNotEmpty() && q != null && q > 0
    } else {
        joinedTracks(listOf(track1, track2, track3)).isNotEmpty()
    }

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
            // Переключатель режима
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = isMulti,
                    onCheckedChange = { isMulti = it }
                )
                Text(text = "Мультизаказ")
            }

            // Подсказка над полями треков
            Text(
                text = "Введите все трек-номера отправления",
                modifier = Modifier.padding(top = 4.dp)
            )

            if (isMulti) {
                // Мультизаказ: до 3 треков + количество
                OutlinedTextField(
                    value = multiTrack1,
                    onValueChange = { multiTrack1 = it },
                    label = { Text("Трек-номер 1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = multiTrack2,
                    onValueChange = { multiTrack2 = it },
                    label = { Text("Трек-номер 2") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = multiTrack3,
                    onValueChange = { multiTrack3 = it },
                    label = { Text("Трек-номер 3") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = multiQtyText,
                    onValueChange = { value ->
                        if (value.all { it.isDigit() } || value.isEmpty()) {
                            multiQtyText = value
                        }
                    },
                    label = { Text("Количество товаров") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Обычный режим: до 3 треков
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
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val comment: String? = null  // комментарий теперь на экране фоток

                    if (isMulti) {
                        val qty = multiQtyText.toIntOrNull() ?: return@Button
                        val tracks = joinedTracks(
                            listOf(multiTrack1, multiTrack2, multiTrack3)
                        )
                        if (tracks.isNotEmpty() && qty > 0) {
                            onNextMulti(tracks, qty, comment)
                        }
                    } else {
                        val tracks = joinedTracks(listOf(track1, track2, track3))
                        if (tracks.isNotEmpty()) {
                            onNextSingle(tracks, comment)
                        }
                    }
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
    itemCount: Int,
    comment: String?,         // сюда пока прилетает "", мы его используем как initial
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var currentIndex by remember { mutableStateOf(1) }
    var success by remember { mutableStateOf(false) }

    // 📝 комментарий для приёмки — редактируется на экране с фотками
    var commentText by remember { mutableStateOf(comment.orEmpty()) }

    Box(Modifier.fillMaxSize()) {
        if (!success) {
            key(currentIndex) {
                PhotoScreen(
                    extraContentBelowPhotos = {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            label = { Text("Комментарий (необязательно)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
                            maxLines = 3
                        )
                    },
                    onNext = { uris: List<Uri> ->
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
                                        "createdAt" to System.currentTimeMillis(),
                                        "itemIndex" to currentIndex,
                                        "itemCount" to itemCount
                                    ),
                                    photos = uris,
                                    itemIndex = if (itemCount > 1) currentIndex else null
                                )
                                val links = uploadResult.publicPhotoUrls

                                val resp = GoogleSheetClient.returnIntake(
                                    trackNumber = trackNumber,
                                    photoLinks = links,
                                    // 👇 берём комментарий уже с этого экрана,
                                    // а не из экрана треков
                                    comment = commentText.trim().ifEmpty { null }
                                )
                                if (!resp.ok) {
                                    throw RuntimeException(resp.error ?: "Ошибка записи в таблицу")
                                }

                                if (itemCount > 1 && currentIndex < itemCount) {
                                    currentIndex += 1
                                } else {
                                    success = true
                                }
                            } catch (t: Throwable) {
                                error = t.message ?: "Ошибка отправки"
                            } finally {
                                sending = false
                            }
                        }
                    }
                )
            }
        }

        // Надпись "Фотки для товара ..." — только если >1 и ещё не success
        if (itemCount > 1 && !success) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(
                        top = 40.dp,
                        start = 16.dp,
                        end = 16.dp
                    )
            ) {
                Text(
                    text = "Фотки для товара #$currentIndex из $itemCount",
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }
        }

        // Крутилка при отправке
        if (sending) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Ошибка внизу
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

        // Успешное завершение
        if (success && !sending) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Возврат добавлен в базу",
                        style = MaterialTheme.typography.h6
                    )
                    Button(
                        onClick = { onFinished() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("В меню возвратов")
                    }
                }
            }
        }
    }
}
