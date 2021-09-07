package com.specknet.pdiotapp.utils

import android.content.IntentFilter

/**
 * A re-usable object containing common Intent Filters
 */
object SpeckIntentFilters {

    // register IMU mode change broadcast filter
    val RESpeckIMUIntentFilter: IntentFilter = IntentFilter().apply {
//        addAction(Constants.RESPECK_USE_ACC_CHARACTERISTIC)
        addAction(Constants.RESPECK_USE_IMU_CHARACTERISTIC)
    }

    // register Speck Bluetooth Service scan request
    val bluetoothServiceScanForDevicesIntentFilter: IntentFilter = IntentFilter().apply {
        addAction(Constants.ACTION_SPECK_BLUETOOTH_SERVICE_SCAN_DEVICES)
    }

}