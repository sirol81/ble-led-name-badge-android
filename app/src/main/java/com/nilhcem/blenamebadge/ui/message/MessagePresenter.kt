package com.nilhcem.blenamebadge.ui.message

import android.app.PendingIntent.getActivity
import android.content.Context
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.nilhcem.blenamebadge.R
import com.nilhcem.blenamebadge.core.android.log.Timber
import com.nilhcem.blenamebadge.core.utils.ByteArrayUtils
import com.nilhcem.blenamebadge.device.DataToByteArrayConverter
import com.nilhcem.blenamebadge.device.bluetooth.GattClient
import com.nilhcem.blenamebadge.device.model.DataToSend

class MessagePresenter {
    private var gattClient = GattClient()

    fun sendSingleMessage(context: Context, dataToSend: DataToSend, text: String, sleep_time: Long, button: Button, console : TextView) {
        Timber.i { "About to send data: $dataToSend" }
        val byteData = DataToByteArrayConverter.convert(dataToSend)
        sendBT(context, byteData, text, sleep_time, button.getTag().toString(), button, console)
    }

    fun onPause() {
        gattClient.stopClient()
    }

    private fun sendBT(context: Context, byteData: List<ByteArray>, text: String, sleep: Long, deviceaddress : String, button : Button, console : TextView) {
        val activity : MessageActivity = context as MessageActivity
        gattClient = GattClient()
        gattClient.startClient(context, deviceaddress, sleep) { onConnected ->
            if (onConnected) {
                console.setText("onConnected TRUE #" + button.getText())
                gattClient.writeDataStart(byteData, context, console) {
                    gattClient.stopClient()
                    console.setText("stopClient")
                    activity.runOnUiThread{
                        button.isEnabled = false
                        Toast.makeText(context, "Sent to BT #" + button.getText() + " : OK", Toast.LENGTH_SHORT).show()
                        console.setText("Sent to BT #" + button.getText() + " : OK")
                    }
                }
            }
            else
            {//some kind of error
                console.setText("onConnected FALSE #" + button.getText())
                sendBT(context, byteData, text, sleep, deviceaddress, button, console)
            }
        }
    }
}
