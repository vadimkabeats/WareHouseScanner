package com.example.warehousescanner.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@Composable
fun CheckScreen(
    url: String,
    allowMatch: Boolean = true,
    onAction: (status: String, comment: String, newLink: String) -> Unit
) {
    val uriHandler = LocalUriHandler.current

    var status by remember { mutableStateOf<String?>(null) }
    var mismatchType by remember { mutableStateOf<String?>(null) }
    var comment by remember { mutableStateOf("") }
    var newLink by remember { mutableStateOf("") }
    var linkError by remember { mutableStateOf<String?>(null) }

    fun extractAndNormalizeUrl(text: String): String? {
        val t = text.trim()
        Regex("""(?i)\b(https?://[^\s]+|www\.[^\s]+)\b""")
            .find(t)
            ?.value
            ?.let { return if (it.startsWith("http", true)) it else "https://$it" }

        val bareUrl = Regex("""^(?i)([A-Za-z0-9-]+\.)+[A-Za-z]{2,}(/[^\s]*)?$""")
        return if (bareUrl.matches(t)) "https://$t" else null
    }

    fun requireLinkIfNeeded(): Boolean {
        linkError = null

        val needLink = when {
            status == "mismatch" && (mismatchType == "other" || mismatchType == "no_barcode") -> true
            else -> false
        }
        if (needLink) {
            val normalized = extractAndNormalizeUrl(newLink)
            if (normalized == null) {
                linkError = "Нет ссылки на товар. Добавьте её"
                return false
            }
            newLink = normalized
        }
        return true
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Проверьте товар", style = MaterialTheme.typography.h6)

        if (url.isNotBlank()) {
            Text(
                text = url,
                color = MaterialTheme.colors.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { runCatching { uriHandler.openUri(url) } }
            )
        } else {
            Text("Ссылка отсутствует")
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

            if (allowMatch) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = status == "match",
                        onClick = {
                            status = "match"
                            mismatchType = null
                            newLink = ""
                            comment = ""
                            linkError = null
                        }
                    )
                    Text("Совпадает", modifier = Modifier.padding(end = 24.dp))

                    RadioButton(
                        selected = status == "mismatch",
                        onClick = {
                            status = "mismatch"
                            if (mismatchType == null) mismatchType = "other"
                            linkError = null
                        }
                    )
                    Text("Не совпадает")
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = status == "mismatch" && mismatchType == "no_barcode",
                        onClick = {
                            status = "mismatch"
                            mismatchType = "no_barcode"
                            linkError = null
                        }
                    )
                    Text("Товар без ШК", modifier = Modifier.padding(end = 24.dp))

                    RadioButton(
                        selected = status == "nlo",
                        onClick = {
                            status = "nlo"
                            mismatchType = null
                            newLink = ""
                            comment = ""
                            linkError = null
                        }
                    )
                    Text("НЛО")
                }
            }

            if (allowMatch) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = status == "nlo",
                        onClick = {
                            status = "nlo"
                            mismatchType = null
                            newLink = ""
                            comment = ""
                            linkError = null
                        }
                    )
                    Text("НЛО")
                }
            }
        }

        if (status == "mismatch") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (allowMatch) {
                    Text("Причина несоответствия", style = MaterialTheme.typography.subtitle1)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = mismatchType == "other",
                            onClick = { mismatchType = "other"; linkError = null }
                        )
                        Text("Другой товар", modifier = Modifier.padding(end = 24.dp))

                        RadioButton(
                            selected = mismatchType == "no_barcode",
                            onClick = { mismatchType = "no_barcode"; linkError = null }
                        )
                        Text("Товар без ШК")
                    }
                } else {
                    Text("Причина: товар без ШК", style = MaterialTheme.typography.subtitle1)
                }

                OutlinedTextField(
                    value = newLink,
                    onValueChange = { txt ->
                        val extracted = extractAndNormalizeUrl(txt)
                        val cleaned = when {
                            extracted != null &&
                                    (txt.contains(' ') || txt.contains('\n') || txt.contains('\r') || txt != extracted) -> extracted
                            else -> txt
                        }
                        newLink = cleaned
                        linkError = if (cleaned.isNotBlank() && extractAndNormalizeUrl(cleaned) == null) {
                            "Нет ссылки на товар. Добавьте её"
                        } else null
                    },
                    label = { Text("Ссылка на товар") },
                    isError = linkError != null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (linkError != null) {
                    Text(
                        linkError!!,
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption
                    )
                }

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Комментарий (необязательно)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.weight(1f))

        val effectiveStatus: String? = when (status) {
            "match" -> if (allowMatch) "match" else null
            "nlo" -> "nlo"
            "mismatch" -> when {
                !allowMatch -> "mismatch_nobarcode"
                mismatchType == "other" -> "mismatch_other"
                mismatchType == "no_barcode" -> "mismatch_nobarcode"
                else -> null
            }
            else -> null
        }

        val canContinue: Boolean =
            when (effectiveStatus) {
                "match", "nlo" -> true
                "mismatch_other", "mismatch_nobarcode" -> extractAndNormalizeUrl(newLink) != null
                else -> false
            }

        Button(
            onClick = {
                if (!requireLinkIfNeeded()) return@Button
                val st = effectiveStatus!!
                val c = when (st) {
                    "mismatch_other", "mismatch_nobarcode" -> comment
                    else -> ""
                }
                val l = when (st) {
                    "mismatch_other", "mismatch_nobarcode" -> extractAndNormalizeUrl(newLink)!!.trim()
                    else -> ""
                }
                onAction(st, c, l)
            },
            enabled = canContinue,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Далее") }
    }
}
