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

        val oauthToken = "y0__xDG6vflBRituTkgq5zagxS2sGCCdf2L4JbOmOIMoeBfsnOKTw"
        val scriptUrl  = "https://script.google.com/macros/s/AKfycbxPAmspfzK46MZw9n99t2eW1NNo9Apx_jdssoVr7qCosidivP-FJ0IglNIYdQgMKCw/exec"
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
