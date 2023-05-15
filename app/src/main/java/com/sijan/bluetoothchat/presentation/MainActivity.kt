package com.sijan.bluetoothchat.presentation

import android.Manifest
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.sijan.bluetoothchat.presentation.components.DeviceScreen
import com.sijan.bluetoothchat.ui.theme.BluetoothChatTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val bluetoothManager by lazy {
        applicationContext.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }
    private val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val enableBluetoothLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {

        }
        //permissions
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { perms ->
            val canEnableBluetooth =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    perms[Manifest.permission.BLUETOOTH_CONNECT] == true
                } else true
            if (canEnableBluetooth && !isBluetoothEnabled) {
                enableBluetoothLauncher.launch(
                    Intent(ACTION_REQUEST_ENABLE)
                )
            }
        }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissionLauncher.launch(
                    arrayOf(
                        BLUETOOTH_SCAN, BLUETOOTH_CONNECT
                    )
                )
            }


        setContent {
            BluetoothChatTheme {
                val viewModel = hiltViewModel<BluetoothViewModel>()
                val state by viewModel.state.collectAsState()

                LaunchedEffect(key1 = state.errorMessage){
                    state.errorMessage?.let { message->
                        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
                    }
                }
                
                LaunchedEffect(key1 = state.isConnected){
                    if (state.isConnected){
                        Toast.makeText(applicationContext, "You're connected!", Toast.LENGTH_SHORT).show()
                    }
                }


                Surface(
                    color = MaterialTheme.colors.background
                ) {
                    when{
                        state.isConnecting-> {
                            Column(modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center){
                                CircularProgressIndicator()
                                Text(text = "Connecting...")
                            }
                        }
                        else -> {
                            DeviceScreen(
                                onStartScan = viewModel::startScan,
                                onStopScan = viewModel::stopScan,
                                state = state,
                                onDeviceClick = viewModel::connectToDevice,
                                onStartServer = viewModel::waitForIncomingConnections
                            )
                        }
                    }

                }

            }
        }
    }
}
