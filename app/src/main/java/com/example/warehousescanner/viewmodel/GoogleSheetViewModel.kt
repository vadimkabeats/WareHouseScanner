// viewmodel/GoogleSheetViewModel.kt
package com.example.warehousescanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehousescanner.data.GoogleSheetClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GoogleSheetViewModel(application: Application) : AndroidViewModel(application) {

    private val _linkState = MutableStateFlow<String?>(null)
    val linkState: StateFlow<String?> = _linkState


    private val _trackState = MutableStateFlow<String?>(null)
    val trackState: StateFlow<String?> = _trackState

    fun lookup(barcode: String) {
        viewModelScope.launch {
            _linkState.value = null
            try {
                val r = GoogleSheetClient.lookup(barcode)
                _linkState.value = if (r.found) r.link.orEmpty() else ""
            } catch (_: Exception) {
                _linkState.value = ""
            }
        }
    }

    fun lookupTrack(barcode: String) {
        viewModelScope.launch {
            _trackState.value = null
            try {
                val r = GoogleSheetClient.lookupTrack(barcode)
                _trackState.value = if (r.ok && r.found && !r.track.isNullOrBlank()) r.track else ""
            } catch (_: Exception) {
                _trackState.value = ""
            }
        }
    }

    fun reset() { _linkState.value = null }
    fun resetTrack() { _trackState.value = null }
}
