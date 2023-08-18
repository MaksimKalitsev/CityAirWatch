package ua.zp.cityairwatch.data

import kotlinx.coroutines.flow.MutableSharedFlow
import ua.zp.cityairwatch.util.Resource

interface SensorResultReceiveManager {

    val data: MutableSharedFlow<Resource<SensorResult>>
    fun reconnect()
    fun disconnect()
    fun startReceiving()
    fun closeConnection()
}