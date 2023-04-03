package com.nilhcem.blenamebadge.device.bluetooth

import android.bluetooth.BluetoothDevice
import android.os.Handler
import android.os.ParcelUuid
import com.nilhcem.blenamebadge.core.android.log.Timber
import com.nilhcem.blenamebadge.device.bluetooth.Constants.SERVICE_UUID
import no.nordicsemi.android.support.v18.scanner.*

class ScanHelper {
    private var isScanning = false
    private var onDeviceFoundCallback: ((BluetoothDevice?) -> Unit)? = null

    private val scanner by lazy { BluetoothLeScannerCompat.getScanner() }
    private val stopScanHandler = Handler()
    private val stopScanRunnable = Runnable {
        Timber.i { "No devices found" }
        stopLeScan()
        onDeviceFoundCallback?.invoke(null)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            Timber.i { "onScanResult: ${result.device.address}" }
            stopLeScan()
            onDeviceFoundCallback?.invoke(result.device)
        }

        override fun onScanFailed(errorCode: Int) {
            Timber.w { "Scan failed: $errorCode" }
            stopLeScan()
            onDeviceFoundCallback?.invoke(null)
        }
    }

    fun startLeScan(timeout : Long, onDeviceFoundCallback: ((BluetoothDevice?) -> Unit)) {
        this.onDeviceFoundCallback = onDeviceFoundCallback
        isScanning = true

        val filters = listOf(ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(SERVICE_UUID))
                .build())

        val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

        scanner.startScan(filters, settings, scanCallback)

        // Stops scanning after a pre-defined scan period.
        stopScanHandler.postDelayed(stopScanRunnable, timeout)
    }

    fun stopLeScan() {
        if (isScanning) {
            isScanning = false

            scanner.stopScan(scanCallback)
            stopScanHandler.removeCallbacks(stopScanRunnable)
        }
    }
}
