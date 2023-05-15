package com.sijan.bluetoothchat.domain.chat

sealed interface ConnectionResult {
    //types of connection results
    //established || error || class

    object ConnectionEstablished: ConnectionResult
    data class Error(val message: String): ConnectionResult
}