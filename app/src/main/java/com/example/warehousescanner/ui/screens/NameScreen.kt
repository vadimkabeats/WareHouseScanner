package com.example.warehousescanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun NameScreen(onSave: (String) -> Unit) {
    var lastName by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Представьтесь", style = MaterialTheme.typography.h6)
        OutlinedTextField(
            value = lastName, onValueChange = { lastName = it },
            label = { Text("Фамилия") }, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = firstName, onValueChange = { firstName = it },
            label = { Text("Имя") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                val full = (lastName.trim() + " " + firstName.trim()).trim()
                if (full.isNotEmpty()) onSave(full)
            },
            enabled = lastName.isNotBlank() && firstName.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Сохранить и продолжить") }
    }
}
