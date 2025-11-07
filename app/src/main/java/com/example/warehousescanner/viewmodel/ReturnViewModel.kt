package com.example.warehousescanner.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ReturnViewModel : ViewModel() {
    private val _dispatchNumber = MutableStateFlow("")
    val dispatchNumber: StateFlow<String> = _dispatchNumber

    private val _printBarcode = MutableStateFlow("")
    val printBarcode: StateFlow<String> = _printBarcode

    private val _returnReason = MutableStateFlow("")
    val returnReason: StateFlow<String> = _returnReason

    private val _hasDefect = MutableStateFlow(false)
    val hasDefect: StateFlow<Boolean> = _hasDefect

    private val _defectDesc = MutableStateFlow("")
    val defectDesc: StateFlow<String> = _defectDesc

    private val _photos = MutableStateFlow<List<Uri>>(emptyList())
    val photos: StateFlow<List<Uri>> = _photos

    private val _productUrl = MutableStateFlow("")
    val productUrl: StateFlow<String> = _productUrl

    /** НОВОЕ: выбранное действие с возвратом */
    private val _decision = MutableStateFlow("")
    val decision: StateFlow<String> = _decision

    fun reset() {
        _dispatchNumber.value = ""
        _printBarcode.value = ""
        _returnReason.value = ""
        _productUrl.value = ""
        _hasDefect.value = false
        _defectDesc.value = ""
        _photos.value = emptyList()
        _decision.value = ""
    }

    fun setDispatchNumber(value: String) { _dispatchNumber.value = value }
    fun setPrintBarcode(value: String) { _printBarcode.value = value }
    fun setReturnReason(value: String) { _returnReason.value = value }
    fun setReturnUrl(value: String)    { _productUrl.value = value }
    fun setDefect(has: Boolean, desc: String) { _hasDefect.value = has; _defectDesc.value = desc }
    fun setPhotos(list: List<Uri>) { _photos.value = list }
    fun setDecision(value: String) { _decision.value = value }
}
