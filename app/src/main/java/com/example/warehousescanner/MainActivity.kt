package com.example.warehousescanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.MaterialTheme
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Surface
import androidx.navigation.compose.rememberNavController
import com.example.warehousescanner.data.GoogleSheetClient
import com.example.warehousescanner.ui.navigation.MainNavHost
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val oauthToken = "y0__xDC-PqfqveAAhiItjgguIj3xRMa5L1mPbJOCrNGULQHPk3yPF52zA"

        // FastAPI (/api) — для Аккаунтов / Базы / Сканировки / Хранения / Статистики
        val fastApiUrl = "http://158.160.87.160:8000/api"

        // Apps Script exec — для Возвратов / Этикеток / Сверки / labelPrinted
        val gasExecUrl = "https://script.google.com/macros/s/AKfycbxobBQ-5mYGgNyy6_fC3qMT-MgC30l7WMnUb6amybCMLI0lQdqtCTrwpS4ngQEqpAM/exec"

        // key должен совпадать с API_KEY в Apps Script
        val apiKey = "SECRET_KEY"

        // ВАЖНО: тут должна быть сигнатура из "постгресс-версии" GoogleSheetClient
        GoogleSheetClient.init(
            fastApiUrl = fastApiUrl,
            gasExecUrl = gasExecUrl,
            key = apiKey
        )

        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            val nav = rememberNavController()
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                color = MaterialTheme.colors.background
            ) {
                MainNavHost(nav = nav, oauthToken = oauthToken)
            }
        }
    }
}
