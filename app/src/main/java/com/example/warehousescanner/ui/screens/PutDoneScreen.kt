package com.example.warehousescanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PutDoneScreen(
    isLoading: Boolean,
    message: String,
    onBackHome: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Положить товар", style = MaterialTheme.typography.h6)
        Spacer(Modifier.height(16.dp))
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Text(message)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onBackHome, modifier = Modifier.fillMaxWidth()) {
                Text("На главное меню")
            }
        }
    }
}
