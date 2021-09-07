package com.specknet.pdiotapp.utils

import java.io.Serializable

/**
 * Class for storing all RESpeck data
 * TODO: use [AccelerometerReading] data class for accelerometer data?
 *  other data classes could also substitute breathing / activity / battery data
 */
data class ThingyLiveData(
    val phoneTimestamp: Long,
    val accelX: Float,
    val accelY: Float,
    val accelZ: Float,
    val gyro: GyroscopeReading = GyroscopeReading(),
    val mag: MagnetometerReading = MagnetometerReading()
) : Serializable, CsvSerializable {

    constructor(
        interpolatedPhoneTimestampOfCurrentSample: Long,
        x: Float,
        y: Float,
        z: Float,
        gyro: GyroscopeReading
    ) : this(
        interpolatedPhoneTimestampOfCurrentSample,
        x,
        y,
        z,
        gyro,
        mag = MagnetometerReading()
    ) {

    }


//    constructor(phoneTimestamp: Long, respeckTimestamp: Long,
//                sequenceNumberInBatch: Int, accelX: Float, accelY: Float, accelZ: Float,
//                breathingSignal: Float, breathingRate: Float, activityLevel: Float,
//                activityType: Int, avgBreathingRate: Float,
//                minuteStepCount: Int, frequency: Float):
//            this(phoneTimestamp, respeckTimestamp, sequenceNumberInBatch, accelX, accelY, accelZ,
//                    breathingSignal, breathingRate, activityLevel, activityType, avgBreathingRate, minuteStepCount, frequency)

    // Returns a one-line representation of data separated by comma used for storage
    fun toStringForFile(): String {
        return listOf(
            phoneTimestamp,
            accelX, accelY, accelZ
        ).joinToString(", ")
    }

//    override fun toCsvString() = toStringForFile()

}

