package com.example.warehousescanner.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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
