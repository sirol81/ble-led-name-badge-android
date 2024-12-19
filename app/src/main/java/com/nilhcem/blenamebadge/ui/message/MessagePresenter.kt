package com.nilhcem.blenamebadge.ui.message

import android.content.Context
import android.widget.Button
import android.widget.RadioButton
import com.nilhcem.blenamebadge.device.DataToByteArrayConverter
import com.nilhcem.blenamebadge.device.bluetooth.GattClient
import com.nilhcem.blenamebadge.device.model.DataToSend

class MessagePresenter {
    private var gattClient = GattClient()

    fun sendSingleMessage(context: Context, dataToSend: DataToSend, sleep_time: Long, button: Button) {
        val byteData = DataToByteArrayConverter.convert(dataToSend)
        val rb = button.getTag() as RadioButton
        sendBT(context, byteData, sleep_time, rb.getTag().toString())
    }

    fun onPause() {
        gattClient.stopClient()
    }

    private fun sendBT(context: Context, byteData: List<ByteArray>, sleep: Long, deviceaddress : String) {
        gattClient = GattClient()
        gattClient.startClient(context, deviceaddress, sleep) { onConnected ->
            if (onConnected) {
                gattClient.writeDataStart(byteData) {
                    //gattClient.stopClient()
                }
            }
        }
    }
}
