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

//
//@Composable
//fun MyApp(modifier: Modifier = Modifier) {
//    Surface(
//        modifier = modifier.fillMaxSize(),
//        color = Background
//    ) {
//        Column(
//            verticalArrangement = Arrangement.Center,
//            horizontalAlignment = Alignment.CenterHorizontally,
//        ) {
//            WatchButton()
//        }
//    }
//}

//@Composable
//fun WatchButton() {
//    ElevatedButton(
//        onClick = { /*TODO*/ },
//        colors = ButtonDefaults.buttonColors(Color.White),
//        modifier = Modifier.padding(100.dp)
//    ) {
//        Text(
//            text = "Watch",
//            color = TextButton,
//            fontWeight = FontWeight.Bold,
//            fontStyle = FontStyle.Italic,
//            fontSize = 30.sp
//        )
//    }
//}
