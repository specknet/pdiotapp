package com.specknet.pdiotapp.utils

import android.content.Intent
import android.util.Log
import com.specknet.pdiotapp.bluetooth.BluetoothSpeckService

/**
 * This class processes new RESpeck packets which are passed from the SpeckBluetoothService.
 * It contains all logic to transform the incoming bytes into the desired variables and then stores and broadcasts
 * this information
 */
class ThingyPacketHandler(val speckService: BluetoothSpeckService) {

    private val TAG = "ThingyPacketHandler"

    var fwVersion: String = speckService.reSpeckFwVersion
        set(value) {
            field = value
        }

    // TODO here is where we process a thingy packet
    fun processThingyPacket(values: ByteArray) {
        val actualPhoneTimestamp = Utils.getUnixTimestamp()

        val thingyData = ThingyPacketDecoder.decodeThingyPacket(values)

        Log.d(TAG, "processThingyPacket: decoded data " + thingyData.toString())
        
        // TODO only one sample per batch here
        val newThingyLiveData = ThingyLiveData(
            actualPhoneTimestamp,
            thingyData.accelData.x,
            thingyData.accelData.y,
            thingyData.accelData.z,
            thingyData.gyroData,
            thingyData.magData
        )
        Log.i("Freq", "newThingyLiveData = $newThingyLiveData")

        // Send live broadcast intent
        val liveDataIntent = Intent(Constants.ACTION_THINGY_BROADCAST)
        liveDataIntent.putExtra(Constants.THINGY_LIVE_DATA, newThingyLiveData)
        speckService.sendBroadcast(liveDataIntent)
    }
        
}