package com.sijan.bluetoothchat.domain.chat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {

    val isConnected: StateFlow<Boolean>
    val scannedDevices: StateFlow<List<BluetoothDevice>>
    val pairedDevices: StateFlow<List<BluetoothDevice>>
    val errors: SharedFlow<String>
    //shared flows are used to send on time events

    fun startDiscovery()
    fun stopDiscovery()

    fun release()

    fun startBluetoothServer(): Flow<ConnectionResult>

    fun connectToDevice(device: BluetoothDevice): Flow<ConnectionResult>

    fun closeConnection()

}