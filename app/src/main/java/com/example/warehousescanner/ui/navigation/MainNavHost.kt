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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.warehousescanner.data.GoogleSheetClient
import com.example.warehousescanner.data.ReturnLookupItem
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

        composable("home") {
            val activity = LocalContext.current as ComponentActivity
            val userVm: UserViewModel = viewModel(activity)
            val settingsVm: SettingsViewModel = viewModel(activity)

            val fullName by userVm.fullName.collectAsState()
            val torchOn by settingsVm.torchEnabled.collectAsState()

            // --- статистика за сегодня
            val statsVm: StatsViewModel = viewModel(activity)
            val statsLoading by statsVm.loading.collectAsState()
            val statsNlo by statsVm.nlo.collectAsState()
            val statsNonNlo by statsVm.nonNlo.collectAsState()

            // персональная статистика
            val statsIdentified by statsVm.identified.collectAsState()
            val statsPutAway by statsVm.putAway.collectAsState()
            val statsLost by statsVm.lost.collectAsState()

            // СУММАРНАЯ статистика по складу
            val totalIdentified by statsVm.totalIdentified.collectAsState()
            val totalPutAway by statsVm.totalPutAway.collectAsState()

            val lostVm: LostItemsViewModel = viewModel(activity)
            val lostItems by lostVm.items.collectAsState()
            val lostItemsLoading by lostVm.loading.collectAsState()
            val lostItemsError by lostVm.error.collectAsState()
            var lostDialogVisible by remember { mutableStateOf(false) }

            if (lostDialogVisible) {
                LostItemsDialog(
                    items = lostItems,
                    loading = lostItemsLoading,
                    error = lostItemsError,
                    onRetry = { lostVm.load(fullName) },
                    onDismiss = { lostDialogVisible = false }
                )
            }
            // первая загрузка
            LaunchedEffect(fullName) {
                statsVm.loadFor(fullName)
            }

            // перезагрузка при возврате на экран
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner, fullName) {
                val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
                    if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        statsVm.loadFor(fullName)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(obs)
                onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
            }
            HomeScreen(
                onAddItem = { nav.navigate("scan") },
                onPutAway = { nav.navigate("put_scan_item") },
                onPrintLabel = { nav.navigate("print_scan") },
                onReceiveReturn = { nav.navigate("return_scan") },
                onReconcile = { nav.navigate("reconcile_home") },

                // 1-я карточка (НЛО)
                statsNonNlo = statsNonNlo,
                statsNlo = statsNlo,
                statsLoading = statsLoading,

                // 2-я карточка (по конкретному работнику — «ты»)
                statsIdentified = statsIdentified,
                statsPutAway = statsPutAway,
                statsLost = statsLost,

                // 3-я карточка — суммарная по складу
                totalIdentified = totalIdentified,
                totalPutAway = totalPutAway,

                // клик по "НЕ дошло до полки"
                onShowLostDetails = {
                    lostDialogVisible = true
                    lostVm.load(fullName)
                },

                // фонарик
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
                session.setStrongPackaging(false)
                session.setBarcode(code)
                session.markScanStart()
                session.setToUtil(false)
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
                    nav.navigate("id_print")
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
            DefectScreen { hasDefect, desc, qty, strongPackaging, toUtil ->
                session.setDefect(hasDefect, desc)
                session.setQuantity(qty)
                session.setStrongPackaging(strongPackaging)
                session.setToUtil(toUtil)           // ← сохраняем флаг "В утиль"
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
            val strongPackaging by session.strongPackaging.collectAsState()
            val toUtil by session.toUtil.collectAsState()

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
                strongPackaging = strongPackaging,
                toUtil = toUtil,
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

        composable("return_scan") {
            val returnVm: ReturnViewModel = viewModel(activity)
            ScanScreen(
                instanceKey = "return_scan",
                allowManualInput = true,
                inputHint = "Введите/отсканируйте Dispatch №",
                torchOn = torchOn,
                manualAllowSpaces = true          // ← ДОБАВИЛИ: разрешаем пробелы в ручном вводе
            ) { dispatch ->
                returnVm.reset()
                returnVm.setDispatchNumber(dispatch)
                val arg = Uri.encode(dispatch)
                nav.navigate("return_resolve?dispatch=$arg")
            }
        }

        // 1a) Резолвим ВСЕ товары по dispatchNumber
        composable(
            route = "return_resolve?dispatch={dispatch}",
            arguments = listOf(navArgument("dispatch"){ defaultValue = "" })
        ) { backStackEntry ->
            val returnVm: ReturnViewModel = viewModel(activity)
            val dispatch = backStackEntry.arguments?.getString("dispatch").orEmpty()

            val items by returnVm.items.collectAsState()

            var state by remember { mutableStateOf("loading") } // loading / single / multi / error
            var err by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(dispatch) {
                state = "loading"
                err = null
                returnVm.reset()
                runCatching { GoogleSheetClient.returnLookup(dispatch) }
                    .onSuccess { resp ->
                        if (resp.ok && resp.found) {
                            val list = when {
                                !resp.items.isNullOrEmpty() -> resp.items
                                !resp.barcode.isNullOrBlank() -> listOf(
                                    ReturnLookupItem(
                                        barcode = resp.barcode,
                                        title = null,
                                        url = resp.url,
                                        reason = resp.reason
                                    )
                                )
                                else -> emptyList()
                            }

                            if (list.isEmpty()) {
                                err = "Не найдено в листе «Возвраты»"
                                state = "error"
                            } else {
                                returnVm.setDispatchNumber(dispatch)
                                returnVm.setItems(list)
                                if (list.size == 1) {
                                    returnVm.selectItem(0)
                                    state = "single"
                                } else {
                                    state = "multi"
                                }
                            }
                        } else {
                            err = resp.error ?: "Не найдено в листе «Возвраты»"
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

                "multi" -> {
                    ReturnPickItemScreen(
                        dispatchNumber = dispatch,
                        items = items,
                        onSelectItem = { idx ->
                            returnVm.selectItem(idx)
                            nav.navigate("return_condition") {
                                popUpTo("return_resolve?dispatch={dispatch}") { inclusive = true }
                            }
                        },
                        onBack = { nav.popBackStack() }
                    )
                }

                "single" -> {
                    LaunchedEffect(Unit) {
                        nav.navigate("return_condition") {
                            popUpTo("return_resolve?dispatch={dispatch}") { inclusive = true }
                        }
                    }
                }

                else -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
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


        // 2) Состояние, фото + ВЫБОР ДЕЙСТВИЯ (НОВОЕ)
        composable("return_condition") {
            val returnVm: ReturnViewModel = viewModel(activity)
            val dispatch by returnVm.dispatchNumber.collectAsState()
            val barcodeToPrint by returnVm.printBarcode.collectAsState()
            val reason by returnVm.returnReason.collectAsState()
            val productUrl by returnVm.productUrl.collectAsState()
            val hasDefect by returnVm.hasDefect.collectAsState()
            val defectDesc by returnVm.defectDesc.collectAsState()
            val photos by returnVm.photos.collectAsState()
            val decision by returnVm.decision.collectAsState()   // ← НОВОЕ (decision)

            ReturnConditionScreen(
                dispatchNumber = dispatch,
                printBarcode = barcodeToPrint,
                reason = reason,
                productUrl = productUrl,
                hasDefectInit = hasDefect,
                defectDescInit = defectDesc,
                photosCount = photos.size,
                decisionInit = decision,                           // ← НОВОЕ
                onChangeState = { has, desc ->
                    returnVm.setDefect(has, desc)
                    if (!has) returnVm.setPhotos(emptyList())
                },
                onSelectDecision = { returnVm.setDecision(it) },   // ← НОВОЕ
                onOpenPhotos = { if (hasDefect) nav.navigate("return_photos") },
                onNext = {
                    if (hasDefect && photos.isEmpty()) return@ReturnConditionScreen
                    nav.navigate("return_result")
                },
                onBack = { nav.popBackStack() }
            )
        }


        // 2a) Фото — защита от прямого попадания без дефекта
        composable("return_photos") {
            val returnVm: ReturnViewModel = viewModel(activity)
            val hasDefect by returnVm.hasDefect.collectAsState()
            LaunchedEffect(hasDefect) {
                if (!hasDefect) nav.popBackStack()
            }
            PhotoScreen { picked ->
                returnVm.setPhotos(picked)
                nav.popBackStack()
            }
        }

        // 2b) Отправка в Диск + запись в таблицу
        // 2b) Отправка в Диск + запись в таблицу
        composable("return_result") {
            val returnVm: ReturnViewModel = viewModel(activity)
            val dispatch by returnVm.dispatchNumber.collectAsState()
            val barcodeToPrint by returnVm.printBarcode.collectAsState()
            val hasDefect by returnVm.hasDefect.collectAsState()
            val defectDesc by returnVm.defectDesc.collectAsState()
            val photos by returnVm.photos.collectAsState()
            val decision by returnVm.decision.collectAsState()   // ← НОВОЕ

            ReturnResultScreen(
                context = LocalContext.current,
                dispatchNumber = dispatch,
                printBarcode = barcodeToPrint,
                hasDefect = hasDefect,
                defectDesc = defectDesc,
                photos = photos,
                userFullName = fullName,
                oauthToken = oauthToken,
                decision = decision,                              // ← НОВОЕ
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

        /* ------------ СВЕРКА С КУРЬЕРОМ (НОВОЕ) ------------ */
        composable("reconcile_home") {
            val rvm: ReconcileViewModel = viewModel(activity)
            val loading by rvm.loading.collectAsState()
            val error by rvm.error.collectAsState()
            val expected by rvm.expected.collectAsState()
            val scanned by rvm.scanned.collectAsState()

            LaunchedEffect(Unit) { rvm.load() }

            ReconcileHomeScreen(
                isLoading = loading,
                error = error,
                expected = expected,
                scannedCount = scanned.size,
                onScan = { nav.navigate("reconcile_scan") },
                onBrowse = { nav.navigate("reconcile_browse") },   // ← НОВОЕ
                onViewPassed = { nav.navigate("reconcile_done?tab=passed") },
                onReload = { rvm.load() }
            )
        }

        composable("reconcile_browse") {
            val rvm: ReconcileViewModel = viewModel(activity)
            val items by rvm.expected.collectAsState()
            ReconcileBrowseScreen(
                items = items,
                onBack = { nav.popBackStack() }
            )
        }

        composable("reconcile_scan") {
            val rvm: ReconcileViewModel = viewModel(activity)
            ReconcileScanScreen(
                vm = rvm,
                torchOn = torchOn,
                onNextItem = { /* остаться тут */ },
                onFinish = { nav.navigate("reconcile_done") }
            )
        }

        composable("id_print") {
            val barcode by session.barcode.collectAsState()

            IdentifyPrintScreen(
                barcode = barcode,
                onSkip = { nav.navigate("photo") },
                onPrinted = { nav.navigate("photo") },
                onBack = { nav.popBackStack() }
            )
        }

        composable(
            route = "reconcile_done?tab={tab}",
            arguments = listOf(navArgument("tab"){ defaultValue = "" })
        ) { backStackEntry ->
            val rvm: ReconcileViewModel = viewModel(activity)

            val passed = remember(rvm.scanned.collectAsState().value, rvm.expected.collectAsState().value) {
                rvm.passedScanned()
            }
            val notPassed = remember(rvm.scanned.collectAsState().value, rvm.expected.collectAsState().value) {
                rvm.notPassedScanned()
            }

            ReconcileDoneScreen(
                passed = passed,
                notPassed = notPassed,
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
