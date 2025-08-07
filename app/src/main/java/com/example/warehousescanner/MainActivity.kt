package com.example.warehousescanner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.warehousescanner.ui.navigation.MainNavHost
import com.example.warehousescanner.ui.screens.AuthScreen
import com.example.warehousescanner.ui.screens.FilePickerScreen
import com.example.warehousescanner.viewmodel.ExcelViewModel

class MainActivity : ComponentActivity() {
    private val CLIENT_ID    = "586d70fd7a0540bf8985c039763b8986"
    private val REDIRECT_URI = "com.example.warehousescanner://oauth"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleRedirect(intent)

        setContent {
            val navController = rememberNavController()
            val excelVm: ExcelViewModel = viewModel()
            val fileUri by excelVm.fileUriState.collectAsState()
            val token = YandexAuth.token

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
                color = MaterialTheme.colors.background
            ) {
                when {
                    fileUri == null ->
                        FilePickerScreen { excelVm.setFile(it) }

                    token == null   ->
                        AuthScreen(CLIENT_ID, REDIRECT_URI)

                    else            ->
                        MainNavHost(navController, excelVm)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleRedirect(intent)
    }

    private fun handleRedirect(intent: Intent) {
        intent.data?.let { uri: Uri ->
            // схема://oauth#access_token=...&...
            if (uri.scheme == "com.example.warehousescanner" && uri.host == "oauth") {
                val frag = uri.fragment
                if (frag != null) {
                    // Ищем параметр access_token в фрагменте
                    val tokenPart = frag.split("&")
                        .firstOrNull { it.startsWith("access_token=") }
                    val token = tokenPart?.substringAfter("access_token=")
                    if (!token.isNullOrBlank()) {
                        YandexAuth.token = token
                        return
                    }
                }
                Toast.makeText(this, "OAuth failed: token not found", Toast.LENGTH_LONG).show()
            }
        }
    }
}
