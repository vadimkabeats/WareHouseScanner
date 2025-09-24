package com.example.warehousescanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun ReturnConditionScreen(
    barcode: String,
    hasDefectInit: Boolean,
    defectDescInit: String,
    photosCount: Int,
    onChangeState: (hasDefect: Boolean, desc: String) -> Unit,
    onOpenPhotos: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var hasDefect by remember { mutableStateOf(hasDefectInit) }
    var defectDesc by remember { mutableStateOf(defectDescInit) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Принять возврат — состояние товара", style = MaterialTheme.typography.h6)
        Text("Штрих-код: $barcode")


        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    hasDefect = false
                    defectDesc = ""
                    onChangeState(false, "")
                },
                enabled = hasDefect
            ) { Text("Нет дефектов") }

            OutlinedButton(
                onClick = {
                    hasDefect = true
                    onChangeState(true, defectDesc)
                },
                enabled = !hasDefect
            ) { Text("Есть дефекты") }
        }

        if (hasDefect) {
            OutlinedTextField(
                value = defectDesc,
                onValueChange = {
                    defectDesc = it
                    onChangeState(true, defectDesc)
                },
                label = { Text("Описание дефекта") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done)
            )


            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Фотографии: $photosCount/6")
                OutlinedButton(onClick = onOpenPhotos) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Добавить/изменить")
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) { Text("Назад") }

            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f)
            ) { Text("Далее") }
        }
    }
}
