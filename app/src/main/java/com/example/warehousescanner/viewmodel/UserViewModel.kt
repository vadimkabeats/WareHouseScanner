package com.example.warehousescanner.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UserViewModel(app: Application) : AndroidViewModel(app) {
    private val sp = app.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    private val _fullName = MutableStateFlow(sp.getString("full_name", "") ?: "")
    val fullName: StateFlow<String> = _fullName

    fun setFullName(value: String) {
        sp.edit().putString("full_name", value).apply()
        _fullName.value = value
    }
}
