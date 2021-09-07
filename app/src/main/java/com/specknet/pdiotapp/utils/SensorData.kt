package com.specknet.pdiotapp.utils

import java.io.Serializable

open class TriaxialData(open val x: Float, open val y: Float, open val z: Float) :
    Serializable

data class AccelerometerReading(
    override val x: Float = 0f,
    override val y: Float = 0f,
    override val z: Float = 0f
) : TriaxialData(x, y, z)

data class GyroscopeReading(
    override val x: Float = 0f,
    override val y: Float = 0f,
    override val z: Float = 0f
) :
    TriaxialData(x, y, z)

data class MagnetometerReading(
    override val x: Float = 0f,
    override val y: Float = 0f,
    override val z: Float = 0f
) : TriaxialData(x, y, z)


data class IMUReading(
    val acc: AccelerometerReading?,
    val mag: MagnetometerReading?,
    val gyro: GyroscopeReading?
)

data class RESpeckTimestampedPacket(
    val phoneTimestamp: Long,
    val rawPacket: RESpeckRawPacket
)

data class RESpeckRawPacket(
//    val phoneTimestamp: Long, // not repeatable for testing, add later in the chain
    val seqNumber: Int,
    val respeckTimestamp: Long,
    val batchData: List<RESpeckSensorData>,
    val frequency: Float = 0f,
    val battLevel: Int = -1,
    val chargingStatus: Boolean = false
) : Serializable

data class ThingyRawPacket(
    val phoneTimestamp: Long,
    val accelData: AccelerometerReading,
    val gyroData: GyroscopeReading,
    val magData: MagnetometerReading
) : Serializable

/**
 * Data class to hold only the sensor data originating from the RESpeck.
 * All other data is dependent on this raw data, and can be added later in the process.
 */
data class RESpeckSensorData(
//    val phoneTimestamp: Long,
//    val respeckTimestamp: Long,
    val sequenceNumberInBatch: Int,
    val acc: AccelerometerReading = AccelerometerReading(),

    /** IMU data (gyro / magnetometer) may be disabled */
    val gyro: GyroscopeReading = GyroscopeReading(),
//    val gyroY: Float = 0f,
//    val gyroZ: Float = 0f,
//    val gyroX: Float = 0f,
    val mag: MagnetometerReading = MagnetometerReading(),
//    val magX: Float = 0f,
//    val magY: Float = 0f,
//    val magZ: Float = 0f

    // need to identify IMU packets, alternatively also whether this one should be ignored
    /** highFrequency indicates that this data is from between two normal data points.
     * it should be ignored in non-IMU mode */
    val highFrequency: Boolean = false
) : Serializable {
    // Seq number only between 0..65535 ?
    fun toRESpeckLiveData(): RESpeckLiveData {
        // TODO: this is hacky, there should be a RESpeck packet superclass or interface
        return RESpeckLiveData(
            phoneTimestamp = 0,
            respeckTimestamp = 0,
            sequenceNumberInBatch = 0,
            accelX = acc.x,
            accelY = acc.y,
            accelZ = acc.z,
            gyro = gyro
        )
    }
}

/**
 * Data class to hold only the sensor data originating from the RESpeck.
 * All other data is dependent on this raw data, and can be added later in the process.
 */
data class ThingySensorData(
    val phoneTimestamp: Long,
    val acc: AccelerometerReading = AccelerometerReading(),

    /** IMU data (gyro / magnetometer) may be disabled */
    val gyro: GyroscopeReading = GyroscopeReading(),
//    val gyroY: Float = 0f,
//    val gyroZ: Float = 0f,
//    val gyroX: Float = 0f,
    val mag: MagnetometerReading = MagnetometerReading(),
//    val magX: Float = 0f,
//    val magY: Float = 0f,
//    val magZ: Float = 0f

    // need to identify IMU packets, alternatively also whether this one should be ignored
    /** highFrequency indicates that this data is from between two normal data points.
     * it should be ignored in non-IMU mode */
    val highFrequency: Boolean = false
) : Serializable {
    // Seq number only between 0..65535 ?
    fun toThingyLiveData(): ThingyLiveData {
        // TODO: this is hacky, there should be a RESpeck packet superclass or interface
        return ThingyLiveData(
            phoneTimestamp = 0,
            accelX = acc.x,
            accelY = acc.y,
            accelZ = acc.z,
            gyro = gyro
        )
    }
}


data class RESpeckSensorDataCsv(
    val interpolatedPhoneTimestamp: Long,
    val respeckTimestamp: Long,
    val sequenceNumber: Int,
    val x: Float,
    val y: Float,
    val z: Float
) : CsvSerializable {
    constructor(r: RESpeckLiveData) : this(
        r.phoneTimestamp,
        r.respeckTimestamp,
        r.sequenceNumberInBatch,
        r.accelX, r.accelY, r.accelZ
    )

    /** TODO: see [CsvSerializable] */
    override fun toCsvString(): String = listOf(
        interpolatedPhoneTimestamp,
        respeckTimestamp,
        sequenceNumber,
        x,
        y,
        z
    ).joinToString(Constants.CSV_DELIMITER)

    companion object {
        val csvHeader: String = listOf(
            "interpolatedPhoneTimestamp",
            "respeckTimestamp",
            "sequenceNumber",
            "x",
            "y",
            "z"
        ).joinToString(Constants.CSV_DELIMITER)
    }
}

data class RESpeckIMUSensorDataCsv(
    val interpolatedPhoneTimestamp: Long,
    val accel_x: Float, val accel_y: Float, val accel_z: Float,
    val gyro_x: Float, val gyro_y: Float, val gyro_z: Float
) : CsvSerializable {
    /** easy interface from [RESpeckLiveData] */
    constructor(r: RESpeckLiveData) : this(
        r.phoneTimestamp,
        r.accelX, r.accelY, r.accelZ,
        r.gyro.x, r.gyro.y, r.gyro.z
    )

    /** TODO: see [CsvSerializable] */
    override fun toCsvString(): String = listOf(
        interpolatedPhoneTimestamp,
        accel_x,
        accel_y,
        accel_z,
        gyro_x,
        gyro_y,
        gyro_z
    ).joinToString(Constants.CSV_DELIMITER)

    companion object {
        val csvHeader: String = listOf(
            "interpolatedPhoneTimestamp",
            "accel_x",
            "accel_y",
            "accel_z",
            "gyro_x",
            "gyro_y",
            "gyro_z"
        ).joinToString(Constants.CSV_DELIMITER)
    }
}

