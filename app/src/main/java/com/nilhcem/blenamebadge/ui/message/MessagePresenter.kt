package com.nilhcem.blenamebadge.ui.message

import android.app.PendingIntent.getActivity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import com.nilhcem.blenamebadge.R
import com.nilhcem.blenamebadge.core.android.log.Timber
import com.nilhcem.blenamebadge.core.utils.ByteArrayUtils
import com.nilhcem.blenamebadge.device.DataToByteArrayConverter
import com.nilhcem.blenamebadge.device.bluetooth.GattClient
import com.nilhcem.blenamebadge.device.bluetooth.ScanHelper
import com.nilhcem.blenamebadge.device.model.DataToSend

class MessagePresenter {
    private val scanHelper = ScanHelper()
    private var gattClient = GattClient()

    fun sendSingleMessage(context: Context, dataToSend: DataToSend, sleep_time: Long, button: Button, console : TextView) {
        Timber.i { "About to send data: $dataToSend" }
        val byteData = DataToByteArrayConverter.convert(dataToSend)
        val rb = button.getTag() as RadioButton
        sendBT(context, byteData, sleep_time, rb.getTag().toString(), button, console)
    }

    fun sendMessage(context: Context, dataToSend: DataToSend, sleep_time: Long, addresses: MutableList<String>, timeout: Long) {
        Timber.i { "About to send data: $dataToSend" }
        val byteData = DataToByteArrayConverter.convert(dataToSend)
        sendBytes(context, byteData, sleep_time, addresses, timeout)
    }

    private fun sendBytes(context: Context, byteData: List<ByteArray>, sleep: Long, addresses : MutableList<String>, /*console : TextView,*/ timeout: Long) {
        Timber.i { "ByteData: ${byteData.map { ByteArrayUtils.byteArrayToHexString(it) }}" }
        scanHelper.timeout = timeout
        scanHelper.startLeScan { device ->
            if (device == null) {
                Timber.e { "Scan could not find any device" }
            } else {
                if (addresses.contains(device.address))
                {
                    Timber.e { "Device found: $device" }
                    val index = addresses.indexOf(device.address) + 1;
                    Toast.makeText(context, "BT found : $index" , Toast.LENGTH_SHORT).show()

                    gattClient.startClient(context, device.address, sleep) { onConnected ->
                        if (onConnected) {
                            gattClient.writeDataStart(byteData) {
                                Timber.i { "Data sent to $device" }
                                gattClient.stopClient()
                            }
                        }
                    }
                }
            }
        }
    }

    fun foundDevice(timeout: Long) :String
    {
        var deviceAddress : String = ""
        scanHelper.timeout = timeout
        scanHelper.startLeScan { device ->
            if (device == null) {
                Timber.e { "Scan could not find any device" }
            } else {
                Timber.e { "Device found: $device" }
                deviceAddress = device.address
            }
        }
        return deviceAddress
    }
    fun sendToDevice(context: Context, byteData: List<ByteArray>, sleep: Long, address: String) {
        gattClient.startClient(context, address, sleep) { onConnected ->
            if (onConnected) {
                gattClient.writeDataStart(byteData) {
                    gattClient.stopClient()
                }
            }
        }
    }

    fun onPause() {
        gattClient.stopClient()
    }

    private fun sendBT(context: Context, byteData: List<ByteArray>, sleep: Long, deviceaddress : String, button : Button, console : TextView) {
        val activity : MessageActivity = context as MessageActivity
        gattClient = GattClient()
        gattClient.startClient(context, deviceaddress, sleep) { onConnected ->
            if (onConnected) {
                console.setText("onConnected TRUE #" + button.getText())
                gattClient.writeDataStart(byteData) {
                    gattClient.stopClient()
                    console.setText("stopClient")
                    activity.runOnUiThread{
                        button.isEnabled = false
                        Toast.makeText(context, "Sent to BT #" + button.getText() + " : OK", Toast.LENGTH_SHORT).show()
                        console.setText("Sent to BT #" + button.getText() + " : OK")
                        Handler(Looper.getMainLooper()).postDelayed({
                            button.isEnabled = true
                        }, 9000)
                    }
                }
            }
            else
            {//some kind of error
                console.setText("onConnected FALSE #" + button.getText())
            }
        }
    }
}
