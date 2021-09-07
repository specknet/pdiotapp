package com.specknet.pdiotapp.utils

import java.io.Serializable

/**
 * Class for storing all RESpeck data
 * TODO: use [AccelerometerReading] data class for accelerometer data?
 *  other data classes could also substitute breathing / activity / battery data
 */
data class RESpeckLiveData(
    val phoneTimestamp: Long,
    val respeckTimestamp: Long,
    val sequenceNumberInBatch: Int,
    val accelX: Float,
    val accelY: Float,
    val accelZ: Float,
    val frequency: Float = 0.0f,
    val battLevel: Int = -1,
    val chargingStatus: Boolean = false,
    val gyro: GyroscopeReading = GyroscopeReading(),
    val mag: MagnetometerReading = MagnetometerReading(),
    /** see [RESpeckSensorData.highFrequency] */
    val highFrequency: Boolean = false
) : Serializable, CsvSerializable {

    // TODO these constructors can be replaced by the default parameters above?
    constructor(
        phoneTimestamp: Long, respeckTimestamp: Long,
        sequenceNumberInBatch: Int, accelX: Float, accelY: Float, accelZ: Float
    ) :
            this(
                phoneTimestamp,
                respeckTimestamp,
                sequenceNumberInBatch,
                accelX,
                accelY,
                accelZ,
                0.0f,
                -1,
                false
            )

    constructor(
        phoneTimestamp: Long, respeckTimestamp: Long,
        sequenceNumberInBatch: Int, accelX: Float, accelY: Float, accelZ: Float,
        frequency: Float
    ) :
            this(
                phoneTimestamp,
                respeckTimestamp,
                sequenceNumberInBatch,
                accelX,
                accelY,
                accelZ,
                frequency,
                -1,
                false
            )

    constructor(
        phoneTimestamp: Long, respeckTimestamp: Long,
        sequenceNumberInBatch: Int, accelX: Float, accelY: Float, accelZ: Float,
        frequency: Float, battLevel: Int
    ) :
            this(
                phoneTimestamp,
                respeckTimestamp,
                sequenceNumberInBatch,
                accelX,
                accelY,
                accelZ,
                frequency,
                battLevel,
                false
            )

    constructor(
        phoneTimestamp: Long, respeckTimestamp: Long,
        sequenceNumberInBatch: Int, accelX: Float, accelY: Float, accelZ: Float,
        battLevel: Int, chargingStatus: Boolean
    ) :
            this(
                phoneTimestamp,
                respeckTimestamp,
                sequenceNumberInBatch,
                accelX,
                accelY,
                accelZ,
                0f,
                battLevel,
                chargingStatus
            )

    constructor(
        phoneTimestamp: Long, respeckTimestamp: Long,
        sequenceNumberInBatch: Int, accelX: Float, accelY: Float, accelZ: Float,
        frequency: Float, battLevel: Int, chargingStatus: Boolean
    ) :
            this(
                phoneTimestamp,
                respeckTimestamp,
                sequenceNumberInBatch,
                accelX,
                accelY,
                accelZ,
                frequency,
                battLevel,
                chargingStatus,
                gyro = GyroscopeReading()
            )


    constructor(
        interpolatedPhoneTimestampOfCurrentSample: Long,
        interpolatedRespeckTimestampOfCurrentSample: Long,
        currentSequenceNumberInBatch: Int,
        x: Float,
        y: Float,
        z: Float,
        mSamplingFrequency: Float,
        battLevel: Int,
        chargingStatus: Boolean,
        gyro: GyroscopeReading
    ) : this(
        interpolatedPhoneTimestampOfCurrentSample,
        interpolatedRespeckTimestampOfCurrentSample,
        currentSequenceNumberInBatch,
        x,
        y,
        z,
        mSamplingFrequency,
        battLevel,
        chargingStatus,
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
            phoneTimestamp, respeckTimestamp,
            sequenceNumberInBatch,
            accelX, accelY, accelZ
        ).joinToString(", ")
    }

//    override fun toCsvString() = toStringForFile()

}

