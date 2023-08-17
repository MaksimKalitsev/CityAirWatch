package ua.zp.cityairwatch.data

data class FieldsSensorResult(
    val temperature: Float,
    val co2: Float,
    val connectionState: ConnectionState
)
