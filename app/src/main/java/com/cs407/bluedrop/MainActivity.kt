package com.cs407.bluedrop

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.cs407.bluedrop.receiver.FileReceiverActivity
import com.cs407.bluedrop.sender.FileSenderActivity

class MainActivity : BaseActivity() {

    private val requestedPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.NEARBY_WIFI_DEVICES
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private val requestPermissionLaunch = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { it ->
        if (it.all { it.value }) {
            showToast("All Permission Granted ")
        } else {
            onPermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.btnCheckPermission).setOnClickListener {
            requestPermissionLaunch.launch(requestedPermissions)
        }
        findViewById<View>(R.id.btnSender).setOnClickListener {
            if (allPermissionGranted()) {
                startActivity(FileSenderActivity::class.java)
            } else {
                onPermissionDenied()
            }
        }
        findViewById<View>(R.id.btnReceiver).setOnClickListener {
            if (allPermissionGranted()) {
                startActivity(FileReceiverActivity::class.java)
            } else {
                onPermissionDenied()
            }
        }
    }

    private fun onPermissionDenied() {
        showToast("Please Give Permission First")
    }

    private fun allPermissionGranted(): Boolean {
        requestedPermissions.forEach {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    it
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

}