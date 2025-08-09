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
import androidx.navigation.compose.*
import com.example.warehousescanner.data.GoogleSheetClient
import com.example.warehousescanner.ui.screens.*
import com.example.warehousescanner.viewmodel.GoogleSheetViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val oauthToken = "y0__xDG6vflBRituTkgq5zagxS2sGCCdf2L4JbOmOIMoeBfsnOKTw"
        val scriptUrl  = "https://script.google.com/macros/s/AKfycbxHXmvL12Fwj_8pJ93ipuJDtHhTFTdwyXA5LapM0xeEXp1Lbr1QIy2oW3-IVcbWlRDj/exec"
        val apiKey     = "REPLACE_WITH_SECRET"

        // Инициализируем клиент Google Sheet (Apps Script)
        GoogleSheetClient.init(scriptUrl, apiKey)

        setContent {
            val nav = rememberNavController()
            val gsVm: GoogleSheetViewModel = viewModel()

            Surface(
                modifier = Modifier.fillMaxSize().systemBarsPadding(),
                color = MaterialTheme.colors.background
            ) {
                NavHost(navController = nav, startDestination = "scan") {
                    composable("scan") {
                        ScanScreen { code ->
                            nav.currentBackStackEntry?.savedStateHandle?.set("barcode", code)
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
                                        gsVm.save(barcode, newLink)
                                    }
                                )
                            }
                        }
                    }
                    composable("check") {
                        val barcode = nav.getBackStackEntry("scan").savedStateHandle.get<String>("barcode") ?: ""
                        val scanUrl = nav.previousBackStackEntry?.savedStateHandle?.get<String>("scanUrl") ?: ""
                        CheckScreen(scanUrl) { status, comment ->
                            nav.currentBackStackEntry?.savedStateHandle?.set("checkResult", status to comment)
                            nav.currentBackStackEntry?.savedStateHandle?.set("barcode", barcode)
                            nav.navigate("photo")
                        }
                    }
                    composable("photo") {
                        val checkResult = nav.previousBackStackEntry?.savedStateHandle?.get<Pair<String, String>>("checkResult") ?: ("" to "")
                        val barcode = nav.previousBackStackEntry?.savedStateHandle?.get<String>("barcode") ?: ""
                        PhotoScreen { photos ->
                            val list = ArrayList(photos.map { it.toString() })
                            nav.currentBackStackEntry?.savedStateHandle?.set("photos", list)
                            nav.currentBackStackEntry?.savedStateHandle?.set("barcode", barcode)
                            nav.navigate("defect")
                        }
                    }
                    composable("defect") {
                        val photos = nav.previousBackStackEntry?.savedStateHandle?.get<ArrayList<String>>("photos")?.map { Uri.parse(it) } ?: emptyList()
                        val barcode = nav.previousBackStackEntry?.savedStateHandle?.get<String>("barcode") ?: ""
                        DefectScreen { hasDefect, desc ->
                            nav.currentBackStackEntry?.savedStateHandle?.set("defectResult", hasDefect to desc)
                            nav.currentBackStackEntry?.savedStateHandle?.set("barcode", barcode)
                            nav.navigate("result")
                        }
                    }
                    composable("result") {
                        val scanUrl = nav.getBackStackEntry("check").savedStateHandle.get<String>("scanUrl") ?: ""
                        val checkResult = nav.getBackStackEntry("photo").savedStateHandle.get<Pair<String, String>>("checkResult") ?: ("" to "")
                        val photos = nav.getBackStackEntry("defect").savedStateHandle.get<ArrayList<String>>("photos")?.map { Uri.parse(it) } ?: emptyList()
                        val defectResult = nav.getBackStackEntry("defect").savedStateHandle.get<Pair<Boolean, String>>("defectResult") ?: (false to "")
                        val barcode = nav.getBackStackEntry("defect").savedStateHandle.get<String>("barcode") ?: ""

                        ResultScreen(
                            context     = LocalContext.current,
                            barcode     = barcode,
                            scanUrl     = scanUrl,
                            checkResult = checkResult,
                            photos      = photos,
                            defectResult= defectResult,
                            oauthToken  = oauthToken
                        )
                    }
                }
            }
        }
    }
}
