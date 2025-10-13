package com.example.warehousescanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehousescanner.data.GoogleSheetClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class StatsViewModel : ViewModel() {
    // Дата в МСК (по умолчанию – сегодня МСК)
    private val msk = ZoneId.of("Europe/Moscow")
    private val isoFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val _date = MutableStateFlow(LocalDate.now(msk))
    val date: StateFlow<LocalDate> = _date

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _nlo = MutableStateFlow<Int?>(null)
    val nlo: StateFlow<Int?> = _nlo

    private val _nonNlo = MutableStateFlow<Int?>(null)
    val nonNlo: StateFlow<Int?> = _nonNlo

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun setDate(newDate: LocalDate) {
        _date.value = newDate
    }

    fun loadFor(userFio: String?) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val d = _date.value.format(isoFmt)   // yyyy-MM-dd
                val resp = GoogleSheetClient.dailyStats(d, userFio?.takeIf { it.isNotBlank() })
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
