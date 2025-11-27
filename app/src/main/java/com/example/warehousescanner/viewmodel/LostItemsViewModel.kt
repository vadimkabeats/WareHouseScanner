package com.example.warehousescanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehousescanner.data.GoogleSheetClient
import com.example.warehousescanner.data.LostItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LostItemsViewModel : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _items = MutableStateFlow<List<LostItem>>(emptyList())
    val items: StateFlow<List<LostItem>> = _items

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun load(userFio: String?) {
        viewModelScope.launch {
            if (userFio.isNullOrBlank()) {
                _error.value = "Пользователь не определён"
                _items.value = emptyList()
                _loading.value = false
                return@launch
            }

            _loading.value = true
            _error.value = null
            try {
                val resp = GoogleSheetClient.lostItems(userFio)
                if (!resp.ok) {
                    throw IllegalStateException(resp.error ?: "lostItems failed")
                }
                _items.value = resp.items
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Ошибка загрузки"
                _items.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }
}
