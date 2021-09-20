package com.specknet.pdiotapp.utils

import android.content.Intent
import android.util.Log
import com.specknet.pdiotapp.R
import com.specknet.pdiotapp.bluetooth.BluetoothSpeckService
import org.apache.commons.lang3.time.DateUtils
import java.io.IOException
import java.util.*
import kotlin.math.abs

/**
 * This class processes new RESpeck packets which are passed from the SpeckBluetoothService.
 * It contains all logic to transform the incoming bytes into the desired variables and then stores and broadcasts
 * this information
 */
class RESpeckPacketHandler(val speckService: BluetoothSpeckService) {

    var fwVersion: String = speckService.reSpeckFwVersion
        set(value) {
            field = value
//            if (value != "-1") {
//                respeckCsvWriter = getNormalCsvFileWriter()
//                respeckIMUCsvWriter = getIMUCsvFileWriter()
//            }
        }

    fun processRESpeckLivePacket(values: ByteArray) {
        val fwVersionStr: String = speckService.reSpeckFwVersion
        Log.i("RAT", "Processing packet from RESpeck v: $fwVersionStr")

        val f = when {
            // First look at the length to determine which packet format is in use
            (values.size < 192) -> fun(_: ByteArray) {
                Log.e(
                    "RAT",
                    "UNKNOWN RESPECK VERSION: $fwVersionStr"
                )
            }
            else -> {
                when (fwVersionStr.first()) {
                    '6' -> { v: ByteArray -> processRESpeckV6Packet(v) }
                    else -> fun(_: ByteArray) {
                        Log.e(
                            "RAT",
                            "UNKNOWN RESPECK VERSION: $fwVersionStr"
                        )
                    }
                }
            }
        }
        // apply the correct function
        f(values)
    }

    fun processRESpeckV6Packet(values: ByteArray, useIMU: Boolean = false) {
        Log.d("BLT", "processRESpeckV6Packet: here")
        // Independent of the RESpeck timestamp, we use the phone timestamp
        val actualPhoneTimestamp = Utils.getUnixTimestamp()
        Log.d("V6Decode: Process", values.contentToString())


        val r = when (useIMU) {
            false -> RESpeckPacketDecoder.V6.decodePacket(
                values,
                0
            ) // FIXME: timestamping isn't quite right yet
            true -> RESpeckPacketDecoder.V6.decodeIMUPacket(values, takeIMU)
        }
        Log.d("V6Decode: Decoded", r.batchData.toString())

        // invert takeIMU to take or skip the next packet
        if (useIMU) {
            takeIMU = !takeIMU
        }

        // wrap detection - no side effects??
        // don't do this if in IMU mode: no sequence number
        if (last_seq_number >= 0 && r.seqNumber - last_seq_number != 1 || !useIMU) {
            // have we just wrapped?
            if (r.seqNumber == 0 && last_seq_number == 65535) {
                Log.w("RESpeckPacketHandler", "Respeck seq number wrapped")
            } else {
                Log.w(
                    "RESpeckPacketHandler",
                    "Unexpected respeck seq number. Expected: ${last_seq_number + 1}, received: ${r.seqNumber}"
                )
                restartRespeckSamplingFrequency()
            }
        }
        last_seq_number = r.seqNumber

        if (!useIMU) {
            // Log battery level and charging status (only available with normal packets)
            Log.i("RESpeckPacketHandler", "Respeck battery level: ${r.battLevel}")
            Log.i("RESpeckPacketHandler", "Respeck charging?: ${r.chargingStatus}")
        }

        // If this is our first sequence, or the last sequence was more than 2.5 times the average time
        // difference in the past, we use the typical time difference between the RESpeck packets for
        // determining the previous timestamp. This only affects the minute calculations. The breathing rate
        // is calculated based on only the sampling rate.
        // TODO this needs to be checked for consistency: IMU mode will produce different timestamps
        mPhoneTimestampLastPacketReceived =
            if (mPhoneTimestampCurrentPacketReceived == -1L || mPhoneTimestampCurrentPacketReceived + 2.5 * Constants.AVERAGE_TIME_DIFFERENCE_BETWEEN_RESPECK_PACKETS < actualPhoneTimestamp) {
                actualPhoneTimestamp - Constants.AVERAGE_TIME_DIFFERENCE_BETWEEN_RESPECK_PACKETS
            } else {
                // Store the previously used phone timestamp as previous timestamp
                mPhoneTimestampCurrentPacketReceived
            }

        // TODO this needs to be checked for consistency: IMU mode will produce different timestamps
        val extrapolatedPhoneTimestamp =
            mPhoneTimestampLastPacketReceived + Constants.AVERAGE_TIME_DIFFERENCE_BETWEEN_RESPECK_PACKETS

        // If the last timestamp plus the average time difference is more than
        // x seconds apart, we use the actual phone timestamp. Otherwise, we use the
        // last plus the average time difference.
        // TODO this needs to be checked for consistency: IMU mode will produce different timestamps
        mPhoneTimestampCurrentPacketReceived =
            when (abs(extrapolatedPhoneTimestamp - actualPhoneTimestamp) > Constants.MAXIMUM_MILLISECONDS_DEVIATION_ACTUAL_AND_CORRECTED_TIMESTAMP) {
                true -> actualPhoneTimestamp
                false -> extrapolatedPhoneTimestamp
            }

        // Similar calculations needed for the respeck timestamp for each sample
        mRESpeckTimestampLastPacketReceived = if (mRESpeckTimestampCurrentPacketReceived == -1L) {
            // If this is our first packet, make an approximation
            r.respeckTimestamp - Constants.AVERAGE_TIME_DIFFERENCE_BETWEEN_RESPECK_PACKETS
        } else {
            // Store the previously used RESpeck timestamp as the previous timestamp
            mRESpeckTimestampCurrentPacketReceived
        }

        // Update the current respeck timestamp

        // Update the current respeck timestamp
        mRESpeckTimestampCurrentPacketReceived = r.respeckTimestamp

        currentSequenceNumberInBatch = 0

        for ((_, acc, gyro, _, highFrequency) in r.batchData) {
            val x = acc.x
            val y = acc.y
            val z = acc.z

            // Calculate interpolated timestamp of current sample based on sequence number
            // There are 32 samples in each acceleration batch the RESpeck sends.
            val interpolatedPhoneTimestampOfCurrentSample =
                ((mPhoneTimestampCurrentPacketReceived - mPhoneTimestampLastPacketReceived) * (currentSequenceNumberInBatch * 1.0 / Constants.NUMBER_OF_SAMPLES_PER_BATCH)).toLong() + mPhoneTimestampLastPacketReceived

            // Calculate a similar interpolated timestamp of the current sample using the respeck timestamp
            val interpolatedRespeckTimestampOfCurrentSample =
                ((mRESpeckTimestampCurrentPacketReceived - mRESpeckTimestampLastPacketReceived) * (currentSequenceNumberInBatch * 1.0 / Constants.NUMBER_OF_SAMPLES_PER_BATCH)).toLong() + mRESpeckTimestampLastPacketReceived

            // Store the respeck timestamps (not phone!) for the frequency estimation
            frequencyTimestampsRespeck.add(interpolatedRespeckTimestampOfCurrentSample)

            // check for the full minute before creating the live data package
            // Also calculate the approximation of the true sampling frequency
            var currentProcessedMinute = DateUtils.truncate(
                Date(mPhoneTimestampCurrentPacketReceived),
                Calendar.MINUTE
            ).time


            if (currentProcessedMinute != lastProcessedMinute) {
                var currentRespeckFrequency: Float
                if (minuteFrequencies.size < Constants.MINUTES_FOR_MEDIAN_CALC) {
                    Log.i("Freq", "One minute passed, calculating frequency")
                    // calculate an approximation of the sampling frequency
                    // and add it to a list for running median
                    currentRespeckFrequency = calculateRespeckSamplingFrequency()
                    minuteRespeckFrequencies.add(currentRespeckFrequency)
                    Collections.sort(minuteRespeckFrequencies)
                    var medianRespeckFrequency: Float
                    medianRespeckFrequency = if (minuteRespeckFrequencies.size % 2 == 0) {
                        //Average 2 middle values
                        (minuteRespeckFrequencies[minuteRespeckFrequencies.size / 2] + minuteRespeckFrequencies[minuteRespeckFrequencies.size / 2 - 1]) / 2
                    } else {
                        //Take middle value
                        minuteRespeckFrequencies[minuteRespeckFrequencies.size / 2]
                    }
                    Log.i("Freq", "medianFrequency = $medianRespeckFrequency")
                    if (medianRespeckFrequency > 10 && medianRespeckFrequency < 15) {
                        mSamplingFrequency = medianRespeckFrequency
                    }
                    Log.i("Freq", "median respeck frequency = $medianRespeckFrequency")
                }
                //After this, just stick to final mSamplingFrequency calculated.
            }

            // TODO: add gyroscope data to RESpeck packet (convert to Kotlin for easy constructor)
            val newRESpeckLiveData = RESpeckLiveData(
                interpolatedPhoneTimestampOfCurrentSample,
                interpolatedRespeckTimestampOfCurrentSample,
                currentSequenceNumberInBatch,
                x, y, z,
                mSamplingFrequency,
                r.battLevel,
                r.chargingStatus,
                gyro,
                highFrequency = highFrequency
            )
            Log.i("Freq", "newRespeckLiveData = $newRESpeckLiveData")

            // Store the important data in the external storage if set in config
            if (mIsStoreDataLocally) {
                writeToCsv(newRESpeckLiveData, useIMU)
            }

            // Send live broadcast intent
            val liveDataIntent = Intent(Constants.ACTION_RESPECK_LIVE_BROADCAST)
            liveDataIntent.putExtra(Constants.RESPECK_LIVE_DATA, newRESpeckLiveData)
            speckService.sendBroadcast(liveDataIntent)

            // Every full minute, calculate the average breathing rate in that minute. This value will
            // only change after a call to "calculateAverageBreathing".
            currentProcessedMinute = DateUtils.truncate(
                Date(mPhoneTimestampCurrentPacketReceived),
                Calendar.MINUTE
            ).time
            if (currentProcessedMinute != lastProcessedMinute) {

                lastProcessedMinute = currentProcessedMinute
            }
            currentSequenceNumberInBatch += 1
        }

    }

    private fun restartRespeckSamplingFrequency() {
        // Should get here if a packet was dropped
        // we clear the collected timestamps
        frequencyTimestampsRespeck.clear()

        // TODO maybe stop the breathing algorithm
    }

    private fun writeToCsv(data: RESpeckLiveData, useIMU: Boolean) {
        val now = Date()
        val currentWriteDay = DateUtils.truncate(now, Calendar.DAY_OF_MONTH).time
        val previousWriteDay =
            DateUtils.truncate(dateOfLastRESpeckWrite, Calendar.DAY_OF_MONTH).time
//        dateOfLastRESpeckWrite = now

        // If the file doesn't exist, or we are in a new day, create a new file
        if (currentWriteDay != previousWriteDay || now.time - dateOfLastRESpeckWrite.time > Constants.NUMBER_OF_MILLIS_IN_A_DAY) {
            try {
                Log.i("RESpeckPacketHandler", "RESpeck data file re-created")
//                respeckCsvWriter = getNormalCsvFileWriter()
//                respeckIMUCsvWriter = getIMUCsvFileWriter()
            } catch (e: IOException) {
                Log.e(
                    "RESpeckPacketHandler",
                    "Error while creating respeck or merged file: " + e.message
                )
            }
        }
//        // conditionally write to both files
//        try {
//            val r = RESpeckSensorDataCsv(data)
//            when (useIMU) {
//                // normal data comes in, goes to the regular file
//                false -> respeckCsvWriter.write(r)
//                // high-frequency data: downsample to regular file, but also keep full data in a separate one
//                true -> {
//                    // write all IMU data to IMU log file
//                    Log.d(TAG, "writing to high frequency file")
//                    respeckIMUCsvWriter.write(RESpeckIMUSensorDataCsv(data))
//                    // and every other packet to the regular file
//                    if (!data.highFrequency) {
//                        Log.d(TAG, "writing to regular file")
//                        respeckCsvWriter.write(r)
//                    }
//                }
//            }
//            dateOfLastRESpeckWrite = now
//        } catch (e: IOException) {
//            Log.e(TAG, "Error while writing to respeck file: " + e.message)
//        }
    }

    fun getNormalCsvFileWriter() =
        getCsvFileWriter<RESpeckSensorDataCsv>(
            Constants.RESPECK_DATA_DIRECTORY_NAME,
            RESpeckSensorDataCsv.csvHeader
        )

    fun getIMUCsvFileWriter() =
        getCsvFileWriter<RESpeckIMUSensorDataCsv>(
            Constants.RESPECK_IMU_DATA_DIRECTORY_NAME,
            RESpeckIMUSensorDataCsv.csvHeader
        )

    private fun <T : CsvSerializable> getCsvFileWriter(
        dir: String,
        header: String
    ): SensorDataCsvWriter<T> {
        // Check whether we are in a new day
        // TODO if we are in a new day then this is where we should upload the last day
        val now = Date()

        // Note that in keeping with the original format, there is *no space* between the
        // RESpeck MAC address / UUID and the firmware version bracket!!
        val filename = "./" +
                dir +
                listOf(
                    speckService.getString(R.string.respeck_name),
                    patientID,
                    androidID,
                    "${RESPECK_UUID.replace(":", "")}($fwVersion)",
                    Constants.dateFormatter.format(now)
                ).joinToString(" ")

        return SensorDataCsvWriter(
            filename,
            header,
            speckService.applicationContext, // utils needs context to access encryption key
//            when (useIMU) {
//                false -> Constants.RESPECK_DATA_HEADER
//                true -> Constants.RESPECK_IMU_DATA_HEADER
//            }
            mIsEncryptData
        )
    }

    private fun calculateRespeckSamplingFrequency(): Float {
        val num_freq = frequencyTimestampsRespeck.size
        if (num_freq <= 1) {
            return 0f
        }
        val first_ts = frequencyTimestampsRespeck[0]
        val last_ts = frequencyTimestampsRespeck[num_freq - 1]
        val samplingFrequencyRespeck = num_freq * 1f / (last_ts - first_ts) * 1000f
        Log.i("Freq", "samplingFrequencyRespeck = $samplingFrequencyRespeck")
        frequencyTimestampsRespeck.clear()
        return samplingFrequencyRespeck
    }

    companion object {
        val TAG = this::class.java.simpleName

        // Battery monitoring (old, doesn't work)
        private const val PROMPT_TO_CHARGE_LEVEL = 1152
        private const val BATTERY_FULL_LEVEL = 1152 // was 1139
        private const val BATTERY_EMPTY_LEVEL = 889
        private var RESPECK_UUID: String = ""

        // Information about packets
        private var latestPacketSequenceNumber = -1
        private var mPhoneTimestampCurrentPacketReceived: Long = -1
        private var mPhoneTimestampLastPacketReceived: Long = -1
        private var mRESpeckTimestampCurrentPacketReceived: Long = -1
        private var mRESpeckTimestampLastPacketReceived: Long = -1

        // Initialise stored queue
        private val storedQueue: Queue<RESpeckStoredSample> = LinkedList<RESpeckStoredSample>()
        private var lastProcessedMinute = 0L
        private var currentSequenceNumberInBatch = -1
        private const val latestStoredRespeckSeq = -1
        private const val mLatestBatteryPercent = 0f
        private const val mLatestRequestCharge = false

        // Minute average breathing stats
        private var mAverageBreathingRate = 0f
        private var lastMinuteActivityLevel = ArrayList<Float>()
        private var lastMinuteActivityType = ArrayList<Int>()

        // frequency estimation
        private val frequencyTimestampsRespeck = ArrayList<Long>()
        private val frequencyTimestamps = ArrayList<Long>()
        private val minuteFrequencies = ArrayList<Float>()
        private val minuteRespeckFrequencies = ArrayList<Float>()
        private val rollingMedianFrequency = ArrayList<Float>()
        private var mSamplingFrequency = Constants.SAMPLING_FREQUENCY

        // Writers
        private var dateOfLastRESpeckWrite = Date(0)

        //        private val mSpeckService: SpeckBluetoothService? = null
        private var mIsStoreDataLocally = false
        private var patientID: String? = null
        private var androidID: String? = null
        private var mIsEncryptData = false
        private var last_seq_number = -1

        // A simple flag to alternate between which IMU packets to take
        private var takeIMU = true
//
//        // two writers, one for regular data, one for IMU
//        lateinit var respeckCsvWriter: SensorDataCsvWriter<RESpeckSensorDataCsv>
//        lateinit var respeckIMUCsvWriter: SensorDataCsvWriter<RESpeckIMUSensorDataCsv>

    }

    fun closeHandler() {
        Log.i(TAG, "Close handler (i.e. OutputstreamWriters)")
//        respeckCsvWriter.close()
    }
}