package com.example.warehousescanner.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SessionViewModel : ViewModel() {
    private val _barcode = MutableStateFlow("")
    val barcode: StateFlow<String> = _barcode

    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url

    private val _checkStatus = MutableStateFlow("") // "match" | "mismatch"
    val checkStatus: StateFlow<String> = _checkStatus

    private val _checkComment = MutableStateFlow("")
    val checkComment: StateFlow<String> = _checkComment

    private val _newLink = MutableStateFlow("")
    val newLink: StateFlow<String> = _newLink

    private val _photos = MutableStateFlow<List<Uri>>(emptyList())
    val photos: StateFlow<List<Uri>> = _photos

    private val _hasDefect = MutableStateFlow(false)
    val hasDefect: StateFlow<Boolean> = _hasDefect

    private val _defectDesc = MutableStateFlow("")
    val defectDesc: StateFlow<String> = _defectDesc

    private val _quantity = MutableStateFlow(1)
    val quantity: StateFlow<Int> = _quantity

    fun setBarcode(value: String)               { _barcode.value = value }
    fun setUrl(value: String)                   { _url.value = value }
    fun setCheckResult(status: String, c: String) { _checkStatus.value = status; _checkComment.value = c }
    fun setNewLink(value: String)               { _newLink.value = value }
    fun setPhotos(list: List<Uri>)              { _photos.value = list }
    fun setDefect(has: Boolean, desc: String)   { _hasDefect.value = has; _defectDesc.value = desc }
    fun setQuantity(qty: Int)                   { _quantity.value = qty.coerceAtLeast(1) }
}
