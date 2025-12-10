package com.example.warehousescanner.ui.returns

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehousescanner.data.GoogleSheetClient
import kotlinx.coroutines.launch

data class ReturnProcessUiItem(
    val barcode: String,
    val title: String,
    val action: String,
    val comment: String
)

data class ReturnProcessUiState(
    val track: String = "",
    val items: List<ReturnProcessUiItem> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val processed: Boolean = false
)

class ReturnProcessViewModel : ViewModel() {
    var state by mutableStateOf(ReturnProcessUiState())
        private set
    fun onTrackChange(new: String) {
        state = state.copy(track = new, processed = false, error = null)
    }
    fun search() {
        val track = state.track.trim()
        if (track.isEmpty()) {
            state = state.copy(error = "Введите трек-номер")
            return
        }
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null, items = emptyList(), processed = false)
            try {
                val resp = GoogleSheetClient.returnProcessLookup(track)
                if (!resp.ok) {
                    state = state.copy(
                        isLoading = false,
                        error = resp.error ?: "Ошибка сервера"
                    )
                    return@launch
                }

                val items = resp.items.orEmpty().map {
                    ReturnProcessUiItem(
                        barcode = it.barcode,
                        title = it.title.orEmpty(),
                        action = it.action.orEmpty(),
                        comment = it.comment.orEmpty()
                    )
                }

                state = state.copy(
                    isLoading = false,
                    items = items,
                    error = if (items.isEmpty()) "По этому треку ничего не найдено" else null
                )
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    error = e.message ?: "Ошибка сети"
                )
            }
        }
    }

    fun markProcessed() {
        val track = state.track.trim()
        if (track.isEmpty()) return
        viewModelScope.launch {
            state = state.copy(isSaving = true, error = null)
            try {
                val resp = GoogleSheetClient.returnProcessDone(track)
                if (!resp.ok) {
                    state = state.copy(
                        isSaving = false,
                        error = resp.error ?: "Не удалось сохранить"
                    )
                    return@launch
                }
                state = state.copy(
                    isSaving = false,
                    processed = true
                )
            } catch (e: Exception) {
                state = state.copy(
                    isSaving = false,
                    error = e.message ?: "Ошибка сети"
                )
            }
        }
    }
}
