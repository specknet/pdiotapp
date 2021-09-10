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

        val decoded = Utils.decodeThingyPacket(values)
        Log.d("decodeThingyPacket", "Thingy accel data new: Accel(${decoded[0]},${decoded[1]},${decoded[2]})")

        val accelX = decoded[0]
        val accelY = decoded[1]
        val accelZ = decoded[2]

        Log.d("decodeThingyPacket", "Thingy accel data old: Accel($accelX, $accelY, $accelZ), ")

        val gyroX = decoded[3]
        val gyroY = decoded[4]
        val gyroZ = decoded[5]

        val magX = decoded[6]
        val magY = decoded[7]
        val magZ = decoded[8]

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