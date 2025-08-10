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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.warehousescanner.data.GoogleSheetClient
import com.example.warehousescanner.ui.screens.*
import com.example.warehousescanner.viewmodel.GoogleSheetViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val oauthToken = "y0__xDG6vflBRituTkgq5zagxS2sGCCdf2L4JbOmOIMoeBfsnOKTw"
        val scriptUrl  = "https://script.google.com/macros/s/AKfycbwlNlY1qpBRq-9i1Ns_tRogIHupiuJS2ggBsvA3vHBFByhTDtocW_jdnfo7DMeWKVvD/exec"
        val apiKey     = "REPLACE_WITH_SECRETT"

        GoogleSheetClient.init(scriptUrl, apiKey)

        setContent {
            val nav = rememberNavController()
            val gsVm: GoogleSheetViewModel = viewModel()

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
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
                        val barcode = nav.previousBackStackEntry
                            ?.savedStateHandle
                            ?.get<String>("barcode") ?: ""
                        LaunchedEffect(barcode) { gsVm.lookup(barcode) }
                        val linkState by gsVm.linkState.collectAsState()
                        when {
                            linkState == null -> {
                                CircularProgressIndicator()
                            }
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

                                        nav.currentBackStackEntry?.savedStateHandle?.set("scanUrl", newLink)
                                        nav.navigate("check")
                                    }
                                )
                            }
                        }
                    }

                    composable("check") {

                        val scanUrl = nav
                            .getBackStackEntry("lookup")
                            .savedStateHandle
                            .get<String>("scanUrl") ?: ""
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
                        DefectScreen { hasDefect, desc ->
                            nav.currentBackStackEntry
                                ?.savedStateHandle
                                ?.set("defectResult", hasDefect to desc)
                            nav.navigate("result")
                        }
                    }

                    composable("result") {
                        val barcode = nav
                            .getBackStackEntry("scan")
                            .savedStateHandle
                            .get<String>("barcode") ?: ""

                        val scanUrl = nav
                            .getBackStackEntry("lookup")
                            .savedStateHandle
                            .get<String>("scanUrl") ?: ""

                        val checkResult = nav
                            .getBackStackEntry("check")
                            .savedStateHandle
                            .get<Triple<String, String, String>>("checkResult")
                            ?: Triple("", "", "")


                        val photosStrings = nav
                            .getBackStackEntry("photo")
                            .savedStateHandle
                            .get<ArrayList<String>>("photos") ?: arrayListOf()
                        val photos = photosStrings.map { Uri.parse(it) }

                        val defectResult = nav
                            .getBackStackEntry("defect")
                            .savedStateHandle
                            .get<Pair<Boolean, String>>("defectResult") ?: (false to "")

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
