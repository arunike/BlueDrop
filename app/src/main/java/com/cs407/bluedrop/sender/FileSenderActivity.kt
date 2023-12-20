package com.cs407.bluedrop.sender

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cs407.bluedrop.BaseActivity
import com.cs407.bluedrop.DeviceAdapter
import com.cs407.bluedrop.DirectActionListener
import com.cs407.bluedrop.DirectBroadcastReceiver
import com.cs407.bluedrop.OnItemClickListener
import com.cs407.bluedrop.R
import com.cs407.bluedrop.models.ViewState
import com.cs407.bluedrop.utils.WifiP2pUtils
import kotlinx.coroutines.launch

@SuppressLint("NotifyDataSetChanged")
class FileSenderActivity : BaseActivity() {

    private val tvDeviceState by lazy {
        findViewById<TextView>(R.id.tvDeviceState)
    }

    private val tvConnectionStatus by lazy {
        findViewById<TextView>(R.id.tvConnectionStatus)
    }

    private val btnDisconnect by lazy {
        findViewById<Button>(R.id.btnDisconnect)
    }

    private val btnChooseFile by lazy {
        findViewById<Button>(R.id.btnChooseFile)
    }

    private val rvDeviceList by lazy {
        findViewById<RecyclerView>(R.id.rvDeviceList)
    }

    private val tvLog by lazy {
        findViewById<TextView>(R.id.tvLog)
    }

    private val btnDirectDiscover by lazy {
        findViewById<Button>(R.id.btnDirectDiscover)
    }

    private val fileSenderViewModel by viewModels<FileSenderViewModel>()

    private val getContentLaunch = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { imageUri ->
        if (imageUri != null) {
            val ipAddress = wifiP2pInfo?.groupOwnerAddress?.hostAddress
            log("getContentLaunch $imageUri $ipAddress")
            if (!ipAddress.isNullOrBlank()) {
                fileSenderViewModel.send(ipAddress = ipAddress, fileUri = imageUri)
            }
        }
    }

    private val wifiP2pDeviceList = mutableListOf<WifiP2pDevice>()

    private val deviceAdapter = DeviceAdapter(wifiP2pDeviceList)

    private var broadcastReceiver: BroadcastReceiver? = null

    private lateinit var wifiP2pManager: WifiP2pManager

    private lateinit var wifiP2pChannel: WifiP2pManager.Channel

    private var wifiP2pInfo: WifiP2pInfo? = null

    private var WIFIEnabled = false

    private val directActionListener = object : DirectActionListener {

        override fun WIFIEnabled(enabled: Boolean) {
            WIFIEnabled = enabled
        }

        override fun onConnectionInfoAvailable(wifiP2pInfo: WifiP2pInfo) {
            dismissLoadingDialog()
            wifiP2pDeviceList.clear()
            deviceAdapter.notifyDataSetChanged()
            btnDisconnect.isEnabled = true
            btnChooseFile.isEnabled = true
            log("onConnectionInfoAvailable")
            log("onConnectionInfoAvailable groupFormed: " + wifiP2pInfo.groupFormed)
            log("onConnectionInfoAvailable isGroupOwner: " + wifiP2pInfo.isGroupOwner)
            log("onConnectionInfoAvailable getHostAddress: " + wifiP2pInfo.groupOwnerAddress.hostAddress)
            val stringBuilder = StringBuilder()
            stringBuilder.append("\n")
            stringBuilder.append("Are You Host：")
            stringBuilder.append(if (wifiP2pInfo.isGroupOwner) "Yes" else "No")
            stringBuilder.append("\n")
            stringBuilder.append("Host IP Address：")
            stringBuilder.append(wifiP2pInfo.groupOwnerAddress.hostAddress)
            tvConnectionStatus.text = stringBuilder
            if (wifiP2pInfo.groupFormed && !wifiP2pInfo.isGroupOwner) {
                this@FileSenderActivity.wifiP2pInfo = wifiP2pInfo
            }
        }

        override fun onDisconnection() {
            log("onDisconnection")
            btnDisconnect.isEnabled = false
            btnChooseFile.isEnabled = false
            wifiP2pDeviceList.clear()
            deviceAdapter.notifyDataSetChanged()
            tvConnectionStatus.text = null
            wifiP2pInfo = null
            showToast("Currently Disconnected")
        }

        override fun onSelfDeviceAvailable(wifiP2pDevice: WifiP2pDevice) {
            log("onSelfDeviceAvailable")
            log("DeviceName: " + wifiP2pDevice.deviceName)
            log("DeviceAddress: " + wifiP2pDevice.deviceAddress)
            log("Status: " + wifiP2pDevice.status)
            val log = "deviceName：" + wifiP2pDevice.deviceName + "\n" +
                    "deviceAddress：" + wifiP2pDevice.deviceAddress + "\n" +
                    "deviceStatus：" + WifiP2pUtils.getDeviceStatus(wifiP2pDevice.status)
            tvDeviceState.text = log
        }

        override fun onPeersAvailable(wifiP2pDeviceList: Collection<WifiP2pDevice>) {
            log("onPeersAvailable :" + wifiP2pDeviceList.size)
            this@FileSenderActivity.wifiP2pDeviceList.clear()
            this@FileSenderActivity.wifiP2pDeviceList.addAll(wifiP2pDeviceList)
            deviceAdapter.notifyDataSetChanged()
            dismissLoadingDialog()
        }

        override fun onChannelDisconnected() {
            log("onChannelDisconnected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_sender)
        initView()
        initDevice()
        initEvent()
    }

    @SuppressLint("MissingPermission")
    private fun initView() {
        supportActionBar?.title = "File Sender"
        btnDisconnect.setOnClickListener {
            disconnect()
        }
        btnChooseFile.setOnClickListener {
            getContentLaunch.launch("image/*")
        }
        btnDirectDiscover.setOnClickListener {
            if (!WIFIEnabled) {
                showToast("Please turn on Wifi first")
                return@setOnClickListener
            }
            showLoadingDialog(message = "Searching for nearby devices")
            wifiP2pDeviceList.clear()
            deviceAdapter.notifyDataSetChanged()
            wifiP2pManager.discoverPeers(wifiP2pChannel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    showToast("discoverPeers Success")
                    dismissLoadingDialog()
                }

                override fun onFailure(reasonCode: Int) {
                    showToast("discoverPeers Failure：$reasonCode")
                    dismissLoadingDialog()
                }
            })
        }
        deviceAdapter.onItemClickListener = object : OnItemClickListener {
            override fun onItemClick(position: Int) {
                val wifiP2pDevice = wifiP2pDeviceList.getOrNull(position)
                if (wifiP2pDevice != null) {
                    connect(wifiP2pDevice = wifiP2pDevice)
                }
            }
        }
        rvDeviceList.adapter = deviceAdapter
        rvDeviceList.layoutManager = object : LinearLayoutManager(this) {
            override fun canScrollVertically(): Boolean {
                return false
            }
        }
    }

    private fun initDevice() {
        val mWifiP2pManager = getSystemService(WIFI_P2P_SERVICE) as? WifiP2pManager
        if (mWifiP2pManager == null) {
            finish()
            return
        }
        wifiP2pManager = mWifiP2pManager
        wifiP2pChannel = mWifiP2pManager.initialize(this, mainLooper, directActionListener)
        broadcastReceiver =
            DirectBroadcastReceiver(mWifiP2pManager, wifiP2pChannel, directActionListener)
        registerReceiver(broadcastReceiver, DirectBroadcastReceiver.getIntentFilter())
    }

    private fun initEvent() {
        lifecycleScope.launch {
            fileSenderViewModel.viewState.collect {
                when (it) {
                    ViewState.Idle -> {
                        clearLog()
                        dismissLoadingDialog()
                    }

                    ViewState.Connecting -> {
                        showLoadingDialog(message = "")
                    }

                    is ViewState.Receiving -> {
                        showLoadingDialog(message = "")
                    }

                    is ViewState.Success -> {
                        dismissLoadingDialog()
                    }

                    is ViewState.Failed -> {
                        dismissLoadingDialog()
                    }
                }
            }
        }
        lifecycleScope.launch {
            fileSenderViewModel.log.collect {
                log(it)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver)
        }
    }

    @SuppressLint("MissingPermission")
    private fun connect(wifiP2pDevice: WifiP2pDevice) {
        val wifiP2pConfig = WifiP2pConfig()
        wifiP2pConfig.deviceAddress = wifiP2pDevice.deviceAddress
        wifiP2pConfig.wps.setup = WpsInfo.PBC
        showLoadingDialog(message = "Connecting，deviceName: " + wifiP2pDevice.deviceName)
        showToast("Connecting，deviceName: " + wifiP2pDevice.deviceName)
        wifiP2pManager.connect(wifiP2pChannel, wifiP2pConfig,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    log("connect onSuccess")
                }

                override fun onFailure(reason: Int) {
                    showToast("Connection Failed $reason")
                    dismissLoadingDialog()
                }
            })
    }

    private fun disconnect() {
        wifiP2pManager.cancelConnect(wifiP2pChannel, object : WifiP2pManager.ActionListener {
            override fun onFailure(reasonCode: Int) {
                log("cancelConnect onFailure:$reasonCode")
            }

            override fun onSuccess() {
                log("cancelConnect onSuccess")
                tvConnectionStatus.text = null
                btnDisconnect.isEnabled = false
                btnChooseFile.isEnabled = false
            }
        })
        wifiP2pManager.removeGroup(wifiP2pChannel, null)
    }

    private fun log(log: String) {
        tvLog.append(log)
        tvLog.append("\n\n")
    }

    private fun clearLog() {
        tvLog.text = ""
    }

}