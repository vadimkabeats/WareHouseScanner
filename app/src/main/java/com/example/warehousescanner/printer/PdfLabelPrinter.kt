package com.example.warehousescanner.printer

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

object PdfLabelPrinter {
    private val SPP_UUID: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    suspend fun printPdfLabelFromUrl(
        context: Context,
        device: BluetoothDevice,
        pdfUrl: String,
        labelWidthMm: Float = 100f,
        labelHeightMm: Float = 150f,
        dpi: Int = 203
    ) = withContext(Dispatchers.IO) {
        val pdfFile = downloadPdfToCache(context, pdfUrl)

        val rawBitmap = renderFirstPageToBitmap(pdfFile)

        val targetWidthDots = (labelWidthMm / 25.4f * dpi).toInt().coerceAtLeast(100)
        val scaledHeightDots =
            (rawBitmap.height * targetWidthDots / rawBitmap.width.toFloat()).toInt()
                .coerceAtLeast(50)

        val scaled = Bitmap.createScaledBitmap(
            rawBitmap,
            targetWidthDots,
            scaledHeightDots,
            true
        )
        rawBitmap.recycle()

        val (bytesPerRow, bitmapBytes) = bitmapToMonoBytes(scaled)
        val heightDots = scaled.height
        scaled.recycle()

        val tsplBytes = buildTsplBitmapCommand(
            widthBytes = bytesPerRow,
            heightDots = heightDots,
            bitmapBytes = bitmapBytes,
            labelWidthMm = labelWidthMm,
            labelHeightMm = labelHeightMm
        )

        sendToPrinter(device, tsplBytes)
    }

    private fun downloadPdfToCache(context: Context, urlStr: String): File {
        val finalUrl = resolveDirectPdfUrl(urlStr)
        val url = URL(finalUrl)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 20000
            instanceFollowRedirects = true
        }
        return try {
            conn.inputStream.use { input ->
                val file = File.createTempFile("label_", ".pdf", context.cacheDir)
                file.outputStream().use { out ->
                    input.copyTo(out)
                }
                file
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun resolveDirectPdfUrl(original: String): String {
        return try {
            val u = URL(original)
            val host = u.host.lowercase()

            if (!host.contains("yadi.sk") && !host.contains("disk.yandex")) {
                return original
            }
            val apiUrl =
                "https://cloud-api.yandex.net/v1/disk/public/resources/download?public_key=" +
                        URLEncoder.encode(original, "UTF-8")

            val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 20000
                instanceFollowRedirects = true
            }

            val json = try {
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }

            val regex = """"href"\s*:\s*"([^"]+)"""".toRegex()
            val match = regex.find(json)
            val href = match?.groupValues?.get(1)

            if (!href.isNullOrBlank()) href else original
        } catch (_: Exception) {
            original
        }
    }

    private fun renderFirstPageToBitmap(pdfFile: File): Bitmap {
        val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        if (renderer.pageCount == 0) {
            renderer.close()
            pfd.close()
            throw IllegalStateException("PDF не содержит страниц")
        }

        val page = renderer.openPage(0)

        val scale = 3f
        val bmpWidth = (page.width * scale).toInt()
        val bmpHeight = (page.height * scale).toInt()

        val raw = Bitmap.createBitmap(
            bmpWidth,
            bmpHeight,
            Bitmap.Config.ARGB_8888
        )
        page.render(raw, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
        page.close()
        renderer.close()
        pfd.close()
        return if (raw.width > raw.height) {
            val matrix = Matrix().apply { postRotate(90f) }
            val rotated = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
            raw.recycle()
            rotated
        } else {
            raw
        }
    }

    private fun bitmapToMonoBytes(bmp: Bitmap): Pair<Int, ByteArray> {
        val w = bmp.width
        val h = bmp.height
        val bytesPerRow = (w + 7) / 8
        val out = ByteArray(bytesPerRow * h)
        var index = 0
        val threshold = 160

        for (y in 0 until h) {
            var bitPos = 7
            var current = 0
            for (x in 0 until w) {
                val color = bmp.getPixel(x, y)
                val r = Color.red(color)
                val g = Color.green(color)
                val b = Color.blue(color)
                var lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                val norm = lum / 255.0
                lum = (Math.pow(norm, 1.3) * 255.0).toInt().coerceIn(0, 255)
                val bit = if (lum < threshold) 0 else 1

                current = current or (bit shl bitPos)

                if (bitPos == 0) {
                    out[index++] = current.toByte()
                    current = 0
                    bitPos = 7
                } else {
                    bitPos--
                }
            }
            if (bitPos != 7) {
                out[index++] = current.toByte()
            }
        }

        return bytesPerRow to out
    }

    private fun buildTsplBitmapCommand(
        widthBytes: Int,
        heightDots: Int,
        bitmapBytes: ByteArray,
        labelWidthMm: Float,
        labelHeightMm: Float
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        fun writeln(cmd: String) {
            baos.write(cmd.toByteArray(Charsets.US_ASCII))
            baos.write("\r\n".toByteArray(Charsets.US_ASCII))
        }

        writeln("SIZE ${labelWidthMm} mm,${labelHeightMm} mm")
        writeln("GAP 2 mm,0 mm")
        writeln("DENSITY 6")
        writeln("DIRECTION 1")
        writeln("CLS")
        baos.write("BITMAP 0,0,$widthBytes,$heightDots,1,".toByteArray(Charsets.US_ASCII))
        baos.write(bitmapBytes)
        baos.write("\r\n".toByteArray(Charsets.US_ASCII))
        writeln("PRINT 1,1")
        return baos.toByteArray()
    }

    private fun sendToPrinter(device: BluetoothDevice, data: ByteArray) {
        var socket: BluetoothSocket? = null
        try {
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            socket.outputStream.use { out ->
                out.write(data)
                out.flush()
            }
        } catch (e: IOException) {
            throw IOException("Ошибка отправки в принтер: ${e.localizedMessage}", e)
        } finally {
            try {
                socket?.close()
            } catch (_: IOException) {
            }
        }
    }
}
