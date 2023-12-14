package com.nilhcem.blenamebadge.device.bluetooth

import android.bluetooth.*
import android.content.Context
import android.content.Context.BLUETOOTH_SERVICE
import com.nilhcem.blenamebadge.core.android.log.Timber
import com.nilhcem.blenamebadge.core.utils.ByteArrayUtils
import com.nilhcem.blenamebadge.device.bluetooth.Constants.CHARACTERISTIC_UUID
import com.nilhcem.blenamebadge.device.bluetooth.Constants.SERVICE_UUID
import java.util.*

class GattClient {

    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var onConnectedListener: ((Boolean) -> Unit)? = null
    private var onFinishWritingDataListener: (() -> Unit)? = null
    private var wait: Long = 50L

    private val messagesToSend = LinkedList<ByteArray>()

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Timber.i { "Connected to GATT client. Attempting to start service discovery" }
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Timber.i { "Disconnected from GATT client" }
                stopClient()
                onConnectedListener?.invoke(false)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                onConnectedListener?.invoke(true)
            } else {
                Timber.w { "onServicesDiscovered received: " + status }
                stopClient()
                onConnectedListener?.invoke(false)
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            Timber.i { "onCharacteristicWrite" }
            Thread.sleep(wait)
            writeNextData()
        }
    }

    fun writeDataStart(byteData: List<ByteArray>, onFinishWritingDataListener: () -> Unit) {
        this.onFinishWritingDataListener = onFinishWritingDataListener
        messagesToSend.addAll(byteData)
        writeNextData()
    }

    fun writeNextData() {
        if (messagesToSend.isEmpty()) {
            onFinishWritingDataListener?.invoke()
        } else {
            val data = messagesToSend.pop()
            Timber.e { "Writing: ${ByteArrayUtils.byteArrayToHexString(data)}" }

            val characteristic = bluetoothGatt?.getService(SERVICE_UUID)?.getCharacteristic(CHARACTERISTIC_UUID)
            characteristic?.value = data
            bluetoothGatt?.writeCharacteristic(characteristic)
        }
    }

    fun startClient(context: Context, deviceAddress: String, wait: Long, onConnectedListener: (Boolean) -> Unit) {
        this.onConnectedListener = onConnectedListener
        bluetoothManager = context.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter
        this.wait = wait

        val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(deviceAddress)
        bluetoothGatt = bluetoothDevice?.connectGatt(context, false, gattCallback)

        if (bluetoothGatt == null) {
            Timber.w { "Unable to create GATT client" }
            return
        }
    }

    fun stopClient() {
        if (bluetoothGatt != null) {
            bluetoothGatt?.close()
            bluetoothGatt = null
        }

        if (bluetoothAdapter != null) {
            bluetoothAdapter = null
        }
    }
}
