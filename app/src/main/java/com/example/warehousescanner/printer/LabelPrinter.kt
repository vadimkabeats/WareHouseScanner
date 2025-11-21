package com.example.warehousescanner.printer

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

object LabelPrinter {
    private const val LABEL_LEFT_MARGIN = 24
    private val SPP_UUID: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private const val PREFS = "label_printer_prefs"
    private const val KEY_ADDR = "last_printer_addr"

    private var socket: BluetoothSocket? = null
    private var connectedAddr: String? = null
    private val printMutex = Mutex()


    private fun hasBtConnect(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 31) {
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else true

    private fun hasBtScan(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 31) {
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else true


    @SuppressLint("MissingPermission")
    fun saveLastPrinter(ctx: Context, device: BluetoothDevice) {
        if (Build.VERSION.SDK_INT >= 31 && !hasBtConnect(ctx)) return
        val addr = try { device.address } catch (_: SecurityException) { null } ?: return
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_ADDR, addr).apply()
    }

    fun restoreLastPrinter(ctx: Context): BluetoothDevice? {
        val addr = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ADDR, null) ?: return null
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return null
        return try { adapter.getRemoteDevice(addr) } catch (_: Exception) { null }
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(ctx: Context): List<BluetoothDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        if (Build.VERSION.SDK_INT >= 31 && !hasBtConnect(ctx)) return emptyList()
        val bonded = try { adapter.bondedDevices } catch (_: SecurityException) { emptySet() }
        return bonded.toList().sortedBy { d ->
            val n = try { d.name } catch (_: SecurityException) { null }
            n ?: try { d.address } catch (_: SecurityException) { "ZZZ" }
        }
    }


    @SuppressLint("MissingPermission")
    private suspend fun ensureConnected(ctx: Context, device: BluetoothDevice): BluetoothSocket =
        withContext(Dispatchers.IO) {
            val s = socket
            if (s != null && s.isConnected && connectedAddr == safeAddress(device)) return@withContext s

            closeSilently()

            val adapter = BluetoothAdapter.getDefaultAdapter()
            try { if (hasBtScan(ctx)) adapter?.cancelDiscovery() } catch (_: Exception) {}

            val newSock = if (Build.VERSION.SDK_INT >= 31 && !hasBtConnect(ctx)) {
                throw SecurityException("Нет разрешения BLUETOOTH_CONNECT")
            } else {
                device.createRfcommSocketToServiceRecord(SPP_UUID)
            }

            try { newSock.connect() } catch (e: Exception) {
                try { newSock.close() } catch (_: Exception) {}
                throw e
            }

            socket = newSock
            connectedAddr = safeAddress(device)
            newSock
        }

    @SuppressLint("MissingPermission")
    private fun safeAddress(d: BluetoothDevice): String? =
        try { d.address } catch (_: SecurityException) { null }

    private fun closeSilently() {
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        connectedAddr = null
    }


    private suspend fun isPrinterReady(sock: BluetoothSocket): Boolean =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val out = sock.outputStream
                val ins = sock.inputStream
                sendRaw(out, "~HS\r\n".toByteArray(Charsets.US_ASCII))
                val resp = readWithTimeout(ins, 600.milliseconds)
                if (resp.isBlank()) true
                else {
                    val low = resp.lowercase()
                    !(low.contains("pause") || low.contains("error")
                            || low.contains("paper") || low.contains("door"))
                }
            } catch (_: Exception) { true }
        }

    private fun readWithTimeout(ins: InputStream, timeoutMs: kotlin.time.Duration): String {
        val end = System.currentTimeMillis() + timeoutMs.inWholeMilliseconds
        val buf = ByteArray(1024)
        val sb = StringBuilder()
        while (System.currentTimeMillis() < end) {
            val avail = try { ins.available() } catch (_: Exception) { 0 }
            if (avail > 0) {
                val n = ins.read(buf, 0, buf.size.coerceAtMost(avail))
                if (n > 0) sb.append(String(buf, 0, n, Charsets.US_ASCII))
            } else {
                try { Thread.sleep(20) } catch (_: InterruptedException) {}
            }
        }
        return sb.toString()
    }

    private fun sendRaw(out: OutputStream, data: ByteArray) {
        out.write(data)
        out.flush()
    }

    suspend fun printTsplFixedSmall(
        context: Context,
        device: BluetoothDevice,
        text: String
    ) = printTsplFixedSmall(context, device, barcodeText = text, captionText = text)

    suspend fun printTsplFixedSmall(
        context: Context,
        device: BluetoothDevice,
        barcodeText: String,
        captionText: String?
    ) = printMutex.withLock {
        withContext(Dispatchers.IO) {
            val sock = ensureConnected(context, device)
            if (!isPrinterReady(sock)) {
                throw IllegalStateException("Принтер не готов (бумага/крышка/пауза)")
            }

            val len = barcodeText.length
            val narrow = when { len <= 18 -> 3; len <= 24 -> 2; else -> 1 }
            val wide   = when { len <= 18 -> 6; len <= 24 -> 5; else -> 3 }
            val baseBcHeight = when { len <= 18 -> 190; len <= 24 -> 180; else -> 165 }

            // Отступ слева — используем общую константу
            val left = LABEL_LEFT_MARGIN
            val top  = 12

            val layout = if (!captionText.isNullOrBlank())
                pickCaptionLayout(captionText!!, baseBcHeight)
            else
                CaptionLayout(font = "3", sx = 1, sy = 1, lineStep = 22, maxPerLine = intArrayOf(), bcHeight = baseBcHeight)

            val lines = if (!captionText.isNullOrBlank())
                wrapCaptionSmart(captionText!!, layout.maxPerLine)
            else emptyList()

            val bcHeight = layout.bcHeight
            val baseTextY = (top + bcHeight + 16).coerceAtMost(300)

            val tspl = buildString {
                append("SIZE 58 mm,40 mm\r\n")
                append("GAP 2 mm,0\r\n")
                append("DENSITY 8\r\n")
                append("SPEED 4\r\n")
                append("DIRECTION 1\r\n")
                append("REFERENCE 0,0\r\n")
                append("CLS\r\n")

                append("""BARCODE $left,$top,"128",$bcHeight,0,0,$narrow,$wide,"$barcodeText"""" + "\r\n")

                if (lines.isNotEmpty()) {
                    append("""TEXT $left,$baseTextY,"${layout.font}",0,${layout.sx},${layout.sy},"${lines[0]}"""" + "\r\n")
                    if (lines.size >= 2)
                        append("""TEXT $left,${baseTextY + layout.lineStep},"${layout.font}",0,${layout.sx},${layout.sy},"${lines[1]}"""" + "\r\n")
                    if (lines.size >= 3)
                        append("""TEXT $left,${baseTextY + layout.lineStep*2},"${layout.font}",0,${layout.sx},${layout.sy},"${lines[2]}"""" + "\r\n")
                }

                append("PRINT 1,1\r\n")
            }.toByteArray(Charsets.US_ASCII)

            try { sendRaw(sock.outputStream, tspl) }
            catch (e: Exception) { closeSilently(); throw e }
        }
    }

    private data class CaptionLayout(
        val font: String, val sx: Int, val sy: Int,
        val lineStep: Int, val maxPerLine: IntArray,
        val bcHeight: Int
    )

    /**
     * Подбираем макет подписи и высоту ШК под длину текста.
     * - Пробуем font "3", 2 строки: ≈28/26 символов
     * - Если не влезает — font "2", 2 строки: ≈34/32
     * - Если и это длинно — font "2", 3 строки: ≈24/24/24 + уменьшаем высоту ШК
     */
    private fun pickCaptionLayout(caption: String, baseBcHeight: Int): CaptionLayout {
        val len = caption.replace('_', ' ').length

        // 1) font=3, 2 строки
        if (len <= 28 + 26) {
            return CaptionLayout(font = "3", sx = 1, sy = 1,
                lineStep = 22, maxPerLine = intArrayOf(28, 26),
                bcHeight = baseBcHeight)
        }

        // 2) font=2, 2 строки (мельче)
        if (len <= 34 + 32) {
            return CaptionLayout(font = "2", sx = 1, sy = 1,
                lineStep = 20, maxPerLine = intArrayOf(34, 32),
                bcHeight = baseBcHeight)
        }

        // 3) font=2, 3 строки + чуть ниже высота ШК
        return CaptionLayout(font = "2", sx = 1, sy = 1,
            lineStep = 18, maxPerLine = intArrayOf(24, 24, 24),
            bcHeight = (baseBcHeight - 20).coerceAtLeast(150))
    }

    // NEW: компактная печать для возвратов
    suspend fun printTsplFixedSmallCompact(
        context: Context,
        device: BluetoothDevice,
        barcodeText: String,
        captionText: String?
    ) = printMutex.withLock {
        withContext(Dispatchers.IO) {
            val sock = ensureConnected(context, device)
            if (!isPrinterReady(sock)) {
                throw IllegalStateException("Принтер не готов (бумага/крышка/пауза)")
            }

            val len = barcodeText.length

            val narrow = when { len <= 18 -> 2; len <= 24 -> 2; else -> 1 }
            val wide   = when { len <= 18 -> 4; len <= 24 -> 4; else -> 3 }
            val baseBcHeight = when { len <= 18 -> 170; len <= 24 -> 160; else -> 150 }

            // Используем тот же отступ слева, что и в обычной печати
            val left = LABEL_LEFT_MARGIN
            val top  = 12

            val layout = if (!captionText.isNullOrBlank())
                pickCaptionLayout(captionText!!, baseBcHeight - 10) // подпись → ещё -10 к высоте
            else
                CaptionLayout(font = "3", sx = 1, sy = 1, lineStep = 22, maxPerLine = intArrayOf(), bcHeight = baseBcHeight)

            val lines = if (!captionText.isNullOrBlank())
                wrapCaptionSmart(captionText!!, layout.maxPerLine)
            else emptyList()

            val bcHeight = layout.bcHeight
            val baseTextY = (top + bcHeight + 14).coerceAtMost(300)

            val tspl = buildString {
                append("SIZE 58 mm,40 mm\r\n")
                append("GAP 2 mm,0\r\n")
                append("DENSITY 8\r\n")
                append("SPEED 4\r\n")
                append("DIRECTION 1\r\n")
                append("REFERENCE 0,0\r\n")
                append("CLS\r\n")

                // Code128 без подписи принтера
                append("""BARCODE $left,$top,"128",$bcHeight,0,0,$narrow,$wide,"$barcodeText"""" + "\r\n")

                if (lines.isNotEmpty()) {
                    append("""TEXT $left,$baseTextY,"${layout.font}",0,${layout.sx},${layout.sy},"${lines[0]}"""" + "\r\n")
                    if (lines.size >= 2)
                        append("""TEXT $left,${baseTextY + layout.lineStep},"${layout.font}",0,${layout.sx},${layout.sy},"${lines[1]}"""" + "\r\n")
                    if (lines.size >= 3)
                        append("""TEXT $left,${baseTextY + layout.lineStep*2},"${layout.font}",0,${layout.sx},${layout.sy},"${lines[2]}"""" + "\r\n")
                }

                append("PRINT 1,1\r\n")
            }.toByteArray(Charsets.US_ASCII)

            try { sendRaw(sock.outputStream, tspl) }
            catch (e: Exception) { closeSilently(); throw e }
        }
    }

    private fun wrapCaptionSmart(src: String, maxPerLine: IntArray): List<String> {
        if (maxPerLine.isEmpty()) return emptyList()
        val caption = src.replace('_', ' ').trim()
        var remain = caption
        val lines = mutableListOf<String>()

        fun splitOnce(s: String, max: Int): Pair<String, String> {
            if (s.length <= max) return s to ""
            val sep = s.substring(0, max + 1).lastIndexOf(' ')
            if (sep in 8..max) return s.substring(0, sep).trim() to s.substring(sep + 1).trim()
            val idx = s.indexOfFirst { it.isDigit() }
            if (idx in 8 until s.length - 6 && idx <= max) {
                return s.substring(0, idx).trim() to s.substring(idx).trim()
            }
            return s.substring(0, max).trim() to s.substring(max).trim()
        }

        for (i in maxPerLine.indices) {
            val max = maxPerLine[i]
            if (remain.isEmpty()) { lines += ""; continue }

            if (i < maxPerLine.lastIndex) {
                val (head, tail) = splitOnce(remain, max)
                lines += head
                remain = tail
            } else {
                lines += if (remain.length <= max) remain
                else {
                    val keep = max.coerceAtLeast(8)
                    val head = remain.take(keep - 3)
                    val tail = remain.takeLast((keep / 2).coerceAtLeast(4))
                    "$head…$tail"
                }
                remain = ""
            }
        }
        return lines
    }

    private fun wrapCaptionFor58mm(src: String): List<String> {
        val caption = src.replace('_', ' ')
        val max1 = 28
        val max2 = 26

        if (caption.length <= max1) return listOf(caption)

        val sepIdx = caption.lastIndexOf(' ')
        if (sepIdx in 8 until caption.length - 8) {
            val l1 = caption.substring(0, sepIdx).trim()
            val l2 = caption.substring(sepIdx + 1).trim()
            if (l1.length <= max1 && l2.length <= max2) return listOf(l1, l2)
        }

        val digitStart = caption.indexOfFirst { it.isDigit() }
        if (digitStart in 8 until caption.length - 6) {
            val l1 = caption.substring(0, digitStart).trim()
            val l2 = caption.substring(digitStart).trim()
            if (l1.length <= max1 && l2.length <= max2) return listOf(l1, l2)
        }

        val mid = (caption.length / 2).coerceIn(10, caption.length - 10)
        val l1 = caption.substring(0, mid).trim()
        val l2 = caption.substring(mid).trim()
        return listOf(
            if (l1.length <= max1) l1 else l1.take(max1),
            if (l2.length <= max2) l2 else l2.take(max2)
        )
    }
}
