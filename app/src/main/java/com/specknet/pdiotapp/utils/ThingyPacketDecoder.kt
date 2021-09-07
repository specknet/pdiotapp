package com.specknet.pdiotapp.utils

import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A simple singleton to handle RESpeck packet decoding routines.
 */
object ThingyPacketDecoder {



    /**
     * Special decode function for the IMU characteristic.
     * The processing done is sourced from `respeckmodeltesting`:
     * https://github.com/specknet/respeckmodeltesting/blob/two_characteristics/app/src/main/java/com/specknet/respeckmodeltesting/utils/Utils.java#L163
     * No additional data in this packet:
     *  - do the two characteristics run together? toggle between them or on / off?
     *  -
     * Note: A `short` is 16 bits, 2 bytes.
     * 3 axes * 3 readings = 9 shorts => 9 * 2 bytes = 18 bytes
     * @param values the byte array to be decoded
     * @param highFrequency flag this packet as "high-frequency" to distinguish between 12.5hz and 25hz data
     */
    @JvmStatic // https://stackoverflow.com/q/56237695/9184658
    fun decodeThingyPacket(values: ByteArray, highFrequency: Boolean = false): RESpeckRawPacket {
        Log.d("decodeThingyPacket", "values = $values")

        val accelX = Utils.qToFloat(values[0], values[1])
        val accelY = Utils.qToFloat(values[2], values[3])
        val accelZ = Utils.qToFloat(values[4], values[5])

        val gyroX = Utils.qToFloat(values[6], values[7])
        val gyroY = Utils.qToFloat(values[8], values[9])
        val gyroZ = Utils.qToFloat(values[10], values[11])

        Log.d("decodeThingyPacket", "Thingy data: Accel($accelX, $accelY, $accelZ), Gyro($gyroX, $gyroY, $gyroZ)")


        val acc = AccelerometerReading(accelX, accelY, accelZ)
        val gyro = GyroscopeReading(gyroX, gyroY, gyroZ)
//        val mag = MagnetometerReading(magX, magY, magZ)

        val r = RESpeckSensorData(
            0,
            acc, gyro,
//            mag
            highFrequency = highFrequency
        )

        return RESpeckRawPacket(
            0,
            0,
            listOf(
                r
            )
        )
    }

}