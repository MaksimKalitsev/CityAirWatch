package ua.zp.cityairwatch.presentation.permissions

import android.os.Build
import android.Manifest

object PermissionUtils {

    val permissions = if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }else{
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
}