package com.example.warehousescanner.ui.navigation

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.warehousescanner.data.GoogleSheetClient
import com.example.warehousescanner.ui.screens.*
import com.example.warehousescanner.viewmodel.*
import kotlinx.coroutines.launch

@SuppressLint("ContextCastToActivity")
@Composable
fun MainNavHost(
    nav: NavHostController,
    oauthToken: String,
    gsVm: GoogleSheetViewModel = viewModel()
) {
    val activity = LocalContext.current as ComponentActivity
    val session: SessionViewModel = viewModel(activity)
    val userVm: UserViewModel = viewModel(activity)
    val settingsVm: SettingsViewModel = viewModel(activity)
    val fullName by userVm.fullName.collectAsState()
    val torchOn by settingsVm.torchEnabled.collectAsState()

    NavHost(navController = nav, startDestination = "login") {

        /* ------------ ЛОГИН ------------ */
        composable("login") {
            var err by remember { mutableStateOf<String?>(null) }
            var loading by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            LoginScreen(
                onLogin = { first, last, pass ->
                    if (loading) return@LoginScreen
                    loading = true
                    err = null
                    scope.launch {
                        runCatching { GoogleSheetClient.auth(first, last, pass) }
                            .onSuccess { resp ->
                                if (resp.ok && !resp.fio.isNullOrBlank()) {
                                    userVm.setFullName(resp.fio!!)
                                    nav.navigate("home") { popUpTo("login") { inclusive = true } }
                                } else {
                                    err = when (resp.error) {
                                        "invalid_credentials" -> "Неверные фамилия/имя/пароль"
                                        "accounts_sheet_missing" -> "Лист «Аккаунты» не найден"
                                        else -> "Не удалось войти"
                                    }
                                }
                            }
                            .onFailure { e -> err = e.localizedMessage ?: "Ошибка сети" }
                        loading = false
                    }
                },
                errorText = err,
                isLoading = loading
            )
        }

        /* ------------ ГЛАВНОЕ МЕНЮ ------------ */
        composable("home") {
            HomeScreen(
                onAddItem = { nav.navigate("scan") },
                onPutAway = { nav.navigate("put_scan_item") },
                onPrintLabel = { nav.navigate("print_scan") },
                onReceiveReturn = { nav.navigate("return_scan") },
                torchOn = torchOn,
                onToggleTorch = { settingsVm.setTorchEnabled(it) }
            )
        }

        /* ------------ ДОБАВИТЬ ТОВАР ------------ */
        composable("scan") {
            ScanScreen(
                instanceKey = "add_item",
                torchOn = torchOn
            ) { code ->
                gsVm.reset()
                session.setUrl("")
                session.setNewLink("")
                session.setCheckResult("", "")
                session.setPhotos(emptyList())
                session.setDefect(false, "")
                session.setQuantity(0)

                session.setBarcode(code)
                session.markScanStart()
                nav.navigate("lookup")
            }
        }

        composable("lookup") {
            val barcode by session.barcode.collectAsState()
            val linkState by gsVm.linkState.collectAsState()
            val scope = rememberCoroutineScope()

            var checking by remember(barcode) { mutableStateOf(true) }
            var askOverwrite by remember(barcode) { mutableStateOf(false) }
            var checkError by remember(barcode) { mutableStateOf<String?>(null) }

            LaunchedEffect(barcode) {
                if (barcode.isBlank()) return@LaunchedEffect
                checking = true
                checkError = null
                askOverwrite = false
                runCatching { GoogleSheetClient.scanExists(barcode) }
                    .onSuccess { ex ->
                        if (ex) {
                            askOverwrite = true
                            checking = false
                        } else {
                            gsVm.lookup(barcode)
                            checking = false
                        }
                    }
                    .onFailure { e ->
                        checkError = e.localizedMessage ?: "Ошибка проверки дубля"
                        checking = false
                    }
            }

            if (askOverwrite) {
                AlertDialog(
                    onDismissRequest = { },
                    title = { Text("Повторный товар") },
                    text = { Text("Этот товар уже есть в базе. Перезаписать информацию?") },
                    confirmButton = {
                        Button(onClick = {
                            askOverwrite = false
                            checking = true
                            scope.launch {
                                gsVm.lookup(barcode)
                                checking = false
                            }
                        }) { Text("Перезаписать") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            val popped = nav.popBackStack("home", false)
                            if (!popped) {
                                nav.navigate("home") {
                                    popUpTo(nav.graph.startDestinationId) { inclusive = false }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }) { Text("Оставить старое") }
                    }
                )
            }

            when {
                checking -> Box(Modifier.fillMaxSize()) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                checkError != null -> Box(Modifier.fillMaxSize()) {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Ошибка: $checkError")
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { nav.popBackStack() }) { Text("Назад") }
                    }
                }
                else -> {
                    when (val link = linkState) {
                        null -> Box(Modifier.fillMaxSize()) {
                            CircularProgressIndicator(Modifier.align(Alignment.Center))
                        }
                        "" -> {
                            LaunchedEffect(Unit) {
                                session.setUrl("")
                                nav.navigate("check?allowMatch=false") {
                                    popUpTo("lookup") { inclusive = true }
                                }
                            }
                            Box(Modifier.fillMaxSize()) { }
                        }
                        else -> {
                            LaunchedEffect(link) {
                                session.setUrl(link)
                                nav.navigate("check?allowMatch=true") {
                                    popUpTo("lookup") { inclusive = true }
                                }
                            }
                            Box(Modifier.fillMaxSize()) {
                                CircularProgressIndicator(Modifier.align(Alignment.Center))
                            }
                        }
                    }
                }
            }
        }

        composable(
            route = "check?allowMatch={allowMatch}",
            arguments = listOf(navArgument("allowMatch"){ type = NavType.BoolType; defaultValue = true })
        ) { backStackEntry ->
            val scanUrl by session.url.collectAsState()
            val allowMatch = backStackEntry.arguments?.getBoolean("allowMatch") ?: true

            CheckScreen(url = scanUrl, allowMatch = allowMatch) { status, comment, newLink ->
                session.setCheckResult(status, comment)
                session.setNewLink(newLink)

                if (status == "nlo") {
                    session.setPhotos(emptyList())
                    session.setDefect(false, "")
                    session.setQuantity(0)
                    nav.navigate("result")
                } else {
                    nav.navigate("photo")
                }
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
            val barcode by session.barcode.collectAsState()
            val scanUrl by session.url.collectAsState()
            val checkStatus by session.checkStatus.collectAsState()
            val checkComment by session.checkComment.collectAsState()
            val newLink by session.newLink.collectAsState()
            val photos by session.photos.collectAsState()
            val hasDefect by session.hasDefect.collectAsState()
            val defectDesc by session.defectDesc.collectAsState()
            val quantity by session.quantity.collectAsState()
            val scanStartMs by session.scanStartMs.collectAsState()

            fun goHomeSafe() {
                val popped = nav.popBackStack("home", false)
                if (!popped) {
                    nav.navigate("home") {
                        popUpTo(nav.graph.startDestinationId) { inclusive = false }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }

            ResultScreen(
                context = LocalContext.current,
                barcode = barcode,
                scanUrl = scanUrl,
                checkResult = Triple(checkStatus, checkComment, newLink),
                photos = photos,
                defectResult = hasDefect to defectDesc,
                quantity = quantity,
                userFullName = fullName,
                scanStartMs = scanStartMs,
                oauthToken = oauthToken,
                onBackHome = { goHomeSafe() }
            )
        }

        /* ------------ ПОЛОЖИТЬ ТОВАР ------------ */
        composable("put_scan_item") {
            ScanScreen(
                instanceKey = "put_item",
                torchOn = torchOn
            ) { itemCode ->
                val itemArg = Uri.encode(itemCode)
                val startArg = System.currentTimeMillis()
                nav.navigate("put_scan_cell?item=$itemArg&start=$startArg") {
                    popUpTo("put_scan_item") { inclusive = true }
                }
            }
        }

        composable(
            route = "put_scan_cell?item={item}&start={start}",
            arguments = listOf(
                navArgument("item")  { defaultValue = "" },
                navArgument("start") { type = NavType.LongType; defaultValue = 0L }
            )
        ) { backStackEntry ->
            val item = backStackEntry.arguments?.getString("item").orEmpty()
            val start = backStackEntry.arguments?.getLong("start") ?: 0L

            ScanScreen(
                instanceKey = "put_cell",
                allowManualInput = true,
                inputHint = "Адрес ячейки (4–5 символов)",
                validator = { s ->
                    val v = s.trim()
                    if (v.length in 4..5) null else "Нужно отсканировать адрес ячейки (4–5 символов)"
                },
                torchOn = torchOn
            ) { cellCode ->
                val itemArg = Uri.encode(item)
                val cellArg = Uri.encode(cellCode)
                nav.navigate("put_done?item=$itemArg&cell=$cellArg&start=$start")
            }
        }

        composable(
            route = "put_done?item={item}&cell={cell}&start={start}",
            arguments = listOf(
                navArgument("item")  { defaultValue = "" },
                navArgument("cell")  { defaultValue = "" },
                navArgument("start") { type = NavType.LongType; defaultValue = 0L }
            )
        ) { backStackEntry ->
            val item  = backStackEntry.arguments?.getString("item").orEmpty()
            val cell  = backStackEntry.arguments?.getString("cell").orEmpty()
            val start = backStackEntry.arguments?.getLong("start") ?: 0L

            var loading by remember { mutableStateOf(true) }
            var msg by remember { mutableStateOf("") }

            LaunchedEffect(item, cell, start, fullName) {
                val now = System.currentTimeMillis()
                val durationSec = (((now - start).coerceAtLeast(0L)) / 1000L).toInt()
                val userFio = fullName

                runCatching {
                    GoogleSheetClient.putAway(item, cell, durationSec, userFio)
                }.onSuccess { res ->
                    msg = if (res.updated == true) {
                        "Обновлено: товар $item → новая ячейка $cell (время: ${durationSec}с)"
                    } else {
                        "Сохранено: товар $item → ячейка $cell (время: ${durationSec}с)"
                    }
                }.onFailure { e ->
                    msg = "Ошибка: ${e.localizedMessage}"
                }
                loading = false
            }

            PutDoneScreen(
                isLoading = loading,
                message = msg,
                onBackHome = {
                    val popped = nav.popBackStack("home", false)
                    if (!popped) {
                        nav.navigate("home") {
                            popUpTo(nav.graph.startDestinationId) { inclusive = false }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }

        /* ------------ ПЕЧАТЬ ЭТИКЕТКИ (обычная) ------------ */
        composable("print_scan") {
            LaunchedEffect(Unit) { gsVm.resetTrack() }
            ScanScreen(
                instanceKey = "print_scan",
                torchOn = torchOn
            ) { code ->
                val arg = Uri.encode(code)
                nav.navigate("print_preview?code=$arg")
            }
        }

        composable(
            route = "print_preview?code={code}",
            arguments = listOf(navArgument("code") { type = NavType.StringType; defaultValue = "" })
        ) { backStackEntry ->
            val code = backStackEntry.arguments?.getString("code").orEmpty()
            PrintPreviewScreen(
                code = code,
                onBack = { nav.popBackStack() }
            )
        }

        /* ------------ ПРИНЯТЬ ВОЗВРАТ (ОБНОВЛЁН) ------------ */

        // 1) Сканируем dispatchNumber
        composable("return_scan") {
            val returnVm: ReturnViewModel = viewModel(activity)
            ScanScreen(
                instanceKey = "return_scan",
                allowManualInput = true,
                inputHint = "Введите/отсканируйте Dispatch №",
                torchOn = torchOn
            ) { dispatch ->
                returnVm.reset()
                returnVm.setDispatchNumber(dispatch)
                val arg = Uri.encode(dispatch)
                nav.navigate("return_resolve?dispatch=$arg")
            }
        }

        // 1a) Резолвим barcode по dispatchNumber
        composable(
            route = "return_resolve?dispatch={dispatch}",
            arguments = listOf(navArgument("dispatch"){ defaultValue = "" })
        ) { backStackEntry ->
            val returnVm: ReturnViewModel = viewModel(activity)
            val dispatch = backStackEntry.arguments?.getString("dispatch").orEmpty()

            var state by remember { mutableStateOf("loading") }
            var err by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(dispatch) {
                state = "loading"
                err = null
                runCatching { GoogleSheetClient.returnLookup(dispatch) }
                    .onSuccess { resp ->
                        if (resp.ok && resp.found && !resp.barcode.isNullOrBlank()) {
                            returnVm.setDispatchNumber(dispatch)
                            returnVm.setPrintBarcode(resp.barcode)
                            state = "ok"
                        } else {
                            err = "Не найдено в листе «Возвраты»"
                            state = "error"
                        }
                    }
                    .onFailure { e ->
                        err = e.localizedMessage ?: "Ошибка сети"
                        state = "error"
                    }
            }

            when (state) {
                "loading" -> Box(Modifier.fillMaxSize()) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                "ok" -> LaunchedEffect(Unit) {
                    nav.navigate("return_condition") {
                        popUpTo("return_resolve?dispatch={dispatch}") { inclusive = true }
                    }
                }
                else -> {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(err ?: "Ошибка")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { nav.popBackStack() }) { Text("Назад") }
                    }
                }
            }
        }

        // 2) Состояние, фото
        composable("return_condition") {
            val returnVm: ReturnViewModel = viewModel(activity)
            val dispatch by returnVm.dispatchNumber.collectAsState()
            val barcodeToPrint by returnVm.printBarcode.collectAsState()
            val hasDefect by returnVm.hasDefect.collectAsState()
            val defectDesc by returnVm.defectDesc.collectAsState()
            val photos by returnVm.photos.collectAsState()

            ReturnConditionScreen(
                dispatchNumber = dispatch,
                printBarcode = barcodeToPrint,
                hasDefectInit = hasDefect,
                defectDescInit = defectDesc,
                photosCount = photos.size,
                onChangeState = { has, desc -> returnVm.setDefect(has, desc) },
                onOpenPhotos = { nav.navigate("return_photos") },
                onNext = { nav.navigate("return_result") },
                onBack = { nav.popBackStack() }
            )
        }

        // 2a) Фото
        composable("return_photos") {
            val returnVm: ReturnViewModel = viewModel(activity)
            PhotoScreen { picked ->
                returnVm.setPhotos(picked)
                nav.popBackStack()
            }
        }

        // 2b) Отправка в Диск + запись в таблицу
        composable("return_result") {
            val returnVm: ReturnViewModel = viewModel(activity)
            val dispatch by returnVm.dispatchNumber.collectAsState()
            val barcodeToPrint by returnVm.printBarcode.collectAsState()
            val hasDefect by returnVm.hasDefect.collectAsState()
            val defectDesc by returnVm.defectDesc.collectAsState()
            val photos by returnVm.photos.collectAsState()

            ReturnResultScreen(
                context = LocalContext.current,
                dispatchNumber = dispatch,
                printBarcode = barcodeToPrint,
                hasDefect = hasDefect,
                defectDesc = defectDesc,
                photos = photos,
                userFullName = fullName,
                oauthToken = oauthToken,
                onNextToPrint = { nav.navigate("return_print") },
                onBackHome = {
                    val popped = nav.popBackStack("home", false)
                    if (!popped) {
                        nav.navigate("home") {
                            popUpTo(nav.graph.startDestinationId) { inclusive = false }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }

        // 3) Печать (показываем dispatch+barcode, печатаем barcode)
        composable("return_print") {
            val returnVm: ReturnViewModel = viewModel(activity)
            val dispatch by returnVm.dispatchNumber.collectAsState()
            val barcodeToPrint by returnVm.printBarcode.collectAsState()
            val hasDefect by returnVm.hasDefect.collectAsState()
            val defectDesc by returnVm.defectDesc.collectAsState()
            val photos by returnVm.photos.collectAsState()

            ReturnPrintScreen(
                dispatchNumber = dispatch,
                barcodeToPrint = barcodeToPrint,
                hasDefect = hasDefect,
                defectDesc = defectDesc,
                photosCount = photos.size,
                onBack = { nav.popBackStack() },
                onBackHome = {
                    val popped = nav.popBackStack("home", false)
                    if (!popped) {
                        nav.navigate("home") {
                            popUpTo(nav.graph.startDestinationId) { inclusive = false }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}
