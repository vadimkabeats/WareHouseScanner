package com.example.warehousescanner.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun AuthScreen(clientId: String, redirectUri: String) {
    val ctx = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Для работы нужно авторизоваться на Яндекс.Диске")
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            // Запрашиваем implicit grant
            val authUrl = "https://oauth.yandex.ru/authorize" +
                    "?response_type=token" +
                    "&client_id=$clientId" +
                    "&redirect_uri=$redirectUri"
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)))
        }) {
            Text("Авторизоваться")
        }
    }
}
