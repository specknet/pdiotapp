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
import java.io.*
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import java.lang.StringBuilder

class RecordingActivity : AppCompatActivity() {
    private val TAG = "RecordingActivity"
    lateinit var sensorTypeSpinner: Spinner
    lateinit var activityTypeSpinner: Spinner
    lateinit var activitySubtypeSpinner: Spinner
    lateinit var startRecordingButton: Button
    lateinit var cancelRecordingButton: Button
    lateinit var stopRecordingButton: Button
    lateinit var univSubjectIdInput: EditText
    lateinit var notesInput: EditText

    lateinit var timer: TextView
    lateinit var countUpTimer: CountUpTimer

    lateinit var respeckReceiver: BroadcastReceiver
    lateinit var respeckLooper: Looper

    val respeckFilterTest = IntentFilter(Constants.ACTION_RESPECK_LIVE_BROADCAST)

    var sensorType = ""
    var universalSubjectId = "s1234567"
    var activityType = ""
    var activitySubtype = "Normal"
    var notes = ""

    private var mIsRespeckRecording = false
    private lateinit var respeckOutputData: StringBuilder

    private lateinit var respeckAccel: TextView
    private lateinit var respeckGyro: TextView


    var respeckOn = false


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate: here")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recording)

        respeckOutputData = StringBuilder()

        setupViews()

        setupSpinners()

        setupButtons()

        setupInputs()

        Log.d(TAG, "onCreate: setting up respeck receiver")
        // register respeck receiver
        respeckReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                val action = intent.action

                if (action == Constants.ACTION_RESPECK_LIVE_BROADCAST) {

                    val liveData = intent.getSerializableExtra(Constants.RESPECK_LIVE_DATA) as RESpeckLiveData
                    Log.d("Live", "onReceive: liveData = " + liveData)

                    updateRespeckData(liveData)

                    respeckOn = true

                }

            }
        }

        // important to set this on a background thread otherwise it will block the UI
        val respeckHandlerThread = HandlerThread("bgProcThreadRespeck")
        respeckHandlerThread.start()
        respeckLooper = respeckHandlerThread.looper
        val respeckHandler = Handler(respeckLooper)
        this.registerReceiver(respeckReceiver, respeckFilterTest, null, respeckHandler)


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

    private fun setupViews() {
        respeckAccel = findViewById(R.id.respeck_accel)
        respeckGyro = findViewById(R.id.respeck_gyro)
    }

    private fun updateRespeckData(liveData: RESpeckLiveData) {
        if (mIsRespeckRecording) {
            val output = liveData.phoneTimestamp.toString() + "," +
                    liveData.accelX + "," + liveData.accelY + "," + liveData.accelZ + "," +
                    liveData.gyro.x + "," + liveData.gyro.y + "," + liveData.gyro.z + "\n"

            respeckOutputData.append(output)
            Log.d(TAG, "updateRespeckData: appended to respeckoutputdata = " + output)

        }

        // update UI thread
        runOnUiThread {
            respeckAccel.text = getString(R.string.respeck_accel, liveData.accelX, liveData.accelY, liveData.accelZ)
            respeckGyro.text = getString(R.string.respeck_gyro, liveData.gyro.x, liveData.gyro.y, liveData.gyro.z)
        }
    }

    private fun setupInputs() {
        Log.d(TAG, "setupInputs: here")
        univSubjectIdInput = findViewById(R.id.universal_subject_id_input)
        notesInput = findViewById(R.id.notes_input)
    }

    private fun setupSpinners() {
        Log.d(TAG, "setupSpinners: here")
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

        activitySubtypeSpinner = findViewById(R.id.activity_subtype_spinner)
        ArrayAdapter.createFromResource(
            this,
            R.array.activity_subtype_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            activitySubtypeSpinner.adapter = adapter
        }
    }

    private fun enableView(view: View) {
        view.isClickable = true
        view.isEnabled = true
    }

    private fun disableView(view: View) {
        view.isClickable = false
        view.isEnabled = false
    }

    private fun setupButtons() {
        startRecordingButton = findViewById(R.id.start_recording_button)
        cancelRecordingButton = findViewById(R.id.cancel_recording_button)
        stopRecordingButton = findViewById(R.id.stop_recording_button)

        disableView(stopRecordingButton)
        disableView(cancelRecordingButton)

        startRecordingButton.setOnClickListener {

            getInputs()

            if (universalSubjectId.length != 8) {
                Toast.makeText(this, "Input a correct student id", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (sensorType == "Respeck" && !respeckOn) {
                Toast.makeText(this, "Respeck is not on! Check connection.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Starting recording", Toast.LENGTH_SHORT).show()

            disableView(startRecordingButton)

            enableView(cancelRecordingButton)
            enableView(stopRecordingButton)

            disableView(sensorTypeSpinner)
            disableView(activityTypeSpinner)
            disableView(activitySubtypeSpinner)
            disableView(univSubjectIdInput)
            disableView(notesInput)

            startRecording()
        }

        cancelRecordingButton.setOnClickListener {
            Toast.makeText(this, "Cancelling recording", Toast.LENGTH_SHORT).show()

            enableView(startRecordingButton)
            disableView(cancelRecordingButton)
            disableView(stopRecordingButton)

            enableView(sensorTypeSpinner)
            enableView(activityTypeSpinner)
            enableView(activitySubtypeSpinner)
            enableView(univSubjectIdInput)
            enableView(notesInput)

            cancelRecording()

        }

        stopRecordingButton.setOnClickListener {
            Toast.makeText(this, "Stop recording", Toast.LENGTH_SHORT).show()

            enableView(startRecordingButton)
            disableView(cancelRecordingButton)
            disableView(stopRecordingButton)

            enableView(sensorTypeSpinner)
            enableView(activityTypeSpinner)
            enableView(activitySubtypeSpinner)
            enableView(univSubjectIdInput)
            enableView(notesInput)

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

        mIsRespeckRecording = !sensorType.equals("Thingy")
    }

    private fun stopRecording() {

        countUpTimer.stop()
        countUpTimer.reset()
        timer.text = "Time elapsed: 00:00"

        Log.d(TAG, "stopRecording")

        mIsRespeckRecording = false

        saveRecording()

    }

    private fun saveRecording() {
        val currentTime = System.currentTimeMillis()
        var formattedDate = ""
        try {
            formattedDate = SimpleDateFormat("dd-MM-yyyy_HH-mm-ss", Locale.UK).format(Date())
            Log.i(TAG, "saveRecording: formattedDate = " + formattedDate)
        } catch (e: Exception) {
            Log.i(TAG, "saveRecording: error = ${e.toString()}")
            formattedDate = currentTime.toString()
        }
        val filename = "${sensorType}_${universalSubjectId}_${activityType}_${activitySubtype}_${formattedDate}.csv" // TODO format this to human readable

        val file = File(getExternalFilesDir(null), filename)

        Log.d(TAG, "saveRecording: filename = " + file.toString())

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
                dataWriter.append("# Activity subtype: $activitySubtype").append("\n")
                dataWriter.append("# Subject id: $universalSubjectId").append("\n")
                dataWriter.append("# Notes: $notes").append("\n")

                if (sensorType == "Thingy") {
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


            if (sensorType.equals("Respeck")) {
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

            Toast.makeText(this, "Recording saved!", Toast.LENGTH_SHORT).show()
        }
        catch (e: IOException) {
            Toast.makeText(this, "Error while saving recording!", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "saveRespeckRecording: Error while writing to the respeck file: " + e.message )
        }
    }

    private fun getInputs() {

        universalSubjectId = univSubjectIdInput.text.toString().toLowerCase().trim()
        activityType = activityTypeSpinner.selectedItem.toString()
        activitySubtype = activitySubtypeSpinner.selectedItem.toString()
        sensorType = sensorTypeSpinner.selectedItem.toString()
        notes = notesInput.text.toString().trim()

    }

    override fun onDestroy() {
        unregisterReceiver(respeckReceiver)
        respeckLooper.quit()

        if (mIsRespeckRecording) {
            saveRecording()
        }

        super.onDestroy()

    }

}