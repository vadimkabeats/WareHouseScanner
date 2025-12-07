package com.example.warehousescanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehousescanner.data.GoogleSheetClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StatsViewModel : ViewModel() {
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    private val _nlo = MutableStateFlow<Int?>(null)
    val nlo: StateFlow<Int?> = _nlo
    private val _nonNlo = MutableStateFlow<Int?>(null)
    val nonNlo: StateFlow<Int?> = _nonNlo
    private val _identified = MutableStateFlow<Int?>(null)
    val identified: StateFlow<Int?> = _identified
    private val _putAway = MutableStateFlow<Int?>(null)
    val putAway: StateFlow<Int?> = _putAway
    private val _lost = MutableStateFlow<Int?>(null)
    val lost: StateFlow<Int?> = _lost
    private val _totalIdentified = MutableStateFlow<Int?>(null)
    val totalIdentified: StateFlow<Int?> = _totalIdentified
    private val _totalPutAway = MutableStateFlow<Int?>(null)
    val totalPutAway: StateFlow<Int?> = _totalPutAway
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private fun resetAll() {
        _nlo.value = null
        _nonNlo.value = null
        _identified.value = null
        _putAway.value = null
        _lost.value = null
        _totalIdentified.value = null
        _totalPutAway.value = null
    }
    fun loadFor(userFio: String?) {
        viewModelScope.launch {
            if (userFio.isNullOrBlank()) {
                _error.value = null
                resetAll()
                _loading.value = false
                return@launch
            }
            _loading.value = true
            _error.value = null
            try {
                val resp = GoogleSheetClient.dailyStats(
                    dateIso = null,
                    user = userFio
                )
                if (!resp.ok) {
                    throw IllegalStateException(resp.error ?: "dailyStats failed")
                }
                _nlo.value = resp.nlo
                _nonNlo.value = resp.nonNlo
                _identified.value = resp.identified
                _putAway.value = resp.identifiedPut
                _lost.value = resp.identifiedNotPut
                _totalIdentified.value = resp.totalIdentified
                _totalPutAway.value = resp.totalPutAway
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Ошибка загрузки"
                resetAll()
            } finally {
                _loading.value = false
            }
        }
    }
}
