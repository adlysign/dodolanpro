package com.example.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID

object BluetoothPrinterHelper {
    private const val TAG = "BluetoothPrinterHelper"
    // Standard Serial Port Profile (SPP) UUID for ESC/POS thermal printers
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    /**
     * Check if Bluetooth permissions are granted.
     */
    fun hasBluetoothPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Get list of paired / bonded Bluetooth devices (Thermal printers usually show up here).
     */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(context: Context): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
            if (adapter != null && adapter.isEnabled) {
                val bonded = adapter.bondedDevices
                if (bonded != null) {
                    for (device: BluetoothDevice in bonded) {
                        val name = device.name ?: "Unknown Bluetooth Device"
                        val address = device.address ?: ""
                        if (address.isNotEmpty()) {
                            result.add(Pair(name, address))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing paired devices: ${e.message}")
        }
        return result
    }

    /**
     * Send ESC/POS raw bytes directly to Bluetooth thermal printer via SPP socket.
     */
    @SuppressLint("MissingPermission")
    suspend fun printOverBluetooth(macAddress: String, data: ByteArray): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (macAddress.isBlank()) {
            return@withContext Pair(false, "MAC Address printer Bluetooth belum ditentukan.")
        }

        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null

        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
                ?: return@withContext Pair(false, "Bluetooth tidak didukung pada perangkat ini.")

            if (!adapter.isEnabled) {
                return@withContext Pair(false, "Bluetooth sedang nonaktif. Silakan aktifkan Bluetooth.")
            }

            val device: BluetoothDevice = try {
                adapter.getRemoteDevice(macAddress.trim())
            } catch (e: Exception) {
                return@withContext Pair(false, "Format MAC Address tidak valid: ${e.message}")
            }

            adapter.cancelDiscovery()

            socket = try {
                device.createRfcommSocketToServiceRecord(SPP_UUID)
            } catch (e: Exception) {
                // Fallback using reflection for older or stubborn thermal printers
                val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                method.invoke(device, 1) as BluetoothSocket
            }

            socket.connect()
            outputStream = socket.outputStream

            outputStream.write(data)
            outputStream.flush()

            // Small delay to ensure hardware buffer finishes
            kotlinx.coroutines.delay(200)

            Pair(true, "Berhasil mengirim data cetak ke printer Bluetooth ($macAddress)")
        } catch (e: Exception) {
            Log.e(TAG, "Bluetooth print failed: ${e.message}", e)
            Pair(false, "Gagal terhubung ke printer ($macAddress): ${e.localizedMessage ?: e.message}")
        } finally {
            try {
                outputStream?.close()
            } catch (_: Exception) {}
            try {
                socket?.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Send ESC/POS command to kick open cash drawer (RJ11 connected to thermal printer).
     */
    suspend fun kickCashDrawer(macAddress: String): Pair<Boolean, String> {
        val drawerCommand = byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte())
        return printOverBluetooth(macAddress, drawerCommand)
    }

    /**
     * Send ESC/POS command to cut paper.
     */
    suspend fun cutPaper(macAddress: String): Pair<Boolean, String> {
        val cutCommand = byteArrayOf(0x1D, 0x56, 0x42, 0x00)
        return printOverBluetooth(macAddress, cutCommand)
    }

    /**
     * Share or export receipt to DodolanPro/Struk and open Android Intent.
     */
    fun shareReceiptText(context: Context, receiptText: String, title: String): Pair<Boolean, String> {
        try {
            val saved = DocExportManager.saveDocument(
                context = context,
                subfolder = "Struk",
                prefix = "Struk",
                extension = "txt",
                content = receiptText
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, receiptText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Cetak / Bagikan Struk").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })

            return Pair(true, "Struk tersimpan di DodolanPro/Struk/${saved.second.name}")
        } catch (e: Exception) {
            return Pair(false, "Gagal membagikan struk: ${e.message}")
        }
    }
}
