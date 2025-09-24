package com.example.warehousescanner.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ReturnViewModel : ViewModel() {
    private val _barcode = MutableStateFlow("")
    val barcode: StateFlow<String> = _barcode

    private val _hasDefect = MutableStateFlow(false)
    val hasDefect: StateFlow<Boolean> = _hasDefect

    private val _defectDesc = MutableStateFlow("")
    val defectDesc: StateFlow<String> = _defectDesc

    private val _photos = MutableStateFlow<List<Uri>>(emptyList())
    val photos: StateFlow<List<Uri>> = _photos

    fun reset() {
        _barcode.value = ""
        _hasDefect.value = false
        _defectDesc.value = ""
        _photos.value = emptyList()
    }

    fun setBarcode(value: String) { _barcode.value = value.trim() }
    fun setDefect(has: Boolean, desc: String) { _hasDefect.value = has; _defectDesc.value = desc }
    fun setPhotos(list: List<Uri>) { _photos.value = list }
}
