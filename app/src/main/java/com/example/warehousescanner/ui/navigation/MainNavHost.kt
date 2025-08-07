package com.example.warehousescanner.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.warehousescanner.YandexAuth
import com.example.warehousescanner.ui.screens.*
import com.example.warehousescanner.viewmodel.ExcelViewModel

@Composable
fun MainNavHost(nav: NavHostController, excelVm: ExcelViewModel = viewModel()) {
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
                    Box(Modifier.fillMaxSize()) {
                        CircularProgressIndicator(Modifier.align(alignment = androidx.compose.ui.Alignment.Center))
                    }
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
            val photosStrings = nav
                .previousBackStackEntry
                ?.savedStateHandle
                ?.get<ArrayList<String>>("photos")
                ?: arrayListOf()
            val photos = photosStrings.map { Uri.parse(it) }
            DefectScreen { hasDefect, desc ->
                nav.currentBackStackEntry
                    ?.savedStateHandle
                    ?.set("defectResult", hasDefect to desc)
                nav.navigate("result")
            }
        }

        composable("result") {
            val scanUrl = nav
                .getBackStackEntry("lookup")
                .savedStateHandle
                .get<String>("scanUrl")
                ?: ""
            val checkResult = nav
                .getBackStackEntry("check")
                .savedStateHandle
                .get<Pair<String, String>>("checkResult")
                ?: "" to ""
            val photosStrings = nav
                .getBackStackEntry("photo")
                .savedStateHandle
                .get<ArrayList<String>>("photos")
                ?: arrayListOf()
            val photos = photosStrings.map { Uri.parse(it) }
            val defectResult = nav
                .getBackStackEntry("defect")
                .savedStateHandle
                .get<Pair<Boolean, String>>("defectResult")
                ?: false to ""

            ResultScreen(
                context     = LocalContext.current,
                scanUrl     = scanUrl,
                checkResult = checkResult,
                photos      = photos,
                defectResult= defectResult,
                oauthToken  = YandexAuth.token!!
            )
        }
    }
}
