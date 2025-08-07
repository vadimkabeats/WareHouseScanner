package com.example.warehousescanner.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoScreen(
    onNext: (List<Uri>) -> Unit
) {
    val photos = remember { mutableStateListOf<Uri>() }
    val pickImage = rememberLauncherForActivityResult(
        GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (photos.size < 6) photos.add(it)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Добавьте фото (до 6)", style = MaterialTheme.typography.h6)
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(photos) { uri ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .fillMaxSize()
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick = { photos.remove(uri) },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Close,
                            contentDescription = "Удалить",
                            tint = Color.White
                        )
                    }
                }
            }
            if (photos.size < 6) {
                item {
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .fillMaxSize()
                            .clickable { pickImage.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "+", style = MaterialTheme.typography.h4)
                    }
                }
            }
        }
        Button(
            onClick = { onNext(photos.toList()) },
            enabled = photos.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Далее")
        }
    }
}
