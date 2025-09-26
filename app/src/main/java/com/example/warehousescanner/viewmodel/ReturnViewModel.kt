package com.example.warehousescanner.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ReturnViewModel : ViewModel() {
    private val _dispatchNumber = MutableStateFlow("")
    val dispatchNumber: StateFlow<String> = _dispatchNumber

    private val _printBarcode = MutableStateFlow("") // barcode, который печатаем
    val printBarcode: StateFlow<String> = _printBarcode

    private val _hasDefect = MutableStateFlow(false)
    val hasDefect: StateFlow<Boolean> = _hasDefect

    private val _defectDesc = MutableStateFlow("")
    val defectDesc: StateFlow<String> = _defectDesc

    private val _photos = MutableStateFlow<List<Uri>>(emptyList())
    val photos: StateFlow<List<Uri>> = _photos

    fun reset() {
        _dispatchNumber.value = ""
        _printBarcode.value = ""
        _hasDefect.value = false
        _defectDesc.value = ""
        _photos.value = emptyList()
    }

    fun setDispatchNumber(value: String) { _dispatchNumber.value = value }
    fun setPrintBarcode(value: String) { _printBarcode.value = value }
    fun setDefect(has: Boolean, desc: String) { _hasDefect.value = has; _defectDesc.value = desc }
    fun setPhotos(list: List<Uri>) { _photos.value = list }
}
