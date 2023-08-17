package ua.zp.cityairwatch.presentation

import android.bluetooth.BluetoothAdapter
import androidx.compose.runtime.Composable
import ua.zp.cityairwatch.presentation.permissions.SystemBroadcastReceiver

@Composable
fun ResultScreen(
    onBluetoothStateChanged: () -> Unit
) {

    SystemBroadcastReceiver(systemAction = BluetoothAdapter.ACTION_STATE_CHANGED) { bluetoothState ->
        val action = bluetoothState?.action ?: return@SystemBroadcastReceiver
        if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            onBluetoothStateChanged()
        }
    }
}