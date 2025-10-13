package com.example.warehousescanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehousescanner.data.GoogleSheetClient
import com.example.warehousescanner.data.ReconcileItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ReconcileViewModel : ViewModel() {

    private val _expected = MutableStateFlow<List<ReconcileItem>>(emptyList())
    val expected: StateFlow<List<ReconcileItem>> = _expected

    private val _itemsByTrack = MutableStateFlow<Map<String, ReconcileItem>>(emptyMap())

    private val _scanned = MutableStateFlow<Set<String>>(emptySet()) // нормализованные треки
    val scanned: StateFlow<Set<String>> = _scanned

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val items = GoogleSheetClient.reconcileInit()
                _expected.value = items
                _itemsByTrack.value = items.associateBy { normalizeTrack(it.track) }
                _scanned.value = emptySet()
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Ошибка загрузки"
            } finally {
                _loading.value = false
            }
        }
    }

    private fun normalizeTrack(s: String) =
        s.replace(Regex("[\\s-]+"), "").uppercase()

    fun addScannedTrack(raw: String) {
        val norm = normalizeTrack(raw)
        if (norm.isNotBlank()) _scanned.value = _scanned.value + norm
    }
    fun removeScannedTrack(raw: String) {
        val norm = normalizeTrack(raw)
        _scanned.value = _scanned.value - norm
    }
    fun reset() {
        _expected.value = emptyList()
        _itemsByTrack.value = emptyMap()
        _scanned.value = emptySet()
        _error.value = null
        _loading.value = false
    }

    /** ОТСКАНИРОВАНО + С ГАЛОЧКОЙ */
    fun passedScanned(): List<ReconcileItem> {
        val map = _itemsByTrack.value
        return _scanned.value.mapNotNull { t -> map[t] }.filter { it.processed }
    }

    /** ОТСКАНИРОВАНО + БЕЗ ГАЛОЧКИ */
    fun notPassedScanned(): List<ReconcileItem> {
        val map = _itemsByTrack.value
        return _scanned.value.mapNotNull { t -> map[t] }.filter { !it.processed }
    }
}
