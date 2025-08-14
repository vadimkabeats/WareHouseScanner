package com.example.warehousescanner.ui.navigation

import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.warehousescanner.YandexAuth
import com.example.warehousescanner.ui.screens.*
import com.example.warehousescanner.viewmodel.GoogleSheetViewModel
import com.example.warehousescanner.viewmodel.SessionViewModel
import com.example.warehousescanner.viewmodel.UserViewModel

@SuppressLint("ContextCastToActivity")
@Composable
fun MainNavHost(
    nav: NavHostController,
    gsVm: GoogleSheetViewModel = viewModel()
) {
    val activity = LocalContext.current as ComponentActivity
    val session: SessionViewModel = viewModel(activity)
    val userVm: UserViewModel = viewModel(activity)
    val fullName by userVm.fullName.collectAsState()

    NavHost(navController = nav, startDestination = "scan") {

        composable("scan") {
            ScanScreen { code ->
                session.setBarcode(code)
                session.markScanStart() // старт таймера
                nav.navigate("lookup")
            }
        }

        composable("lookup") {
            val barcode by session.barcode.collectAsState()
            LaunchedEffect(barcode) { if (barcode.isNotBlank()) gsVm.lookup(barcode) }
            val linkState by gsVm.linkState.collectAsState()

            when (linkState) {
                null -> Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
                "" -> LinkEditScreen(
                    barcode = barcode,
                    initialLink = "",
                    onSave = { newLink ->
                        gsVm.save(barcode, newLink, fullName) // пишем ФИО в лист1
                        session.setUrl(newLink)
                        nav.navigate("check")
                    }
                )
                else -> {
                    session.setUrl(linkState!!)
                    nav.navigate("check")
                }
            }
        }

        composable("check") {
            val scanUrl by session.url.collectAsState()
            CheckScreen(scanUrl) { status, comment, newLink ->
                session.setCheckResult(status, comment)
                session.setNewLink(newLink)
                nav.navigate("photo")
            }
        }

        composable("photo") {
            PhotoScreen { picked ->
                session.setPhotos(picked)
                nav.navigate("defect")
            }
        }

        composable("defect") {
            DefectScreen { hasDefect, desc, qty ->
                session.setDefect(hasDefect, desc)
                session.setQuantity(qty)
                nav.navigate("result")
            }
        }

        composable("result") {
            val barcode       by session.barcode.collectAsState()
            val scanUrl       by session.url.collectAsState()
            val checkStatus   by session.checkStatus.collectAsState()
            val checkComment  by session.checkComment.collectAsState()
            val newLink       by session.newLink.collectAsState()
            val photos        by session.photos.collectAsState()
            val hasDefect     by session.hasDefect.collectAsState()
            val defectDesc    by session.defectDesc.collectAsState()
            val quantity      by session.quantity.collectAsState()
            val scanStartMs   by session.scanStartMs.collectAsState()

            ResultScreen(
                context      = LocalContext.current,
                barcode      = barcode,
                scanUrl      = scanUrl,
                checkResult  = Triple(checkStatus, checkComment, newLink),
                photos       = photos,
                defectResult = hasDefect to defectDesc,
                quantity     = quantity,
                userFullName = fullName,
                scanStartMs  = scanStartMs,
                oauthToken   = YandexAuth.token!!
            )
        }
    }
}
