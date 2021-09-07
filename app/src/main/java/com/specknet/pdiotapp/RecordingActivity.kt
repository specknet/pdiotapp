package com.specknet.pdiotapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.CountUpTimer
import com.specknet.pdiotapp.utils.RESpeckLiveData
import com.specknet.pdiotapp.utils.ThingyLiveData
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.lang.StringBuilder

class RecordingActivity : AppCompatActivity() {
    private val TAG = "RecordingActivity"
    lateinit var sensorTypeSpinner: Spinner
    lateinit var activityTypeSpinner: Spinner
    lateinit var startRecordingButton: Button
    lateinit var cancelRecordingButton: Button
    lateinit var stopRecordingButton: Button
    lateinit var univSubjectIdInput: EditText
    lateinit var notesInput: EditText

    lateinit var timer: TextView
    lateinit var countUpTimer: CountUpTimer

    lateinit var respeckReceiver: BroadcastReceiver
    lateinit var thingyReceiver: BroadcastReceiver
    lateinit var respeckLooper: Looper
    lateinit var thingyLooper: Looper

    val respeckFilterTest = IntentFilter(Constants.ACTION_RESPECK_LIVE_BROADCAST)
    val thingyFilterTest = IntentFilter(Constants.ACTION_THINGY_BROADCAST)

    var sensorType = ""
    var universalSubjectId = "s1234567"
    var activityType = ""
    var activityCode = 0
    var notes = ""

    private var mIsRespeckRecording = false
    private var mIsThingyRecording = false
    private lateinit var respeckOutputData: StringBuilder
    private lateinit var thingyOutputData: StringBuilder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recording)

        respeckOutputData = StringBuilder()
        thingyOutputData = StringBuilder()

        setupSpinners()

        setupButtons()

        setupInputs()

        // register respeck receiver
        respeckReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                val action = intent.action

                if (action == Constants.ACTION_RESPECK_LIVE_BROADCAST) {

                    val liveData = intent.getSerializableExtra(Constants.RESPECK_LIVE_DATA) as RESpeckLiveData
                    Log.d("Live", "onReceive: liveData = " + liveData)

                    updateRespeckData(liveData)

                }

            }
        }

        // important to set this on a background thread otherwise it will block the UI
        val respeckHandlerThread = HandlerThread("bgProcThreadRespeck")
        respeckHandlerThread.start()
        respeckLooper = respeckHandlerThread.looper
        val respeckHandler = Handler(respeckLooper)
        this.registerReceiver(respeckReceiver, respeckFilterTest, null, respeckHandler)

        // register thingy receiver
        thingyReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                val action = intent.action

                if (action == Constants.ACTION_THINGY_BROADCAST) {

                    val liveData = intent.getSerializableExtra(Constants.THINGY_LIVE_DATA) as ThingyLiveData
                    Log.d("Live", "onReceive: thingyLiveData = " + liveData)

                    updateThingyData(liveData)

                }

            }
        }

        // important to set this on a background thread otherwise it will block the UI
        val thingyHandlerThread = HandlerThread("bgProcThreadThingy")
        thingyHandlerThread.start()
        thingyLooper = thingyHandlerThread.looper
        val thingyHandler = Handler(thingyLooper)
        this.registerReceiver(thingyReceiver, thingyFilterTest, null, thingyHandler)

        timer = findViewById(R.id.count_up_timer_text)
        timer.visibility = View.INVISIBLE

        countUpTimer = object: CountUpTimer(1000) {
            override fun onTick(elapsedTime: Long) {
                val date = Date(elapsedTime)
                val formatter = SimpleDateFormat("mm:ss")
                val dateFormatted = formatter.format(date)
                timer.text = "Time elapsed: " + dateFormatted
            }
        }

    }

    private fun updateRespeckData(liveData: RESpeckLiveData) {
        if (mIsRespeckRecording) {
            val output = liveData.phoneTimestamp.toString() + "," +
                    liveData.accelX + "," + liveData.accelY + "," + liveData.accelZ + "," +
                    liveData.gyro.x + "," + liveData.gyro.y + "," + liveData.gyro.z + "\n"

            respeckOutputData.append(output)
            Log.d(TAG, "updateRespeckData: appended to respeckoutputdata = " + output)
        }
    }

    private fun updateThingyData(liveData: ThingyLiveData) {
        if (mIsThingyRecording) {
            val output = liveData.phoneTimestamp.toString() + "," +
                    liveData.accelX + "," + liveData.accelY + "," + liveData.accelZ + "," +
                    liveData.gyro.x + "," + liveData.gyro.y + "," + liveData.gyro.z + "," +
                    liveData.mag.x + "," + liveData.mag.y + "," + liveData.mag.z + "\n"

            thingyOutputData.append(output)
            Log.d(TAG, "updateThingyData: appended to thingyOutputData = " + output)
        }
    }

    private fun setupInputs() {
        univSubjectIdInput = findViewById(R.id.universal_subject_id_input)
        notesInput = findViewById(R.id.notes_input)
    }

    private fun setupSpinners() {
        sensorTypeSpinner = findViewById(R.id.sensor_type_spinner)

        ArrayAdapter.createFromResource(
            this,
            R.array.sensor_type_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            sensorTypeSpinner.adapter = adapter
        }

        sensorTypeSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, viwq: View, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position).toString()
                sensorType = selectedItem
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                sensorType = "Respeck"
            }
        }

        activityTypeSpinner = findViewById(R.id.activity_type_spinner)
        ArrayAdapter.createFromResource(
            this,
            R.array.activity_type_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            activityTypeSpinner.adapter = adapter
        }

        activityTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, viwq: View, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position).toString()
                activityType = Constants.ACTIVITY_NAME_TO_CODE_MAPPING[selectedItem].toString()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                activityType = Constants.ACTIVITY_NAME_TO_CODE_MAPPING["Sitting"].toString()
            }
        }

    }

    private fun enableButton(button: Button) {
        button.isClickable = true
        button.isEnabled = true
    }

    private fun disableButton(button: Button) {
        button.isClickable = false
        button.isEnabled = false
    }

    private fun setupButtons() {
        startRecordingButton = findViewById(R.id.start_recording_button)
        cancelRecordingButton = findViewById(R.id.cancel_recording_button)
        stopRecordingButton = findViewById(R.id.stop_recording_button)

        disableButton(stopRecordingButton)
        disableButton(cancelRecordingButton)

        startRecordingButton.setOnClickListener {

            getInputs()

            if (universalSubjectId.length != 8) {
                Toast.makeText(this, "Input a correct student id", Toast.LENGTH_LONG).show()
                // TODO use material design here to highlight field
                return@setOnClickListener
            }

            Toast.makeText(this, "Starting recording", Toast.LENGTH_LONG).show()

            disableButton(startRecordingButton)

            enableButton(cancelRecordingButton)
            enableButton(stopRecordingButton)

            startRecording()
        }

        cancelRecordingButton.setOnClickListener {
            Toast.makeText(this, "Cancelling recording", Toast.LENGTH_LONG).show()

            enableButton(startRecordingButton)
            disableButton(cancelRecordingButton)
            disableButton(stopRecordingButton)

            cancelRecording()

        }

        stopRecordingButton.setOnClickListener {
            Toast.makeText(this, "Stop recording", Toast.LENGTH_LONG).show()

            enableButton(startRecordingButton)
            disableButton(cancelRecordingButton)
            disableButton(stopRecordingButton)

            stopRecording()
        }

    }

    private fun cancelRecording() {
        countUpTimer.stop()
        countUpTimer.reset()
        timer.text = "Time elapsed: 00:00"

        // reset output data
        respeckOutputData = StringBuilder()

        mIsRespeckRecording = false
    }

    private fun startRecording() {
        timer.visibility = View.VISIBLE

        countUpTimer.start()

        if (sensorType.equals("Thingy")) {
            mIsThingyRecording = true
            mIsRespeckRecording = false
        }
        else {
            mIsRespeckRecording = true
            mIsThingyRecording = false
        }
    }

    private fun stopRecording() {

        countUpTimer.stop()
        countUpTimer.reset()
        timer.text = "Time elapsed: 00:00"

        Log.d(TAG, "stopRecording")

        if (sensorType.equals("Thingy")) {
            mIsThingyRecording = false
        }
        else {
            mIsRespeckRecording = false
        }

        saveRecording()

    }

    private fun saveRecording() {
        val filename = "${sensorType}_${universalSubjectId}_${activityType}_${System.currentTimeMillis()}.csv" // TODO format this to human readable

        val file = File(getExternalFilesDir(null), filename)

        Log.d(TAG, "saveRecording: filename = " + filename)

        val dataWriter: BufferedWriter

        // Create file for current day and append header, if it doesn't exist yet
        try {
            val exists = file.exists()
            dataWriter = BufferedWriter(OutputStreamWriter(FileOutputStream(file, true)))

            if (!exists) {
                Log.d(TAG, "saveRecording: filename doesn't exist")

                // the header columns in here
                dataWriter.append("# Sensor type: $sensorType").append("\n")
                dataWriter.append("# Activity type: $activityType").append("\n")
                dataWriter.append("# Activity code: $activityCode").append("\n")
                dataWriter.append("# Subject id: $universalSubjectId").append("\n")
                dataWriter.append("# Notes: $notes").append("\n")

                if (sensorType.equals("Thingy")) {
                    dataWriter.write(Constants.RECORDING_CSV_HEADER_THINGY)
                }
                else {
                    dataWriter.write(Constants.RECORDING_CSV_HEADER_RESPECK)
                }
                dataWriter.newLine()
                dataWriter.flush()
            }
            else {
                Log.d(TAG, "saveRecording: filename exists")
            }

            if (sensorType.equals("Thingy")) {
                if (thingyOutputData.isNotEmpty()) {
                    dataWriter.write(thingyOutputData.toString())
                    dataWriter.flush()

                    Log.d(TAG, "saveRecording: thingy recording saved")
                }
                else {
                    Log.d(TAG, "saveRecording: no data from thingy during recording period")
                }
            }
            else {
                if (respeckOutputData.isNotEmpty()) {
                    dataWriter.write(respeckOutputData.toString())
                    dataWriter.flush()

                    Log.d(TAG, "saveRecording: respeck recording saved")
                }
                else {
                    Log.d(TAG, "saveRecording: no data from respeck during recording period")
                }
            }

            dataWriter.close()

            respeckOutputData = StringBuilder()
            thingyOutputData = StringBuilder()
        }
        catch (e: IOException) {
            Log.e(TAG, "saveRespeckRecording: Error while writing to the respeck file: " + e.message )
        }
    }

    private fun getInputs() {

        universalSubjectId = univSubjectIdInput.text.toString().toLowerCase().trim()
        activityType = activityTypeSpinner.selectedItem.toString()
        activityCode = Constants.ACTIVITY_NAME_TO_CODE_MAPPING[activityType]!!
        sensorType = sensorTypeSpinner.selectedItem.toString()
        notes = notesInput.text.toString().trim()

    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(respeckReceiver)
        unregisterReceiver(thingyReceiver)
        respeckLooper.quit()
        thingyLooper.quit()
    }

}