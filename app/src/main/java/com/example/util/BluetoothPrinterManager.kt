package com.example.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID

sealed class PrinterConnectionState {
    object Disconnected : PrinterConnectionState()
    object Connecting : PrinterConnectionState()
    data class Connected(val deviceName: String, val address: String) : PrinterConnectionState()
    data class Error(val message: String) : PrinterConnectionState()
}

class BluetoothPrinterManager(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    private val _connectionState = MutableStateFlow<PrinterConnectionState>(PrinterConnectionState.Disconnected)
    val connectionState: StateFlow<PrinterConnectionState> = _connectionState.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDevice>> = _pairedDevices.asStateFlow()

    companion object {
        private const val TAG = "BmkPrinter"
        // Standard SPP UUID for thermal printers
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    val isBluetoothSupported: Boolean
        get() = bluetoothAdapter != null

    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        try {
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
                val devices = bluetoothAdapter.bondedDevices.toList()
                _pairedDevices.value = devices
            } else {
                _pairedDevices.value = emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching bonded devices: ${e.message}")
            _pairedDevices.value = emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun connectToDevice(device: BluetoothDevice): Boolean = withContext(Dispatchers.IO) {
        _connectionState.value = PrinterConnectionState.Connecting
        try {
            disconnect()
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            bluetoothAdapter?.cancelDiscovery()
            socket.connect()
            bluetoothSocket = socket
            outputStream = socket.outputStream
            val name = try { device.name ?: "Thermal Printer" } catch (e: SecurityException) { "Thermal Printer" }
            _connectionState.value = PrinterConnectionState.Connected(name, device.address)
            Log.i(TAG, "Connected to printer: $name")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to printer: ${e.message}")
            _connectionState.value = PrinterConnectionState.Error("ချိတ်ဆက်မှု မအောင်မြင်ပါ: ${e.localizedMessage}")
            disconnect()
            false
        }
    }

    fun disconnect() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting: ${e.message}")
        } finally {
            outputStream = null
            bluetoothSocket = null
            if (_connectionState.value !is PrinterConnectionState.Error) {
                _connectionState.value = PrinterConnectionState.Disconnected
            }
        }
    }

    suspend fun printBitmap(bitmap: Bitmap): Boolean = withContext(Dispatchers.IO) {
        val stream = outputStream
        if (stream == null || bluetoothSocket?.isConnected != true) {
            _connectionState.value = PrinterConnectionState.Error("ပရင်တာနှင့် ချိတ်ဆက်ထားခြင်း မရှိပါ (Printer Not Connected)")
            return@withContext false
        }
        try {
            // ESC/POS Init
            stream.write(byteArrayOf(0x1B, 0x40)) // ESC @ (Initialize printer)

            // Convert Bitmap to ESC/POS Raster bit image (GS v 0)
            val rasterBytes = bitmapToEscPosRaster(bitmap)
            stream.write(rasterBytes)

            // Feed and Cut lines
            stream.write(byteArrayOf(0x1B, 0x64, 0x05)) // ESC d 5 (feed 5 lines)
            stream.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00)) // GS V 66 0 (cut paper if supported)
            stream.flush()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Print error: ${e.message}")
            _connectionState.value = PrinterConnectionState.Error("Print ထုတ်ရာတွင် အမှားဖြစ်ပေါ်ပါသည်: ${e.message}")
            false
        }
    }

    /**
     * Converts an Android Bitmap into ESC/POS GS v 0 raster bit image bytes.
     * This faithfully renders all Burmese glyphs, tables, and borders on 58mm/80mm/100mm thermal heads.
     */
    private fun bitmapToEscPosRaster(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val widthBytes = (width + 7) / 8

        val header = byteArrayOf(
            0x1D, 0x76, 0x30, 0x00, // GS v 0 m (m=0 normal)
            (widthBytes and 0xFF).toByte(),
            ((widthBytes shr 8) and 0xFF).toByte(),
            (height and 0xFF).toByte(),
            ((height shr 8) and 0xFF).toByte()
        )

        val imageBytes = ByteArray(widthBytes * height)
        var byteIndex = 0

        for (y in 0 until height) {
            for (xByte in 0 until widthBytes) {
                var currentByte = 0
                for (bit in 0 until 8) {
                    val x = xByte * 8 + bit
                    if (x < width) {
                        val pixel = bitmap.getPixel(x, y)
                        val r = Color.red(pixel)
                        val g = Color.green(pixel)
                        val b = Color.blue(pixel)
                        // Luminance threshold for monochrome conversion
                        val luminance = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
                        if (luminance < 165 && Color.alpha(pixel) > 100) {
                            currentByte = currentByte or (1 shl (7 - bit))
                        }
                    }
                }
                imageBytes[byteIndex++] = currentByte.toByte()
            }
        }

        return header + imageBytes
    }
}
