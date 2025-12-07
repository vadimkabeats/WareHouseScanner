package com.example.warehousescanner.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.warehousescanner.data.ReturnLookupItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ReturnViewModel : ViewModel() {
    private val _dispatchNumber = MutableStateFlow("")
    val dispatchNumber: StateFlow<String> = _dispatchNumber
    private val _printBarcode = MutableStateFlow("")
    val printBarcode: StateFlow<String> = _printBarcode
    private val _returnReason = MutableStateFlow("")
    val returnReason: StateFlow<String> = _returnReason
    private val _productUrl = MutableStateFlow("")
    val productUrl: StateFlow<String> = _productUrl
    private val _hasDefect = MutableStateFlow(false)
    val hasDefect: StateFlow<Boolean> = _hasDefect
    private val _defectDesc = MutableStateFlow("")
    val defectDesc: StateFlow<String> = _defectDesc
    private val _photos = MutableStateFlow<List<Uri>>(emptyList())
    val photos: StateFlow<List<Uri>> = _photos
    private val _decision = MutableStateFlow("")
    val decision: StateFlow<String> = _decision
    private val _items = MutableStateFlow<List<ReturnLookupItem>>(emptyList())
    val items: StateFlow<List<ReturnLookupItem>> = _items
    fun reset() {
        _dispatchNumber.value = ""
        _printBarcode.value = ""
        _returnReason.value = ""
        _productUrl.value = ""
        _hasDefect.value = false
        _defectDesc.value = ""
        _photos.value = emptyList()
        _decision.value = ""
        _items.value = emptyList()
    }
    fun setDispatchNumber(v: String) { _dispatchNumber.value = v }
    fun setPrintBarcode(v: String)   { _printBarcode.value = v }
    fun setReturnReason(v: String)   { _returnReason.value = v }
    fun setReturnUrl(v: String)      { _productUrl.value = v }

    fun setDefect(has: Boolean, desc: String) {
        _hasDefect.value = has
        _defectDesc.value = desc
    }

    fun setPhotos(list: List<Uri>)   { _photos.value = list }
    fun setDecision(v: String)       { _decision.value = v }
    fun setItems(list: List<ReturnLookupItem>) {
        _items.value = list
    }
    fun selectItem(index: Int) {
        val item = _items.value.getOrNull(index) ?: return
        _printBarcode.value = item.barcode.orEmpty()
        _returnReason.value = item.reason.orEmpty()
        _productUrl.value = item.url.orEmpty()
    }
}
