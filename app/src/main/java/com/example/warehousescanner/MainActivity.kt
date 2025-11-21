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
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val oauthToken = "y0__xDC-PqfqveAAhiItjgguIj3xRMa5L1mPbJOCrNGULQHPk3yPF52zA"

        // FastAPI (/api) — для Аккаунтов/Базы/Сканировки/Хранения/Статистики
        //val fastApiUrl = "https://warehouseapi123.loca.lt/api"

        // Apps Script exec — для Возвратов/Этикеток/Сверки (ПОСТАВЬ СВОЙ exec URL!)
        val gasExecUrl = "https://script.google.com/macros/s/AKfycby76ct_HbxRL2F69iFNzolBrMQvJoWyPyiacuex8HMxjhFb27piTzl907Usf7K5Y-0/exec"

        val apiKey = "SECRET_KEY" // если Apps Script проверяет key — укажи его здесь

        GoogleSheetClient.init(gasExecUrl, apiKey)

        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContent {
            val nav = rememberNavController()
            Surface(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                color = MaterialTheme.colors.background
            ) {
                MainNavHost(nav = nav, oauthToken = oauthToken)
            }
        }
    }
}
