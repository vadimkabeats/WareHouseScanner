package com.example.warehousescanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import com.example.warehousescanner.data.LostItem
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.Dialog
@Composable
fun HomeScreen(
    onAddItem: () -> Unit,
    onPutAway: () -> Unit,
    onPrintLabel: () -> Unit,
    onReceiveReturn: () -> Unit,
    onReconcile: () -> Unit,
    // Статистика НЛО — только за сегодня (личная)
    statsNonNlo: Int?,
    statsNlo: Int?,
    statsLoading: Boolean,
    // Личная статистика потока
    statsIdentified: Int?,
    statsPutAway: Int?,
    statsLost: Int?,
    // Суммарная статистика по складу
    totalIdentified: Int?,
    totalPutAway: Int?,
    // Клик по "НЕ дошло до полки"
    onShowLostDetails: () -> Unit,
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
        // 1) Переключатель фонарика — теперь сверху
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

        // 2) Карточка статистики НЛО за сегодня (по сотруднику)
        Row(Modifier.fillMaxWidth()) {
            DailyStatsCard(
                title = "Проверено за сегодня (ты)",
                nonNlo = statsNonNlo,
                nlo = statsNlo,
                loading = statsLoading
            )
            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))

        // 3) Вторая карточка: статистика конкретного сотрудника
        FlowStatsCard(
            identified = statsIdentified,
            putAway = statsPutAway,
            lost = statsLost,
            loading = statsLoading,
            onShowLostDetails = onShowLostDetails
        )

        Spacer(Modifier.height(8.dp))

        // 4) СУММАРНАЯ статистика по складу
        TotalFlowStatsCard(
            totalIdentified = totalIdentified,
            totalPutAway = totalPutAway,
            loading = statsLoading
        )

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
        Spacer(Modifier.height(12.dp))

    }
}

@Composable
private fun DailyStatsCard(
    title: String,
    nonNlo: Int?,
    nlo: Int?,
    loading: Boolean
) {
    Card {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.subtitle1)
            if (loading) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }
            Text("не НЛО: ${nonNlo?.toString() ?: "—"}")
            Text("НЛО: ${nlo?.toString() ?: "—"}")
        }
    }
}

/**
 * Личная статистика конкретного сотрудника.
 */
@Composable
private fun FlowStatsCard(
    identified: Int?,
    putAway: Int?,
    lost: Int?,
    loading: Boolean,
    onShowLostDetails: () -> Unit
) {
    Card {
        Column(Modifier.padding(12.dp)) {
            Text("Движение товаров за сегодня", style = MaterialTheme.typography.subtitle1)

            if (loading) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }

            Text("Идентифицировано товаров: ${identified?.toString() ?: "—"}")
            Text("Из них дошло до полки: ${putAway?.toString() ?: "—"}")

            Spacer(Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Из них НЕ дошло до полки: ${lost?.toString() ?: "—"}",
                    color = MaterialTheme.colors.error      // красный текст
                )

                // Кнопка "Посмотреть" вместо клика по цифре
                if ((lost ?: 0) > 0) {
                    TextButton(onClick = onShowLostDetails) {
                        Text("Посмотреть")
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LostItemsDialog(
    items: List<LostItem>,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    val config = LocalConfiguration.current
    // Диалог максимум на ~80% высоты экрана
    val maxHeight = (config.screenHeightDp.dp * 0.8f)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(min = 200.dp, max = maxHeight)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Заголовок
                Text(
                    text = "Товары, не дошедшие до полки",
                    style = MaterialTheme.typography.h6
                )

                Spacer(Modifier.height(8.dp))

                // Основная область контента — фиксированной высоты, со скроллом
                CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = true)   // забираем всё свободное место, но не растягиваем диалог выше maxHeight
                    ) {
                        when {
                            loading -> {
                                Box(
                                    Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }

                            error != null -> {
                                Column(
                                    modifier = Modifier.align(Alignment.TopStart)
                                ) {
                                    Text("Ошибка загрузки:")
                                    Spacer(Modifier.height(4.dp))
                                    Text(error)
                                }
                            }

                            items.isEmpty() -> {
                                Box(
                                    Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Все идентифицированные тобой товары дошли до полки 🎉")
                                }
                            }

                            else -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(scrollState)
                                ) {
                                    items.forEachIndexed { index, item ->
                                        LostItemRow(item)
                                        if (index < items.lastIndex) {
                                            Spacer(Modifier.height(8.dp))
                                            Divider()
                                            Spacer(Modifier.height(8.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Кнопки внизу
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (error != null) {
                        TextButton(onClick = onRetry) {
                            Text("Повторить")
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Закрыть")
                    }
                }
            }
        }
    }
}

@Composable
private fun LostItemRow(item: LostItem) {
    val context = LocalContext.current
    val photos = item.photos
    val photosCount = photos.size

    Column {
        // ШК
        Text("ШК: ${item.barcode}")

        // Время сканирования
        item.createdAt?.takeIf { it.isNotBlank() }?.let {
            Text(
                "Время сканирования: $it",
                style = MaterialTheme.typography.caption
            )
        }

        // Кликабельная ссылка
        item.link?.takeIf { it.isNotBlank() }?.let { link ->
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Ссылка:",
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = {
                        runCatching {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                            context.startActivity(intent)
                        }
                    }
                ) {
                    Text("Открыть")
                }
            }

            // Короткая форма ссылки (чтобы было видно, что за сайт)
            Text(
                text = link,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.body2
            )
        }

        // Фото
        if (photosCount > 0) {
            Spacer(Modifier.height(4.dp))

            Text(
                text = "Фото: $photosCount",
                style = MaterialTheme.typography.caption
            )

            Spacer(Modifier.height(2.dp))

            // Кнопки "Фото 1", "Фото 2", "Фото 3" (максимум 3 для компактности)
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                photos.take(3).forEachIndexed { index, url ->
                    TextButton(
                        onClick = {
                            runCatching {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                        }
                    ) {
                        Text("Фото ${index + 1}")
                    }
                    if (index < photos.take(3).lastIndex) {
                        Spacer(Modifier.width(4.dp))
                    }
                }
            }

            // Если фоток больше 3 — показываем, что есть ещё
            if (photosCount > 3) {
                Text(
                    text = "+ ещё ${photosCount - 3}",
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}


/**
 * Суммарная статистика по всем сотрудникам за сегодняшний день.
 */
@Composable
private fun TotalFlowStatsCard(
    totalIdentified: Int?,
    totalPutAway: Int?,
    loading: Boolean
) {
    Card {
        Column(Modifier.padding(12.dp)) {
            Text("Суммарно по складу за сегодня", style = MaterialTheme.typography.subtitle1)
            if (loading) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }
            Text("Идентифицировано товаров: ${totalIdentified?.toString() ?: "—"}")
            Text("Положено товаров: ${totalPutAway?.toString() ?: "—"}")
        }
    }
}
