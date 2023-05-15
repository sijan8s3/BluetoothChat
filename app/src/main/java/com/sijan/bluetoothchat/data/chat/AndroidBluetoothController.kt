package com.sijan.bluetoothchat.data.chat

import android.Manifest
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice.ACTION_FOUND
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import com.sijan.bluetoothchat.domain.chat.BluetoothController
import com.sijan.bluetoothchat.domain.chat.BluetoothDevice
import com.sijan.bluetoothchat.domain.chat.BluetoothDeviceDomain
import com.sijan.bluetoothchat.domain.chat.ConnectionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

@SuppressLint("MissingPermission")
class AndroidBluetoothController(
    private val context: Context
) : BluetoothController {


    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private val _isConnected = MutableStateFlow<Boolean>(false)
    override val isConnected: StateFlow<Boolean>
        get() = _isConnected.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothDevice>>
        get() = _pairedDevices.asStateFlow()

    private val _errors= MutableSharedFlow<String>()
    override val errors: SharedFlow<String>
        get() = _errors.asSharedFlow()

    private var currentServerSocket: BluetoothServerSocket? = null
    private var currentClientSocket: BluetoothSocket? = null


    private val foundDeviceReceiver = FoundDeviceReceiver { device ->
        _scannedDevices.update { devices ->
            val newDevice = device.toBluetoothDeviceDomain()
            if (newDevice in devices) devices else devices + newDevice
        }
    }

    private val bluetoothStaterReceiver = BluetoothStateReceiver { isConnected, bluetoothDevice ->
        if (bluetoothAdapter?.bondedDevices?.contains(bluetoothDevice) == true) {
            //update connection state if only paired to that device
            _isConnected.update { isConnected }
        }else{
            CoroutineScope(Dispatchers.IO).launch {
                _errors.tryEmit("Can't connect to non paired device")
            }
        }

    }


    init {
        updatePairedDevices()
        context.registerReceiver(
            bluetoothStaterReceiver,
            IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
                addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED)
            }
        )
    }

    override fun startDiscovery() {
        if (!hasPermission(BLUETOOTH_SCAN)) {
            return
        }
        context.registerReceiver(
            foundDeviceReceiver, IntentFilter(ACTION_FOUND)
        )
        updatePairedDevices()
        bluetoothAdapter?.startDiscovery()
    }

    override fun stopDiscovery() {
        if (!hasPermission(BLUETOOTH_SCAN)) {
            return
        }
        bluetoothAdapter?.cancelDiscovery()
    }

    override fun release() {
        context.unregisterReceiver(foundDeviceReceiver)
        context.unregisterReceiver(bluetoothStaterReceiver)
        closeConnection()
    }

    override fun startBluetoothServer(): Flow<ConnectionResult> {
        return flow {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                throw SecurityException("NO BLUETOOTH CONNECTION PERMISSION")
            }
            //launch server
            currentServerSocket = bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord(
                "chat_service",
                UUID.fromString(SERVICE_UUID)
            )
            //block background thread as long as we accept conn
            var shouldLoop = true
            while (shouldLoop) {
                currentClientSocket = try {
                    currentServerSocket?.accept()
                } catch (e: IOException) {
                    shouldLoop = false
                    null
                }
                emit(ConnectionResult.ConnectionEstablished)
                //after accepted connection
                currentClientSocket?.let {
                    //close server socket
                    //we communicate with client socket
                    currentServerSocket?.close()
                }
            }
        }.onCompletion {
            closeConnection()
        }.flowOn(
            //executing it on IO thread
            Dispatchers.IO
        )
    }

    override fun connectToDevice(device: BluetoothDeviceDomain): Flow<ConnectionResult> {
        return flow {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                throw SecurityException("NO BLUETOOTH CONNECTION PERMISSION")
            }
            val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)

            //connect to device
            currentClientSocket = bluetoothDevice
                ?.createRfcommSocketToServiceRecord(UUID.fromString(SERVICE_UUID))

            stopDiscovery()

            if (bluetoothAdapter?.bondedDevices?.contains(bluetoothDevice) == false) {
                //not paired to the device

            }

            currentClientSocket?.let { socket ->
                try {
                    socket.connect()
                    emit(ConnectionResult.ConnectionEstablished)


                } catch (e: IOException) {
                    socket.close()
                    currentClientSocket = null
                    emit(ConnectionResult.Error("Interrupted: ${e.message}"))
                }
            }

        }.onCompletion {
            closeConnection()
        }.flowOn(
            //executing it on IO thread
            Dispatchers.IO
        )
    }

    override fun closeConnection() {
        currentClientSocket?.close()
        currentServerSocket?.close()
        currentClientSocket = null
        currentServerSocket = null
    }

    private fun updatePairedDevices() {
        if (!hasPermission(BLUETOOTH_CONNECT)) {
            return
        }
        bluetoothAdapter?.bondedDevices?.map {
            it.toBluetoothDeviceDomain()
        }?.also { devices -> _pairedDevices.update { devices } }
    }

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val SERVICE_UUID = "5bf9581e-f239-11ed-a05b-0242ac120003"
    }
}