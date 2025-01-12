package com.example.tictactoe
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import java.io.IOException
import java.util.*

class BluetoothClient(
    private val device: BluetoothDevice,
    private val context: Context,
    private val onConnected: (BluetoothSocket) -> Unit
) {
    private val MY_UUID: UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")

    fun connect() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                    if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                }

                val socket: BluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
                socket.connect()
                onConnected(socket)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
