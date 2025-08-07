package com.example.warehousescanner.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream
import java.io.OutputStream

class ExcelRepository(private val context: Context) {
    private var fileUri: Uri? = null

    /** Должно вызываться сразу после выбора файла через SAF */
    fun setFile(uri: Uri) {
        fileUri = uri
        // Захватываем постоянные права на чтение/запись
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }

    /** Ищет ссылку по штрихкоду или возвращает null */
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

    /**
     * Дописывает новую строку (barcode, link) и сохраняет книгу обратно на тот же URI.
     */
    fun appendMapping(barcode: String, link: String) {
        val uri = fileUri ?: return
        // Открываем входной поток и сразу создаём Workbook
        context.contentResolver.openInputStream(uri).use { input ->
            val wb = WorkbookFactory.create(input)
            val sheet = wb.getSheetAt(0)
            val newRow = sheet.createRow(sheet.lastRowNum + 1)
            newRow.createCell(0).setCellValue(barcode)
            newRow.createCell(1).setCellValue(link)

            // Сохраняем изменения обратно
            context.contentResolver.openOutputStream(uri).use { output ->
                wb.write(output)
                wb.close()
            }
        }
    }
}
