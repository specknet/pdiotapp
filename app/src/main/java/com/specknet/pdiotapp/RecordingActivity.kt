package com.specknet.pdiotapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.CountUpTimer
import com.specknet.pdiotapp.utils.RESpeckLiveData
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
    lateinit var looper: Looper

    val filterTest = IntentFilter(Constants.ACTION_RESPECK_LIVE_BROADCAST)

    var sensorType = ""
    var universalSubjectId = "s123456"
    var activityType = ""
    var activityCode = 0
    var notes = ""

    private var mIsRecording = false
    private lateinit var respeckOutputData: StringBuilder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recording)

        respeckOutputData = StringBuilder()

        setupSpinners()

        setupButtons()

        setupInputs()

        // register receiver
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
        val handlerThread = HandlerThread("bgProcThread")
        handlerThread.start()
        looper = handlerThread.looper
        val handler = Handler(looper)
        this.registerReceiver(respeckReceiver, filterTest, null, handler)

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
        if (mIsRecording) {
            val output = liveData.phoneTimestamp.toString() + "," +
                    liveData.accelX + "," + liveData.accelY + "," + liveData.accelZ + "," +
                    liveData.gyro.x + "," + liveData.gyro.y + "," + liveData.gyro.z + "\n"

            respeckOutputData.append(output)
            Log.d(TAG, "updateRespeckData: appended to respeckoutputdata = " + output)
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

        mIsRecording = false
    }

    private fun startRecording() {
        timer.visibility = View.VISIBLE

        countUpTimer.start()

        mIsRecording = true
    }

    private fun stopRecording() {

        countUpTimer.stop()
        countUpTimer.reset()
        timer.text = "Time elapsed: 00:00"

        Log.d(TAG, "stopRecording")

        saveRespeckRecording()

        mIsRecording = false

    }

    private fun saveRespeckRecording() {
        val filename = "${sensorType}_${universalSubjectId}_${activityType}_${System.currentTimeMillis()}.csv" // TODO format this to human readable

        val file = File(getExternalFilesDir(null), filename)

        Log.d(TAG, "saveRespeckRecording: filename = " + filename)

        val respeckWriter: BufferedWriter

        // Create file for current day and append header, if it doesn't exist yet
        try {
            val exists = file.exists()
            respeckWriter = BufferedWriter(OutputStreamWriter(FileOutputStream(file, true)))

            if (!exists) {
                Log.d(TAG, "saveRespeckRecording: filename doesn't exist")

                // the header columns in here
                respeckWriter.append("# Sensor type: $sensorType").append("\n")
                respeckWriter.append("# Activity type: $activityType").append("\n")
                respeckWriter.append("# Activity code: $activityCode").append("\n")
                respeckWriter.append("# Subject id: $universalSubjectId").append("\n")
                respeckWriter.append("# Notes: $notes").append("\n")

                respeckWriter.write(Constants.RECORDING_CSV_HEADER)
                respeckWriter.newLine()
                respeckWriter.flush()
            }
            else {
                Log.d(TAG, "saveRespeckRecording: filename exists")
            }

            if (respeckOutputData.isNotEmpty()) {
                respeckWriter.write(respeckOutputData.toString())
                respeckWriter.flush()

                Log.d(TAG, "saveRespeckRecording: recording saved")
            }
            else {
                Log.d(TAG, "saveRespeckRecording: no data from recording period")
            }

            respeckWriter.close()

            respeckOutputData = StringBuilder()
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
        looper.quit()
    }

}