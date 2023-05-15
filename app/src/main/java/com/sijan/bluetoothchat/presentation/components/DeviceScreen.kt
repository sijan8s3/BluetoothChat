package com.sijan.bluetoothchat.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sijan.bluetoothchat.domain.chat.BluetoothDevice
import com.sijan.bluetoothchat.presentation.BluetoothUiState

@Composable
fun DeviceScreen(
    state: BluetoothUiState,
    onStartScan:() -> Unit,
    onStopScan:() -> Unit,
    onStartServer: () -> Unit,
    onDeviceClick: (BluetoothDevice) -> Unit
){
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {

        BluetoothDeviceList(scannedDevices = state.scannedDevices, pairedDevices =state.pairedDevices , onClick = onDeviceClick,
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f))

        Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround) {
            Button(onClick = onStartScan) {
                Text(text = "Start Scan")
            }
            Button(onClick = onStopScan) {
                Text(text = "Stop Scan")
            }
            Button(onClick = onStartServer) {
                Text(text = "Start Server")
            }
        }
        
    }

}

@Composable
fun BluetoothDeviceList(
    scannedDevices: List<BluetoothDevice>,
    pairedDevices: List<BluetoothDevice>,
    onClick: (BluetoothDevice) -> Unit,
    modifier: Modifier= Modifier
){
    LazyColumn(modifier = modifier){
        //Header
        item {
            Text(text = "Paired Devices",
            fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
        //Paired Device List
        items(pairedDevices) {device ->
            Text(text = device.name ?: "<NO NAME>",
            modifier= Modifier
                .fillMaxWidth()
                .clickable { onClick(device) }
                .padding(16.dp)
            )
        }
        //Header
        item {
            Text(text = "Scanned Devices",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
        //Scanned Device List
        items(scannedDevices) {device ->
            Text(text = device.name ?: "<NO NAME>",
                modifier= Modifier
                    .fillMaxWidth()
                    .clickable { onClick(device) }
                    .padding(16.dp)
            )
        }
    }
}