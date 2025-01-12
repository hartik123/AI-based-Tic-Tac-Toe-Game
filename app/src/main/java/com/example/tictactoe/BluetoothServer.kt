package com.example.tictactoe

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import java.io.IOException
import java.util.*

class BluetoothServer(
    private val adapter: BluetoothAdapter,
    private val context: Context,
    private val onConnected: (BluetoothSocket) -> Unit
) {
    private val SERVICE_NAME = "TicTacToe"
    private val MY_UUID: UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")

    fun startListening() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return@launch
                }
                val serverSocket: BluetoothServerSocket =
                    adapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, MY_UUID)
                val socket = serverSocket.accept()
                onConnected(socket)
                serverSocket.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
