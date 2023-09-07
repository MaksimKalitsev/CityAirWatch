package ua.zp.cityairwatch.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import ua.zp.cityairwatch.data.ConnectionState
import ua.zp.cityairwatch.data.SensorResult
import ua.zp.cityairwatch.data.SensorResultReceiveManager
import ua.zp.cityairwatch.util.Resource
import java.util.UUID
import javax.inject.Inject

@SuppressLint("MissingPermission")
class SensorResultBLEReceiveManager @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter,
    private val context: Context
) : SensorResultReceiveManager {

    private val DEVICE_NAME = "CityAirWatch"
    private val SENSOR_RESULT_SERVICE_UUID = "1958930a-4afc-11ee-be56-0242ac120002"
    private val SENSOR_RESULT_CHARACTERISTICS_UUID = "a581f042-4afc-11ee-be56-0242ac120002"

    override val data: MutableSharedFlow<Resource<SensorResult>> = MutableSharedFlow()

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private var gatt: BluetoothGatt? = null

    private var isScanning = false

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            if (result?.device?.name == DEVICE_NAME) {
                coroutineScope.launch {
                    data.emit(Resource.Loading(message = "Connecting to device..."))
                }
                if (isScanning) {
                    result?.device?.connectGatt(context, false, gattCallback)
                    isScanning = false
                    bleScanner.stopScan(this)
                }
            }
        }
    }

    private var currentConnectionAttempt = 1
    private var maximumConnectionAttempts = 5

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    coroutineScope.launch {
                        data.emit(Resource.Loading(message = "Discovering Services..."))
                    }
                    gatt?.discoverServices()
                    this@SensorResultBLEReceiveManager.gatt = gatt
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    coroutineScope.launch {
                        data.emit(
                            Resource.Success(
                                data = SensorResult(
                                    0f,
                                    0f,
                                    ConnectionState.Disconnected
                                )
                            )
                        )
                    }
                    gatt?.close()
                }
            } else {
                gatt?.close()
                currentConnectionAttempt += 1
                coroutineScope.launch {
                    data.emit(
                        Resource.Loading(
                            message = "Attempting to connect $currentConnectionAttempt/$maximumConnectionAttempts"
                        )
                    )
                }
                if (currentConnectionAttempt <= maximumConnectionAttempts) {
                    startReceiving()
                } else {
                    coroutineScope.launch {
                        data.emit(Resource.Error(errorMessage = "Could not connect to BLE device"))
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                printGattTable()
                coroutineScope.launch {
                    data.emit(Resource.Loading(message = "Adjusting MTU space..."))
                }
                gatt.requestMtu(517)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {

            val characteristic =
                findCharacteristics(SENSOR_RESULT_SERVICE_UUID, SENSOR_RESULT_CHARACTERISTICS_UUID)
            if (characteristic == null) {
                coroutineScope.launch {
                    data.emit(Resource.Error(errorMessage = "Could not find result publisher"))
                }
                return
            }
            enableNotification(characteristic)
        }


        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            with(characteristic) {
                when (uuid) {
                    UUID.fromString(SENSOR_RESULT_CHARACTERISTICS_UUID) -> {
                        val temperature =
                            (value[0].toInt() + (value[1].toInt() shl 8)).toFloat() / 100f
                        val humidity =
                            (value[2].toInt() + (value[3].toInt() shl 8)).toFloat() / 100f
                        val sensorResult = SensorResult(
                            temperature,
                            humidity,
                            ConnectionState.Connected
                        )
                        coroutineScope.launch {
                            data.emit(
                                Resource.Success(data = sensorResult)
                            )
                        }
                    }

                    else -> Unit
                }
            }
        }
    }

    private fun enableNotification(characteristic: BluetoothGattCharacteristic) {
        val cccdUuid = UUID.fromString(CCCD_DESCRIPTOR_UUID)
        val payload = when {
            characteristic.isIndicatable() -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            characteristic.isNotifiable() -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> return
        }
        characteristic.getDescriptor(cccdUuid)?.let { cccdDescriptor ->
            if (gatt?.setCharacteristicNotification(characteristic, true) == false) {
                Log.d("BLEReceiveManager", "Set characteristics notification failed")
                return
            }
            writeDescription(cccdDescriptor, payload)
        }
    }

    private fun writeDescription(descriptor: BluetoothGattDescriptor, payload: ByteArray) {
        gatt?.let { gatt ->
            descriptor.value = payload
            gatt.writeDescriptor(descriptor)
        } ?: error("Not connected to a BLE device!")
    }

    private fun findCharacteristics(
        serviceUUID: String,
        characteristicsUUID: String
    ): BluetoothGattCharacteristic? {
        return gatt?.services?.find { service ->
            service.uuid.toString() == serviceUUID
        }?.characteristics?.find { characteristics ->
            characteristics.uuid.toString() == characteristicsUUID
        }
    }

    override fun startReceiving() {
        coroutineScope.launch {
            data.emit(Resource.Loading(message = "Scanning BLE devices..."))
        }
        isScanning = true
        bleScanner.startScan(null, scanSettings, scanCallback)
    }

    override fun reconnect() {
        gatt?.connect()
    }

    override fun disconnect() {
        gatt?.disconnect()
    }

    override fun closeConnection() {
        bleScanner.stopScan(scanCallback)
        val characteristic =
            findCharacteristics(SENSOR_RESULT_SERVICE_UUID, SENSOR_RESULT_CHARACTERISTICS_UUID)
        if (characteristic != null) {
            disconnectCharacteristic(characteristic)
        }
        gatt?.close()
    }

    private fun disconnectCharacteristic(characteristic: BluetoothGattCharacteristic) {
        val cccdUuid = UUID.fromString(CCCD_DESCRIPTOR_UUID)
        characteristic.getDescriptor(cccdUuid)?.let { cccdDescriptor ->
            if (gatt?.setCharacteristicNotification(characteristic, false) == false) {
                Log.d("BLEReceiveManager", "Set characteristics notification failed")
            }
        }
    }
}