package com.nilhcem.blenamebadge.ui.message

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
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

    fun sendMessage(context: Context, dataToSend: DataToSend) {
        Timber.i { "About to send data: $dataToSend" }
        val byteData = DataToByteArrayConverter.convert(dataToSend)
        val addresses = mutableListOf("38:3B:26:EC:64:BF","38:3B:26:EC:64:89","38:3B:26:EC:64:3B","38:3B:26:EC:64:CD")
        sendBytes(context, byteData, addresses, SLEEP_TIME)
    }

    fun sendBitmap(context: Context, bmp: Bitmap) {
        val byteData = DataToByteArrayConverter.convertBitmap(bmp)
        val addresses = mutableListOf("38:3B:26:EC:64:BF","38:3B:26:EC:64:89","38:3B:26:EC:64:3B","38:3B:26:EC:64:CD")
        sendBytes(context, byteData, addresses, SLEEP_TIME)
    }

    fun onPause() {
        scanHelper.stopLeScan()
        gattClient.stopClient()
    }

    private fun sendBytes(context: Context, byteData: List<ByteArray>, addresses : MutableList<String>, sleep : Long) {
        Timber.i { "ByteData: ${byteData.map { ByteArrayUtils.byteArrayToHexString(it) }}" }

        scanHelper.startLeScan { device ->
            if (device == null) {
                Timber.e { "Scan could not find any device" }
                Toast.makeText(context, "BT non trovati" , Toast.LENGTH_LONG).show()
            } else {
                Timber.e { "Device found: $device" }
                Toast.makeText(context, "BT found $device" , Toast.LENGTH_SHORT).show()

                gattClient.startClient(context, device.address) { onConnected ->
                    if (onConnected) {
                        gattClient.writeDataStart(byteData) {
                            Timber.i { "Data sent to $device" }
                            gattClient.stopClient()
                            addresses.remove(device.address)
                            Thread.sleep(sleep)
                            if (addresses.isNotEmpty())
                            {
                                Timber.e { addresses.joinToString(",") }
                                sendBytes(context, byteData, addresses, sleep)
                            }
                        }
                    }
                }
            }
        }
    }
}
