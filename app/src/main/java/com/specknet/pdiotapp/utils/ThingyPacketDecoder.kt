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
     */
    @JvmStatic // https://stackoverflow.com/q/56237695/9184658
    fun decodeThingyPacket(values: ByteArray): ThingyRawPacket {
        Log.d("decodeThingyPacket", "values = $values")

        val accelX = Utils.qToFloat(values[0], values[1])
        val accelY = Utils.qToFloat(values[2], values[3])
        val accelZ = Utils.qToFloat(values[4], values[5])

        val gyroX = Utils.qToFloat(values[6], values[7])
        val gyroY = Utils.qToFloat(values[8], values[9])
        val gyroZ = Utils.qToFloat(values[10], values[11])

        val magX = Utils.qToFloat(values[12], values[13])
        val magY = Utils.qToFloat(values[14], values[15])
        val magZ = Utils.qToFloat(values[16], values[17])

        Log.d("decodeThingyPacket", "Thingy data: Accel($accelX, $accelY, $accelZ), " +
                "Gyro($gyroX, $gyroY, $gyroZ), " +
                "Mag($magX, $magY, $magZ)")

        val acc = AccelerometerReading(accelX, accelY, accelZ)
        val gyro = GyroscopeReading(gyroX, gyroY, gyroZ)
        val mag = MagnetometerReading(magX, magY, magZ)

        return ThingyRawPacket(
            0,
            acc,
            gyro,
            mag
        )
    }

}