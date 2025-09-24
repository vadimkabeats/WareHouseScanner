package com.example.warehousescanner.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Хранит состояние последней напечатанной коробки (full-трек),
 * чтобы не печатать этикетку повторно для товаров с тем же full,
 * когда в таблице стоит признак "Несколько товаров".
 */
class PrintSessionViewModel : ViewModel() {
    private val _lastPrintedTrackFull = MutableStateFlow<String?>(null)
    val lastPrintedTrackFull: StateFlow<String?> = _lastPrintedTrackFull

    fun markPrinted(trackFull: String) {
        _lastPrintedTrackFull.value = trackFull
    }

    fun reset() {
        _lastPrintedTrackFull.value = null
    }
}
