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

    fun lookup(barcode: String) {
        viewModelScope.launch {
            _linkState.value = null
            runCatching { GoogleSheetClient.lookup(barcode) }
                .onSuccess { r -> _linkState.value = if (r.found) r.link.orEmpty() else "" }
                .onFailure { _linkState.value = "" }
        }
    }

    fun save(barcode: String, link: String, user: String) {
        viewModelScope.launch {
            _linkState.value = null
            runCatching { GoogleSheetClient.save(barcode, link, user) }
                .onSuccess { _linkState.value = link }
                .onFailure { _linkState.value = "" }
        }
    }
}
