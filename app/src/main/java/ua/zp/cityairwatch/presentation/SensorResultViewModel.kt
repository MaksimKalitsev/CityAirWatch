package ua.zp.cityairwatch.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ua.zp.cityairwatch.data.ConnectionState
import ua.zp.cityairwatch.data.SensorResultReceiveManager
import ua.zp.cityairwatch.util.Resource
import javax.inject.Inject

@HiltViewModel
class SensorResultViewModel @Inject constructor(
    private val sensorResultReceiveManager: SensorResultReceiveManager
) : ViewModel() {

    var initializingMessage by mutableStateOf<String?>(null)
    var errorMessage by mutableStateOf<String?>(null)
    var temperature by mutableStateOf(0f)
    var humidity by mutableStateOf(0f)
    var connectionState by mutableStateOf<ConnectionState>(ConnectionState.Uninitialized)

    private fun subscribeToChanges() {
        viewModelScope.launch {
            sensorResultReceiveManager.data.collect { result ->
                when (result) {
                    is Resource.Success -> {
                        connectionState = result.data.connectionState
                        temperature = result.data.temperature
                        humidity = result.data.humidity
                    }

                    is Resource.Loading -> {
                        initializingMessage = result.message
                        connectionState = ConnectionState.CurrentlyInitialized
                    }

                    is Resource.Error -> {
                        errorMessage = result.errorMessage
                        connectionState = ConnectionState.Uninitialized
                    }
                }
            }
        }
    }

    fun disconnect(){
        sensorResultReceiveManager.disconnect()
    }

    fun reconnect(){
        sensorResultReceiveManager.reconnect()
    }

    fun initializeConnection(){
        errorMessage = null
        subscribeToChanges()
        sensorResultReceiveManager.startReceiving()
    }

    override fun onCleared() {
        super.onCleared()
        sensorResultReceiveManager.closeConnection()
    }

}