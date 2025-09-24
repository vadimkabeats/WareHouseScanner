package com.example.warehousescanner.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File

fun createTempImageUri(context: Context): Uri {
    val base = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        ?: context.filesDir
    val dir = File(base, "CameraTemp").apply { mkdirs() }
    val file = File.createTempFile("photo_", ".jpg", dir)


    val authority = "${context.packageName}.fileprovider"

    return FileProvider.getUriForFile(context, authority, file)
}
