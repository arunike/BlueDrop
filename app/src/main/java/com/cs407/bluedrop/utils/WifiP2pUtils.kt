package com.cs407.bluedrop.utils

import android.net.wifi.p2p.WifiP2pDevice

object WifiP2pUtils {

    fun getDeviceStatus(deviceStatus: Int): String {
        return when (deviceStatus) {
            WifiP2pDevice.AVAILABLE -> "AVAILABLE"
            WifiP2pDevice.INVITED -> "INVITING"
            WifiP2pDevice.CONNECTED -> "CONNECTED"
            WifiP2pDevice.FAILED -> "FAILED"
            WifiP2pDevice.UNAVAILABLE -> "UNAVAILABLE"
            else -> "UNKNOWN"
        }
    }

}