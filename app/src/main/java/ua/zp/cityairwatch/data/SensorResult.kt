package ua.zp.cityairwatch.data

data class SensorResult(
    val temperature: Float,
    val co2: Float,
    val connectionState: ConnectionState
)
