package com.nilhcem.blenamebadge.ui.message

import android.app.PendingIntent.getActivity
import android.content.Context
import android.widget.Button
import android.widget.Toast
import com.nilhcem.blenamebadge.R
import com.nilhcem.blenamebadge.core.android.log.Timber
import com.nilhcem.blenamebadge.core.utils.ByteArrayUtils
import com.nilhcem.blenamebadge.device.DataToByteArrayConverter
import com.nilhcem.blenamebadge.device.bluetooth.GattClient
import com.nilhcem.blenamebadge.device.bluetooth.ScanHelper
import com.nilhcem.blenamebadge.device.model.DataToSend

class MessagePresenter {

    companion object {
        private val SLEEP_TIME = 800L
    }
    private val scanHelper = ScanHelper()
    private val gattClient = GattClient()

    fun sendMessage(context: Context, dataToSend: DataToSend, vararg buttons: Button) {
        Timber.i { "About to send data: $dataToSend" }
        val byteData = DataToByteArrayConverter.convert(dataToSend)
        val addresses = mutableListOf("38:3B:26:EC:64:BF","38:3B:26:EC:64:89","38:3B:26:EC:64:3B","38:3B:26:EC:64:CD")
        sendBytes(context, byteData, addresses, SLEEP_TIME, buttons)
    }

    /*fun sendBitmap(context: Context, bmp: Bitmap) {
        val byteData = DataToByteArrayConverter.convertBitmap(bmp)
        sendBytes(context, byteData)
    }*/

    fun onPause() {
        scanHelper.stopLeScan()
        gattClient.stopClient()
    }

    private fun sendBytes(context: Context, byteData: List<ByteArray>, addresses : MutableList<String>, sleep : Long, buttons: Array<out Button>) {
        Timber.i { "ByteData: ${byteData.map { ByteArrayUtils.byteArrayToHexString(it) }}" }

        scanHelper.startLeScan { device ->
            if (device == null) {
                Timber.e { "Scan could not find any device" }
                Toast.makeText(context, "BT non trovati" , Toast.LENGTH_LONG).show()
            } else {
                Timber.e { "Device found: $device" }
                Toast.makeText(context, "BT found $device" , Toast.LENGTH_SHORT).show()
                scanHelper.stopLeScan()
                val activity : MessageActivity = context as MessageActivity
                gattClient.startClient(context, device.address) { onConnected ->
                    if (onConnected) {
                        gattClient.writeDataStart(byteData) {
                            Timber.i { "Data sent to $device" }
                            gattClient.stopClient()
                            var sendBF : Button = buttons[0]
                            var send89 : Button = buttons[0]
                            var send3B : Button = buttons[0]
                            var sendCD : Button = buttons[0]
                            for (b in buttons)
                            {
                                when (b.id) {
                                    R.id.send_button_BF -> sendBF = b
                                    R.id.send_button_89 -> send89 = b
                                    R.id.send_button_3B -> send3B = b
                                    R.id.send_button_CD -> sendCD = b
                                }
                            }
                            when (device.address) {
                                "38:3B:26:EC:64:BF" -> activity.runOnUiThread{ sendBF.isEnabled = false }
                                "38:3B:26:EC:64:89" -> activity.runOnUiThread{ send89.isEnabled = false }
                                "38:3B:26:EC:64:3B" -> activity.runOnUiThread{ send3B.isEnabled = false }
                                "38:3B:26:EC:64:CD" -> activity.runOnUiThread{ sendCD.isEnabled = false }
                            }
                            Thread.sleep(sleep)
                            if (addresses.isNotEmpty())
                            {
                                Timber.e { addresses.joinToString(",") }
                                sendBytes(context, byteData, addresses, sleep, buttons)
                            }
                        }
                    }
                }
            }
        }
    }
}
