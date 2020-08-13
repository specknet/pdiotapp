package com.specknet.pdiotapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.CountUpTimer
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

class RecordingActivity : AppCompatActivity() {
    lateinit var sensorPositionSpinner: Spinner
    lateinit var sensorSideSpinner: Spinner
    lateinit var activityTypeSpinner: Spinner
    lateinit var socialSignalTypeSpinner: Spinner
    lateinit var sourceProjectSpinner: Spinner

    lateinit var startRecordingButton: Button
    lateinit var stopRecordingButton: Button

    lateinit var projectSubjectIdInput: EditText
    lateinit var univSubjectIdInput: EditText

    lateinit var timer: TextView
    lateinit var countUpTimer: CountUpTimer

    lateinit var writer: OutputStreamWriter
    var outputData: StringBuilder = StringBuilder()
    var fileClosed = true

    lateinit var respeckReceiver: BroadcastReceiver
    lateinit var looper: Looper

    var seq = 0

    val filterTest = IntentFilter(Constants.ACTION_INNER_RESPECK_BROADCAST)

    var projectSubjectId = "XXT"
    var sensorPosition = ""
    var sensorSide = ""
    var universalSubjectId = "XXTU"
    var activityType = ""
    var ssType = ""
    var sourceProject = ""
    var recordingId = "123"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recording)

        setupSpinners()

        setupButtons()

        setupInputs()

        // register receiver
        respeckReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                val action = intent.action

                if (action == Constants.ACTION_INNER_RESPECK_BROADCAST) {
                    // get all relevant intent contents
                    val x = intent.getFloatExtra(Constants.EXTRA_RESPECK_LIVE_X, 0f)
                    val y = intent.getFloatExtra(Constants.EXTRA_RESPECK_LIVE_Y, 0f)
                    val z = intent.getFloatExtra(Constants.EXTRA_RESPECK_LIVE_Z, 0f)

                    if(!fileClosed) {

                        appendToFile(x, y, z)
                    }

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

    private fun setupInputs() {
        projectSubjectIdInput = findViewById(R.id.project_subject_id_input)
        univSubjectIdInput = findViewById(R.id.universal_subject_id_input)
    }

    private fun setupSpinners() {
        sensorPositionSpinner = findViewById(R.id.sensor_position_spinner)

        ArrayAdapter.createFromResource(
            this,
            R.array.sensor_position_array,
            android.R.layout.simple_spinner_item
        ).also {
                adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            sensorPositionSpinner.adapter = adapter
        }

        sensorPositionSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, viwq: View, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position).toString()
                sensorPosition = selectedItem
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                sensorPosition = "Abdomen"
            }
        }

        sensorSideSpinner = findViewById(R.id.sensor_side_spinner)
        ArrayAdapter.createFromResource(
            this,
            R.array.sensor_side_array,
            android.R.layout.simple_spinner_item
        ).also {
                adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            sensorSideSpinner.adapter = adapter
        }

        sensorSideSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, viwq: View, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position).toString()
                sensorSide = selectedItem
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                sensorSide = "Left"
            }
        }

        activityTypeSpinner = findViewById(R.id.activity_type_spinner)
        ArrayAdapter.createFromResource(
            this,
            R.array.activity_type_array,
            android.R.layout.simple_spinner_item
        ).also {
                adapter ->
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

        socialSignalTypeSpinner = findViewById(R.id.ss_type_spinner)
        ArrayAdapter.createFromResource(
            this,
            R.array.social_signal_type_array,
            android.R.layout.simple_spinner_item
        ).also {
                adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            socialSignalTypeSpinner.adapter = adapter
        }

        socialSignalTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, viwq: View, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position).toString()
                ssType = Constants.SS_NAME_TO_CODE_MAPPING[selectedItem].toString()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                ssType = Constants.ACTIVITY_NAME_TO_CODE_MAPPING["Breathing"].toString()
            }
        }

        sourceProjectSpinner = findViewById(R.id.source_project_spinner)
        ArrayAdapter.createFromResource(
            this,
            R.array.source_project_array,
            android.R.layout.simple_spinner_item
        ).also {
                adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            sourceProjectSpinner.adapter = adapter
        }

        sourceProjectSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, viwq: View, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position).toString()
                sourceProject = selectedItem
                Log.i("selection", "i selected the iteem = " + sourceProject)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                sourceProject = "pdiot"
            }
        }

    }

    private fun setupButtons() {
        startRecordingButton = findViewById(R.id.start_recording_button)
        stopRecordingButton = findViewById(R.id.stop_recording_button)

        stopRecordingButton.isClickable = false
        stopRecordingButton.isEnabled = false

        startRecordingButton.setOnClickListener {
            Toast.makeText(this, "Starting recording", Toast.LENGTH_LONG).show()

            startRecordingButton.isClickable = false
            startRecordingButton.isEnabled = false

            stopRecordingButton.isClickable = true
            stopRecordingButton.isEnabled = true

            startRecording()
        }

        stopRecordingButton.setOnClickListener {
            Toast.makeText(this, "Stop recording", Toast.LENGTH_LONG).show()
            startRecordingButton.isClickable = true
            startRecordingButton.isEnabled = true

            stopRecordingButton.isClickable = false
            stopRecordingButton.isEnabled = false

            stopRecording()
        }

    }

    private fun startRecording() {
        timer.visibility = View.VISIBLE
        getInputs()
        createFile()
        fileClosed = false
        countUpTimer.start()
    }

    private fun stopRecording() {
        if (!fileClosed) {
            fileClosed = true
            if (outputData.isNotEmpty()) {
                writer.write(outputData.toString())
            }
            Log.i("recording", "stop rec")
            outputData = StringBuilder()
            writer.close()
            seq = 0
        }
        countUpTimer.stop()
        countUpTimer.reset()
        timer.text = "Time elapsed: 00:00"
    }

    private fun getInputs() {
        projectSubjectId = projectSubjectIdInput.text.toString()
        universalSubjectId = univSubjectIdInput.text.toString()
    }

    private fun createFile() {

        val activityTypeName = Constants.ACTIVITY_CODE_TO_NAME_MAPPING[activityType.toInt()]
        val ssTypeName = Constants.SS_CODE_TO_NAME_MAPPING[ssType.toInt()]
        val fileName = projectSubjectId + "_" + activityTypeName + "_" + ssTypeName + "_" + System.currentTimeMillis().toString() + ".csv"
        val file = File(getExternalFilesDir(null), fileName)

        if(file.exists()) {
            Log.i("recording", "file exists!")
            try {
                writer = OutputStreamWriter(FileOutputStream(file, true))
            } catch (e: IOException) {
                Log.e("recording", "error while writing to the file")
            }
        }
        else {
            try {
                writer = OutputStreamWriter(FileOutputStream(file, true))
                writer.append(Constants.RECORDING_CSV_HEADER).append("\n")
                writer.flush()
            } catch (e: IOException){
                Log.e("recording", "error while writing to the file")
            }
        }

    }

    private fun appendToFile(x: Float, y: Float, z: Float) {
        val outputString = System.currentTimeMillis().toString() + "," +
                seq + "," + x.toString() + "," + y.toString() + "," +
                z.toString() + "," + projectSubjectId + "," + sensorPosition + "," +
                sensorSide + "," + universalSubjectId + "," + activityType + "," +
                ssType + "," + sourceProject + "," + recordingId + "\n"
        outputData.append(outputString)
        seq++
        Log.i("recording", "new data")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(respeckReceiver)
        looper.quit()
    }

}