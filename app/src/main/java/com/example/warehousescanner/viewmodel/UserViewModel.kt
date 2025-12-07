package com.example.warehousescanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UserViewModel(app: Application): AndroidViewModel(app) {
    private val _fullName = MutableStateFlow("")
    val fullName: StateFlow<String> = _fullName
    fun setFullName(fio: String) { _fullName.value = fio }
    fun clearFullName() { _fullName.value = "" }
}
