package com.example.warehousescanner

/**
 * Singleton для хранения OAuth-токена в памяти.
 * Можно заменить на DataStore/SharedPreferences для долговременного хранения.
 */
object YandexAuth {
    var token: String? = null
}
