package ua.zp.cityairwatch.presentation

import android.bluetooth.BluetoothAdapter
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Lifecycling.lifecycleEventObserver
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import ua.zp.cityairwatch.data.ConnectionState
import ua.zp.cityairwatch.presentation.permissions.PermissionUtils
import ua.zp.cityairwatch.presentation.permissions.SystemBroadcastReceiver
import ua.zp.cityairwatch.ui.theme.Background

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ResultScreen(
    onBluetoothStateChanged: () -> Unit,
    viewModel: SensorResultViewModel = hiltViewModel()
) {

    SystemBroadcastReceiver(systemAction = BluetoothAdapter.ACTION_STATE_CHANGED) { bluetoothState ->
        val action = bluetoothState?.action ?: return@SystemBroadcastReceiver
        if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            onBluetoothStateChanged()
        }
    }

    val permissionsState =
        rememberMultiplePermissionsState(permissions = PermissionUtils.permissions)
    val lifecycleOwner = LocalLifecycleOwner.current
    val bleConnectionState = viewModel.connectionState

    DisposableEffect(
        key1 = lifecycleOwner,
        effect = {
            val observer =
                lifecycleEventObserver { source: LifecycleOwner, event: Lifecycle.Event ->
                    if (event == Lifecycle.Event.ON_START) {
                        permissionsState.launchMultiplePermissionRequest()
                        if (permissionsState.allPermissionsGranted && bleConnectionState == ConnectionState.Disconnected) {
                            viewModel.reconnect()
                        }
                    }
                    if (event == Lifecycle.Event.ON_STOP) {
                        if (bleConnectionState == ConnectionState.Connected) {
                            viewModel.disconnect()
                        }
                    }
                }
            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    )

    LaunchedEffect(key1 = permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            if (bleConnectionState == ConnectionState.Uninitialized) {
                viewModel.initializeConnection()
            }
        }
    }

    Box(
        modifier = Modifier
            .background(Background)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (bleConnectionState == ConnectionState.CurrentlyInitialized) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    if (viewModel.initializingMessage != null) {
                        Text(
                            text = viewModel.initializingMessage!!
                        )
                    }
                }
            } else if (!permissionsState.allPermissionsGranted) {
                Text(
                    text = "Go to the app setting and allow the missing permissions.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(10.dp),
                    textAlign = TextAlign.Center
                )
            } else if (viewModel.errorMessage != null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = viewModel.errorMessage!!
                    )
                    Button(
                        onClick = {
                            if (permissionsState.allPermissionsGranted) {
                                viewModel.initializeConnection()
                            }
                        }
                    ) {
                        Text(
                            "Try again"
                        )
                    }
                }
            } else if (bleConnectionState == ConnectionState.Connected) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircleImage(
                        circleSize = 150.dp,
                        headText = "Humidity:",
                        resultText = "\n${viewModel.humidity} %",
                        humidity = viewModel.humidity
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    CircleImage(
                        circleSize = 150.dp,
                        headText = "Temperature:",
                        resultText = "\n${viewModel.temperature} °C",
                        temperature = viewModel.temperature
                    )
                }
            } else if (bleConnectionState == ConnectionState.Disconnected) {
                Button(onClick = {
                    viewModel.initializeConnection()
                }) {
                    Text("Initialize again")
                }
            }
        }

    }
}

@Composable
fun CircleImage(circleSize: Dp, headText: String, resultText: String, temperature: Float? = null, humidity: Float? = null) {

    val bgColor = when {
        temperature != null -> temperatureToColor(temperature)
        humidity != null -> humidityToColor(humidity)
        else -> Color.Gray
    }
    val color by animateColorAsState(
        targetValue = bgColor,
        animationSpec = tween(
            durationMillis = 2000,
            easing = LinearEasing
        )
    )
    Box(
        modifier = Modifier
            .size(circleSize)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        val annotatedText = buildAnnotatedString {
            withStyle(style = SpanStyle(fontSize = 16.sp)) {
                append(headText)
            }
            withStyle(style = SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)) {
                append(resultText)
            }
        }
        Text(
            text = annotatedText,
            textAlign = TextAlign.Center
        )
    }
}
fun temperatureToColor(temperature: Float): Color {
    val normalizedTemperature = ((temperature + 20) / 70).coerceIn(0f, 1f)
    return Color(
        red = normalizedTemperature,
        green = 0f,
        blue = 1f - normalizedTemperature
    )
}
fun humidityToColor(humidity: Float): Color {
    val normalizedHumidity = (humidity / 100f).coerceIn(0f, 1f)
    return Color(
        red = 1f - normalizedHumidity,
        green = normalizedHumidity,
        blue = normalizedHumidity
    )
}


