package ua.zp.cityairwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ua.zp.cityairwatch.presentation.Navigation
import ua.zp.cityairwatch.ui.theme.CityAirWatchTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CityAirWatchTheme() {

                Navigation()
            }
        }
    }
}

