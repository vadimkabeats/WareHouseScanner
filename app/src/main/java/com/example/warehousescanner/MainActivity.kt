package com.example.warehousescanner

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.*
import com.example.warehousescanner.data.GoogleSheetClient
import com.example.warehousescanner.ui.screens.*
import com.example.warehousescanner.viewmodel.GoogleSheetViewModel
import com.example.warehousescanner.viewmodel.UserViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val oauthToken = "y0__xDG6vflBRituTkgq5zagxS2sGCCdf2L4JbOmOIMoeBfsnOKTw"
        val scriptUrl  = "https://script.google.com/macros/s/AKfycbyaZ2Wyujl9IVhY1sP8MKS4AX9OuzSqUPygJdVwrziHjqxv0g4YeLoF1BgSOqVjc6bx/exec"
        val apiKey     = "REPLACE_WITH_SECRET"

        GoogleSheetClient.init(scriptUrl, apiKey)

        setContent {
            val nav = rememberNavController()
            val gsVm: GoogleSheetViewModel = viewModel()
            val userVm: UserViewModel = viewModel(this@MainActivity)
            val fullName by userVm.fullName.collectAsState()

            Surface(
                modifier = Modifier.fillMaxSize().systemBarsPadding(),
                color = MaterialTheme.colors.background
            ) {
                NavHost(navController = nav, startDestination = "gate") {

                    composable("gate") {
                        LaunchedEffect(fullName) {
                            if (fullName.isBlank()) {
                                nav.navigate("login") { popUpTo("gate") { inclusive = true } }
                            } else {
                                nav.navigate("scan") { popUpTo("gate") { inclusive = true } }
                            }
                        }
                        CircularProgressIndicator()
                    }

                    composable("login") {
                        NameScreen { name ->
                            userVm.setFullName(name)
                            nav.navigate("scan") { popUpTo("login") { inclusive = true } }
                        }
                    }

                    composable("scan") {
                        ScanScreen { code ->
                            val now = System.currentTimeMillis() // ← старт таймера
                            nav.currentBackStackEntry?.savedStateHandle?.set("barcode", code)
                            nav.currentBackStackEntry?.savedStateHandle?.set("scanStartMs", now)
                            nav.navigate("lookup")
                        }
                    }

                    composable("lookup") {
                        val barcode = nav.previousBackStackEntry?.savedStateHandle?.get<String>("barcode") ?: ""
                        LaunchedEffect(barcode) { gsVm.lookup(barcode) }
                        val linkState by gsVm.linkState.collectAsState()
                        when {
                            linkState == null -> CircularProgressIndicator()
                            linkState!!.isNotEmpty() -> {
                                nav.currentBackStackEntry?.savedStateHandle?.set("scanUrl", linkState!!)
                                nav.navigate("check")
                            }
                            else -> {
                                LinkEditScreen(
                                    barcode = barcode,
                                    initialLink = "",
                                    onSave = { newLink ->
                                        gsVm.save(barcode, newLink, fullName) // ← пишем ФИО в лист 1
                                        nav.currentBackStackEntry?.savedStateHandle?.set("scanUrl", newLink)
                                        nav.navigate("check")
                                    }
                                )
                            }
                        }
                    }

                    composable("check") {
                        val scanUrl = nav.getBackStackEntry("lookup").savedStateHandle.get<String>("scanUrl") ?: ""
                        nav.currentBackStackEntry?.savedStateHandle?.set("scanUrl", scanUrl)
                        CheckScreen(scanUrl) { status, comment, newLink ->
                            nav.currentBackStackEntry?.savedStateHandle?.set("checkResult", Triple(status, comment, newLink))
                            nav.navigate("photo")
                        }
                    }

                    composable("photo") {
                        PhotoScreen { photos ->
                            val list = ArrayList(photos.map { it.toString() })
                            nav.currentBackStackEntry?.savedStateHandle?.set("photos", list)
                            nav.navigate("defect")
                        }
                    }

                    composable("defect") {
                        DefectScreen { hasDefect, desc, qty ->
                            nav.currentBackStackEntry?.savedStateHandle?.set("defectResult", hasDefect to desc)
                            nav.currentBackStackEntry?.savedStateHandle?.set("quantity", qty)
                            nav.navigate("result")
                        }
                    }

                    composable("result") {
                        val barcode = nav.getBackStackEntry("scan").savedStateHandle.get<String>("barcode") ?: ""
                        val scanUrl = nav.getBackStackEntry("lookup").savedStateHandle.get<String>("scanUrl") ?: ""
                        val checkResult = nav.getBackStackEntry("check").savedStateHandle
                            .get<Triple<String, String, String>>("checkResult") ?: Triple("", "", "")
                        val photosStrings = nav.getBackStackEntry("photo").savedStateHandle
                            .get<ArrayList<String>>("photos") ?: arrayListOf()
                        val photos = photosStrings.map { Uri.parse(it) }
                        val defectResult = nav.getBackStackEntry("defect").savedStateHandle
                            .get<Pair<Boolean, String>>("defectResult") ?: (false to "")
                        val quantity = nav.getBackStackEntry("defect").savedStateHandle.get<Int>("quantity") ?: 1
                        val scanStartMs = nav.getBackStackEntry("scan").savedStateHandle.get<Long>("scanStartMs") ?: 0L

                        ResultScreen(
                            context      = LocalContext.current,
                            barcode      = barcode,
                            scanUrl      = scanUrl,
                            checkResult  = checkResult,
                            photos       = photos,
                            defectResult = defectResult,
                            quantity     = quantity,
                            userFullName = fullName,
                            scanStartMs  = scanStartMs,
                            oauthToken   = oauthToken
                        )
                    }
                }
            }
        }
    }
}
