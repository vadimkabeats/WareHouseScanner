package com.example.warehousescanner.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehousescanner.data.ExcelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ExcelViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = ExcelRepository(application)

    // URI файла базы, либо null, если ещё не выбран
    private val _fileUriState = MutableStateFlow<Uri?>(null)
    val fileUriState: StateFlow<Uri?> = _fileUriState

    // Состояние найденной ссылки: null=ещё не искали, ""=не найдено, не-пустая=найдено
    private val _linkState = MutableStateFlow<String?>(null)
    val linkState: StateFlow<String?> = _linkState

    // Вызываем сразу после выбора пользователем файла .xlsx
    fun setFile(uri: Uri) {
        // даём постоянные права
        getApplication<Application>().contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        repo.setFile(uri)
        _fileUriState.value = uri
    }

    fun lookup(barcode: String) {
        viewModelScope.launch {
            _linkState.value = repo.findLink(barcode) ?: ""
        }
    }

    fun save(barcode: String, link: String) {
        viewModelScope.launch {
            repo.appendMapping(barcode, link)
            _linkState.value = link
        }
    }
}
