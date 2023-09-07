package ua.zp.cityairwatch.data

data class SensorResult(
    val temperature: Float,
    val humidity: Float,
    val connectionState: ConnectionState
)
