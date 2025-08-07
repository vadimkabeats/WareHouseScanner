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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.warehousescanner.ui.screens.*
import com.example.warehousescanner.viewmodel.ExcelViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val oauthToken = "<YOUR_OAUTH_TOKEN>"

        setContent {
            val nav = rememberNavController()
            val excelVm: ExcelViewModel = viewModel()
            val fileUri by excelVm.fileUriState.collectAsState()

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
                color = MaterialTheme.colors.background
            ) {
                if (fileUri == null) {
                    FilePickerScreen { uri ->
                        excelVm.setFile(uri)
                    }
                } else {
                    NavHost(navController = nav, startDestination = "scan") {
                        composable("scan") {
                            ScanScreen { code ->
                                nav.currentBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("barcode", code)
                                nav.navigate("lookup")
                            }
                        }
                        composable("lookup") {
                            val barcode = nav
                                .previousBackStackEntry
                                ?.savedStateHandle
                                ?.get<String>("barcode")
                                ?: ""
                            LaunchedEffect(barcode) { excelVm.lookup(barcode) }
                            val linkState by excelVm.linkState.collectAsState()
                            when {
                                linkState == null -> {
                                    CircularProgressIndicator()
                                }
                                linkState!!.isNotEmpty() -> {
                                    nav.currentBackStackEntry
                                        ?.savedStateHandle
                                        ?.set("scanUrl", linkState!!)
                                    nav.navigate("check")
                                }
                                else -> {
                                    LinkEditScreen(
                                        barcode = barcode,
                                        initialLink = "",
                                        onSave = { newLink ->
                                            excelVm.save(barcode, newLink)
                                        }
                                    )
                                }
                            }
                        }
                        composable("check") {
                            val scanUrl = nav
                                .previousBackStackEntry
                                ?.savedStateHandle
                                ?.get<String>("scanUrl")
                                ?: ""
                            CheckScreen(scanUrl) { status, comment ->
                                nav.currentBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("checkResult", status to comment)
                                nav.navigate("photo")
                            }
                        }
                        composable("photo") {
                            val checkResult = nav
                                .previousBackStackEntry
                                ?.savedStateHandle
                                ?.get<Pair<String, String>>("checkResult")
                                ?: "" to ""
                            PhotoScreen { photos ->
                                val list = ArrayList(photos.map { it.toString() })
                                nav.currentBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("photos", list)
                                nav.navigate("defect")
                            }
                        }
                        composable("defect") {
                            val photos = nav
                                .previousBackStackEntry
                                ?.savedStateHandle
                                ?.get<ArrayList<String>>("photos")
                                ?.map { Uri.parse(it) }
                                ?: emptyList()
                            DefectScreen { hasDefect, desc ->
                                nav.currentBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("defectResult", hasDefect to desc)
                                nav.navigate("result")
                            }
                        }
                        composable("result") {
                            val scanUrl = nav
                                .getBackStackEntry("check")
                                .savedStateHandle
                                .get<String>("scanUrl")
                                ?: ""
                            val checkResult = nav
                                .getBackStackEntry("check")
                                .savedStateHandle
                                .get<Pair<String, String>>("checkResult")
                                ?: "" to ""
                            val photos = nav
                                .getBackStackEntry("photo")
                                .savedStateHandle
                                .get<ArrayList<String>>("photos")
                                ?.map { Uri.parse(it) }
                                ?: emptyList()
                            val defectResult = nav
                                .getBackStackEntry("defect")
                                .savedStateHandle
                                .get<Pair<Boolean, String>>("defectResult")
                                ?: false to ""

                            ResultScreen(
                                context = LocalContext.current,
                                scanUrl = scanUrl,
                                checkResult = checkResult,
                                photos = photos,
                                defectResult = defectResult,
                                oauthToken = oauthToken
                            )
                        }
                    }
                }
            }
        }
    }
}
