package com.example.warehousescanner.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun PhotoScreen(onNext: (List<Uri>) -> Unit) {
    val photos = remember { mutableStateListOf<Uri>() }
    val pick = rememberLauncherForActivityResult(GetContent()) { uri ->
        if (uri != null && photos.size < 6) photos.add(uri)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Добавьте фото (до 6)", style = MaterialTheme.typography.h6)
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(photos) { u ->
                Box(Modifier.aspectRatio(1f)) {
                    AsyncImage(model = u, contentDescription = null, modifier = Modifier.fillMaxSize())
                    IconButton(onClick = { photos.remove(u) }, modifier = Modifier.align(Alignment.TopEnd)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null)
                    }
                }
            }
            if (photos.size < 6) {
                item {
                    Box(
                        Modifier.aspectRatio(1f).clickable { pick.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) { Text("+", style = MaterialTheme.typography.h4) }
                }
            }
        }
        Button(
            onClick = { onNext(photos.toList()) },
            enabled = photos.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Далее") }
    }
}
