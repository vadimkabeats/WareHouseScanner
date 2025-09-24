package com.example.warehousescanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    onLogin: (firstName: String, lastName: String, password: String) -> Unit,
    errorText: String?,
    isLoading: Boolean
) {
    var first by remember { mutableStateOf("") }
    var last  by remember { mutableStateOf("") }
    var pass  by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Вход в аккаунт", style = MaterialTheme.typography.h6)

        OutlinedTextField(
            value = last,
            onValueChange = { last = it },
            label = { Text("Фамилия") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = first,
            onValueChange = { first = it },
            label = { Text("Имя") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (!errorText.isNullOrBlank()) {
            Text(errorText, color = MaterialTheme.colors.error)
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { onLogin(first.trim(), last.trim(), pass) },
            enabled = !isLoading && first.isNotBlank() && last.isNotBlank() && pass.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoading) "Проверяем..." else "Войти")
        }
    }
}
