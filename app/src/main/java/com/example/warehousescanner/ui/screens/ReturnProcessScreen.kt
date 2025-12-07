package com.example.warehousescanner.ui.returns

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.warehousescanner.printer.LabelPrinter
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions

@Composable
fun ReturnProcessScreen(
    navController: NavHostController,
    viewModel: ReturnProcessViewModel = viewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var printError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Обработка возвратов") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = state.track,
                onValueChange = {
                    printError = null
                    viewModel.onTrackChange(it)
                },
                label = { Text("Трек-номер") },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { viewModel.search() }) {
                        Icon(Icons.Default.Search, contentDescription = "Найти")
                    }
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { viewModel.search() }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (state.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                )
            }

            val errorToShow = state.error ?: printError
            if (errorToShow != null) {
                Text(
                    text = errorToShow,
                    color = MaterialTheme.colors.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            if (state.items.isNotEmpty()) {
                Text(
                    text = "Товары по этому треку:",
                    style = MaterialTheme.typography.subtitle1
                )
                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(state.items) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("ШК: ${item.barcode}", style = MaterialTheme.typography.body1)
                                if (item.title.isNotBlank()) {
                                    Text(item.title, style = MaterialTheme.typography.body2)
                                }
                                if (item.action.isNotBlank()) {
                                    Text(
                                        "Что делать: ${item.action}",
                                        style = MaterialTheme.typography.body2
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        scope.launch {
                                            printError = null
                                            val device = LabelPrinter.restoreLastPrinter(context)
                                            if (device == null) {
                                                printError =
                                                    "Сначала выберите принтер в разделе «Печать ШК»"
                                                return@launch
                                            }
                                            try {
                                                LabelPrinter.printTsplFixedSmallCompact(
                                                    context = context,
                                                    device = device,
                                                    barcodeText = item.barcode,
                                                    captionText = item.title.ifBlank { null }
                                                )
                                            } catch (e: Exception) {
                                                printError =
                                                    e.localizedMessage ?: "Ошибка печати"
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Печать ШК")
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.markProcessed() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Обработано")
                }
                if (state.processed) {
                    Text(
                        text = "Отмечено как обработано",
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                if (!state.isLoading && state.track.isNotBlank() && state.error == null) {
                    Text(
                        text = "По этому треку пока нет строк в таблице.",
                        style = MaterialTheme.typography.body2
                    )
                }
            }
        }
    }
}
