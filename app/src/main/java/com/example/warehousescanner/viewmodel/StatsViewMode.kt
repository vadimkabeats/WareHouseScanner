package com.example.warehousescanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehousescanner.data.GoogleSheetClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Показываем только статистику за сегодня (МСК).
 * Дата не выбирается, DatePicker убран.
 */
class StatsViewModel : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _nlo = MutableStateFlow<Int?>(null)
    val nlo: StateFlow<Int?> = _nlo

    private val _nonNlo = MutableStateFlow<Int?>(null)
    val nonNlo: StateFlow<Int?> = _nonNlo

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /**
     * Всегда грузим статистику «за сегодня» по Москве.
     * В Apps Script при пустой дате используется Europe/Moscow.
     */
    fun loadFor(userFio: String?) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                // dateIso = null  => «сегодня» (МСК) на стороне backend
                val resp = GoogleSheetClient.dailyStats(dateIso = null, user = userFio?.takeIf { it.isNotBlank() })
                if (!resp.ok) throw IllegalStateException(resp.error ?: "dailyStats failed")
                _nlo.value = resp.nlo
                _nonNlo.value = resp.nonNlo
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Ошибка загрузки"
                _nlo.value = null
                _nonNlo.value = null
            } finally {
                _loading.value = false
            }
        }
    }
}
