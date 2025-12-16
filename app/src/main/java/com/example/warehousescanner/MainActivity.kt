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
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val oauthToken = "y0__xDC-PqfqveAAhiItjgguIj3xRMa5L1mPbJOCrNGULQHPk3yPF52zA"
        val fastApiUrl = "http://158.160.87.160:8000/api"
        val gasExecUrl = "https://script.google.com/macros/s/AKfycbwB9JnexK_J4XZ3260BuoHAr0jMABKAAij_-LN5by2ez2N-qKuQ2gVJlHTkCdBWIdI/exec"
        val apiKey = "SECRET_KEY"
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
