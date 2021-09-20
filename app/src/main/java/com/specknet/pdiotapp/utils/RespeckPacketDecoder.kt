package com.specknet.pdiotapp.utils

import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A simple singleton to handle RESpeck packet decoding routines.
 */
object RESpeckPacketDecoder {

    object V6 {
        @JvmStatic // https://stackoverflow.com/q/56237695/9184658
        fun combineAccelerationBytes(upper: Byte, lower: Byte): Float {
            val unsignedLower = lower.toInt() and 0xff
            return (upper.toInt() shl 8 or unsignedLower) / 16384.0f
        }

        @JvmStatic // https://stackoverflow.com/q/56237695/9184658
        fun decodePacketData(values: ByteArray): AccelerometerReading {
            val (x, y, z) = values
                .take(6) // ensure there are six values per batch: [a, b, c, d, e, f]
                .chunked(2) // take in pairs: [(a, b), (c, d), (e, f)]
                .map { (a, b) ->
                    combineAccelerationBytes(a, b)
                }
            return AccelerometerReading(x, y, z)
        }

        /**
         * Decode logic extracted from processRESpeckV6Packet (RESpeckPacketHandler.java).
         * RESpeck packet byte format (accelerometer):
         * [8 bytes] -- header?
         * n * [6 bytes] -- acceleration data, n readings
         * @param values the 6-byte array representing accelerometer data
         */
        @JvmStatic // https://stackoverflow.com/q/56237695/9184658
        fun decodePacket(values: ByteArray, lastPacketTimestamp: Long = 0): RESpeckRawPacket {
            //get the respeck timestamp
            val buffer = ByteBuffer.wrap(values)
            buffer.order(ByteOrder.BIG_ENDIAN)
            buffer.position(0)

            Log.d("decodeV6Packet", "RESpeck Acc data: %s".format(buffer))

            val uncorrectedRESpeckTimestamp =
                buffer.int.toLong() and 0xffff_ffff // 8 * 2 bytes = 16 bytes
            val newRESpeckTimestamp = uncorrectedRESpeckTimestamp * 197 * 1000 / 32768

            // get the packet sequence number.
            // This counts from zero when the respeck is reset and is a uint32 value,
            // so we'll all be long dead by the time it wraps!
            val seqNumber = buffer.short.toInt() and 0xffff // 4 * 2 bytes = 8 bytes

            // Read battery level and charging status
            val battLevel = values[6].toInt()
            val chargingStatus = values[7] == 0x01.toByte()

            // these are batches of readings in a single packet
            // for (i in 8..values.size step 6) {
            // NUMBER_OF_SAMPLES_PER_BATCH = 32
            // sample delta is the time difference between packets divided by the number of samples
            // TODO change sample delta time
            val sampleDelta = 1 / Constants.NUMBER_OF_SAMPLES_PER_BATCH

            return RESpeckRawPacket(
//                actualPhoneTimestamp,
                seqNumber,
                newRESpeckTimestamp, // TODO: AVERAGE_TIME_DIFFERENCE_BETWEEN_RESPECK_PACKETS

                // take the accelerometer data (from byte 8) in groups of 6 bytes
                values.drop(8).chunked(6).withIndex().map { (seqNumberInBatch, vals) ->
                    val (x, y, z) = decodePacketData(vals.toByteArray())
                    RESpeckSensorData(
                        seqNumberInBatch,
                        AccelerometerReading(x, y, z)
                    )
                },
                0f, // TODO: update frequency calculation
                battLevel,
                chargingStatus
            )
        }

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
        fun decodeIMUPacket(values: ByteArray, highFrequency: Boolean = false): RESpeckRawPacket {
//        val packetData = ByteBuffer.allocate(9 * 2)
            val buffer = ByteBuffer.wrap(values)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            buffer.clear()
            buffer.put(values)
            buffer.position(0)
            Log.d("decodeV6PacketIMU", "RESpeck IMU data: $buffer")

            // IMU format:
            // - gyro(x, y, z)
            // - accelerometer(x, y, z)
            // integer part: 10 bits, fractional part 6 bits, so div by 2^6
            val gyroX: Float = buffer.short / 64f
            val gyroY: Float = buffer.short / 64f
            val gyroZ: Float = buffer.short / 64f

            // integer part: 2 bits, fractional part 14 bits, so div by 2^14
            val accelX: Float = buffer.short / 16384f
            val accelY: Float = buffer.short / 16384f
            val accelZ: Float = buffer.short / 16384f

            // TODO: no magnetometer data currently.
            // java.nio.BufferUnderflowException
            // integer part: 12 bits, fractional part 4 bits, so div by 2^4
//        val magX: Float = buffer.short / 16f
//        val magY: Float = buffer.short / 16f
//        val magZ: Float = buffer.short / 16f

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

    object V2 {

        @JvmStatic
        fun combineTimestampBytes(
            upper: Byte,
            upper_middle: Byte,
            lower_middle: Byte,
            lower: Byte
        ): Long {
            TODO("Test cases?")
            val unsigned_upper = upper.toInt() and 0xFF
            val unsigned_upper_middle = upper_middle.toInt() and 0xFF
            val unsigned_lower_middle = lower_middle.toInt() and 0xFF
            val unsigned_lower = lower.toInt() and 0xFF
            val value: Int =
                unsigned_upper shl 24 or (unsigned_upper_middle shl 16) or (unsigned_lower_middle shl 8) or unsigned_lower
            return value.toLong() and 0xffff_ffff
        }

    }
}