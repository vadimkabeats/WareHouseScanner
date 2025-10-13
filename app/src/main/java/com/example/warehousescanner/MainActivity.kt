package com.example.warehousescanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
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
        val scriptUrl  = "https://script.google.com/macros/s/AKfycby6bPg0Pgcphm-8qO4D1_UiikWz0oaq4CbxxrTMyntJTGtqVbQVjo6Zg_KN6bLfank/exec"
        val apiKey     = "SECRET_KEY"

        GoogleSheetClient.init(scriptUrl, apiKey)
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
