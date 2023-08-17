package ua.zp.cityairwatch.data

sealed interface ConnectionState {
    object Connected : ConnectionState
    object Disconnected : ConnectionState
    object Uninitialized : ConnectionState
    object CurrentlyInitialized : ConnectionState
}
