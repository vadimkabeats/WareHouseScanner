package com.example.warehousescanner.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.warehousescanner.util.createTempImageUri
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PhotoScreen(onNext: (List<Uri>) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val photos = remember { mutableStateListOf<Uri>() }

    // Нижний лист (выбор источникa)
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )

    // Pending URI для камеры (переживает рекомпозиции/повороты)
    var pendingCameraUriStr by rememberSaveable { mutableStateOf<String?>(null) }

    // Камера через ACTION_IMAGE_CAPTURE с ручной выдачей прав на URI
    val cameraLauncher = rememberLauncherForActivityResult(StartActivityForResult()) { res: ActivityResult ->
        val u = pendingCameraUriStr?.let(Uri::parse)
        if (res.resultCode == Activity.RESULT_OK && u != null) {
            if (photos.size < 6) photos.add(u)
        }
        // Отзываем временные права у всех получателей
        if (u != null) {
            context.revokeUriPermission(
                u,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        pendingCameraUriStr = null
    }

    fun launchCamera(ctx: Context) {
        val uri = createTempImageUri(ctx)
        pendingCameraUriStr = uri.toString()

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Выдаём временные права всем обработчикам интента камеры
        val resInfo = ctx.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        for (resolveInfo in resInfo) {
            val pkg = resolveInfo.activityInfo.packageName
            ctx.grantUriPermission(
                pkg, uri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        cameraLauncher.launch(intent)
    }

    // Галерея: мультивыбор
    val pickMultiple = rememberLauncherForActivityResult(GetMultipleContents()) { uris ->
        if (!uris.isNullOrEmpty()) {
            val left = 6 - photos.size
            if (left > 0) photos.addAll(uris.take(left))
        }
    }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            Column(Modifier.fillMaxWidth().padding(8.dp)) {
                ListItem(
                    icon = { Icon(Icons.Default.PhotoCamera, contentDescription = null) },
                    text  = { Text("Сфотографировать") },
                    modifier = Modifier.clickable {
                        scope.launch { sheetState.hide() }
                        launchCamera(context)
                    }
                )
                ListItem(
                    icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                    text  = { Text("Выбрать из галереи") },
                    modifier = Modifier.clickable {
                        scope.launch { sheetState.hide() }
                        pickMultiple.launch("image/*")
                    }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Фотографии", style = MaterialTheme.typography.h6)
                Text("${photos.size}/6", style = MaterialTheme.typography.body2, color = MaterialTheme.colors.primary)
            }

            Spacer(Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Плитка "Добавить"
                item {
                    Box(
                        Modifier
                            .aspectRatio(1f)
                            .background(Color(0x11000000))
                            .clickable { scope.launch { sheetState.show() } },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Text("Добавить", textAlign = TextAlign.Center, style = MaterialTheme.typography.caption)
                        }
                    }
                }

                // Превью выбранных фото
                items(photos) { u ->
                    Box(Modifier.aspectRatio(1f)) {
                        AsyncImage(model = u, contentDescription = null, modifier = Modifier.fillMaxSize())
                        IconButton(
                            onClick = { photos.remove(u) },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) { Icon(Icons.Default.Close, contentDescription = "Удалить") }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { onNext(photos.toList()) },
                enabled = photos.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Далее") }
        }
    }
}
