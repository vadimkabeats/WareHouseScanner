package com.example.warehousescanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Хранит пользовательские настройки приложения.
 * Сейчас: флаг "Темно" — включать ли фонарик во всех сценариях сканирования.
 */
class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val _torchEnabled = MutableStateFlow(false)
    val torchEnabled: StateFlow<Boolean> = _torchEnabled

    fun setTorchEnabled(enabled: Boolean) { _torchEnabled.value = enabled }
}
