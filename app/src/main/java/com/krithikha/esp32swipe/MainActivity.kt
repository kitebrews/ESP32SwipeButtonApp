package com.krithikha.esp32swipe

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.util.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasBluetoothPermission()) {
            requestBluetoothPermission()
        }

        setContent {
            ESP32SwipeApp()
        }
    }

    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val connectPermission = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED

            val scanPermission = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED

            connectPermission && scanPermission
        } else {
            true
        }
    }

    private fun requestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.BLUETOOTH_SCAN
                ),
                100
            )
        }
    }
}

@Composable
fun ESP32SwipeApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var bluetoothSocket by remember { mutableStateOf<BluetoothSocket?>(null) }
    var isConnected by remember { mutableStateOf(false) }

    val esp32MacAddress = "F4:65:0B:4A:5F:3A"

    fun sendSwipeCommandToService(command: String) {
        val intent = Intent(context, SwipeAccessibilityService::class.java)
        intent.putExtra("COMMAND", command)
        context.startService(intent)
    }

    fun startListening(socket: BluetoothSocket) {
        scope.launch(Dispatchers.IO) {
            try {
                val reader = socket.inputStream.bufferedReader()
                while (true) {
                    val line = reader.readLine() ?: break
                    Log.d("ESP32SwipeApp", "Received: $line")
                    when {
                        line.contains("SWIPE_DOWN", ignoreCase = true) -> {
                            Log.d("ESP32SwipeApp", "Triggering SWIPE_DOWN via Accessibility")
                            sendSwipeCommandToService("SWIPE_DOWN")
                        }
                        line.contains("SWIPE_UP", ignoreCase = true) -> {
                            Log.d("ESP32SwipeApp", "Triggering SWIPE_UP via Accessibility")
                            sendSwipeCommandToService("SWIPE_UP")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ESP32SwipeApp", "Error reading from Bluetooth", e)
            }
        }
    }

    val connectToESP32 = {
        scope.launch(Dispatchers.IO) {
            try {
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

                if (ActivityCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.e("ESP32SwipeApp", "BLUETOOTH_CONNECT permission NOT granted!")
                    return@launch
                }

                val device = bluetoothAdapter.getRemoteDevice(esp32MacAddress)
                val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                val socket = device.createRfcommSocketToServiceRecord(uuid)

                bluetoothAdapter.cancelDiscovery()
                socket.connect()

                bluetoothSocket = socket
                isConnected = true

                startListening(socket)

                Log.d("ESP32SwipeApp", "Connected to ESP32!")
            } catch (e: Exception) {
                Log.e("ESP32SwipeApp", "Connection failed", e)
            }
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "ESP32 Swipe Controller",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { connectToESP32() },
                enabled = !isConnected
            ) {
                Text(if (isConnected) "Connected!" else "Connect to ESP32")
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Press ESP32 button for swipe up/down in other apps.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
