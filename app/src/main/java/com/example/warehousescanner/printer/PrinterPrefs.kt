package com.example.warehousescanner.printer

import android.content.Context
import android.content.Context.MODE_PRIVATE

object PrinterPrefs {
    private const val PREFS = "printer_prefs"
    private const val KEY_MAC = "saved_mac"

    fun getSavedMac(ctx: Context): String? =
        ctx.getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_MAC, null)

    fun setSavedMac(ctx: Context, mac: String) {
        ctx.getSharedPreferences(PREFS, MODE_PRIVATE)
            .edit().putString(KEY_MAC, mac).apply()
    }

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREFS, MODE_PRIVATE)
            .edit().remove(KEY_MAC).apply()
    }
}
