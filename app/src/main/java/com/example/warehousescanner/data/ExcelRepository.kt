package com.example.warehousescanner.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.apache.poi.ss.usermodel.WorkbookFactory

class ExcelRepository(private val context: Context) {
    private var fileUri: Uri? = null

    fun setFile(uri: Uri) {
        fileUri = uri

        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }

    fun findLink(barcode: String): String? {
        val uri = fileUri ?: return null
        context.contentResolver.openInputStream(uri).use { input ->
            val wb = WorkbookFactory.create(input)
            val sheet = wb.getSheetAt(0)
            for (row in sheet) {
                val code = row.getCell(0)?.stringCellValue
                val link = row.getCell(1)?.stringCellValue
                if (code == barcode) {
                    wb.close()
                    return link
                }
            }
            wb.close()
            return null
        }
    }

    fun appendMapping(barcode: String, link: String) {
        val uri = fileUri ?: return
        context.contentResolver.openInputStream(uri).use { input ->
            val wb = WorkbookFactory.create(input)
            val sheet = wb.getSheetAt(0)
            val newRow = sheet.createRow(sheet.lastRowNum + 1)
            newRow.createCell(0).setCellValue(barcode)
            newRow.createCell(1).setCellValue(link)

            context.contentResolver.openOutputStream(uri).use { output ->
                wb.write(output)
                wb.close()
            }
        }
    }
}
