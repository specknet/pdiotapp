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
import com.specknet.pdiotapp.utils.Utils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class RecordingActivity : AppCompatActivity() {
    lateinit var sensorPositionSpinner: Spinner
    lateinit var sensorSideSpinner: Spinner
    lateinit var activityTypeSpinner: Spinner
    lateinit var startRecordingButton: Button
    lateinit var stopRecordingButton: Button
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

    var sensorPosition = ""
    var sensorSide = ""
    var universalSubjectId = "s123456"
    var activityType = ""
    var recordingId = "123"

    var last_x = -100f
    var last_y = -100f
    var last_z = -100f

    var last_x_rec = -100f
    var last_y_rec = -100f
    var last_z_rec = -100f

    lateinit var frequencies: ArrayList<Long>
    lateinit var frequenciesAppend: ArrayList<Long>

    var lastProcessedMinuteAppend = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recording)

        setupSpinners()

        setupButtons()

        setupInputs()

        frequencies = ArrayList<Long>()
        frequenciesAppend = ArrayList()
        var lastProcessedMinute = -1L

        // register receiver
        respeckReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                val action = intent.action

                if (action == Constants.ACTION_INNER_RESPECK_BROADCAST) {
                    // get all relevant intent contents
                    val x = intent.getFloatExtra(Constants.EXTRA_RESPECK_LIVE_X, 0f)
                    val y = intent.getFloatExtra(Constants.EXTRA_RESPECK_LIVE_Y, 0f)
                    val z = intent.getFloatExtra(Constants.EXTRA_RESPECK_LIVE_Z, 0f)
                    val ts = intent.getLongExtra(Constants.EXTRA_INTERPOLATED_TS, 0L)

                    if (x == last_x && y == last_y && z == last_z) {
                        Log.e("Debug", "DUPLICATE VALUES")
                    }

                    last_x = x
                    last_y = y
                    last_z = z

                    var now = System.currentTimeMillis()
                    frequencies.add(now)
                    var currentProcessedMinute = TimeUnit.MILLISECONDS.toMinutes(now)

                    if (lastProcessedMinute != currentProcessedMinute && lastProcessedMinute != -1L) {
                        var freq = calculateRecordingFrequency()
                        Log.i("Debug", "Recording freq = " + freq)
                    }

                    lastProcessedMinute = currentProcessedMinute

                    if(!fileClosed) {

                        appendToFile(x, y, z, ts)
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
        univSubjectIdInput = findViewById(R.id.universal_subject_id_input)
    }

    private fun setupSpinners() {
        sensorPositionSpinner = findViewById(R.id.sensor_position_spinner)

        ArrayAdapter.createFromResource(
            this,
            R.array.sensor_position_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            sensorPositionSpinner.adapter = adapter
        }

        sensorPositionSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, viwq: View, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position).toString()
                sensorPosition = selectedItem
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                sensorPosition = "Chest"
            }
        }

        sensorSideSpinner = findViewById(R.id.sensor_side_spinner)
        ArrayAdapter.createFromResource(
            this,
            R.array.sensor_side_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
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
        universalSubjectId = univSubjectIdInput.text.toString()
    }

    private fun createFile() {

        val activityTypeName = Constants.ACTIVITY_CODE_TO_NAME_MAPPING[activityType.toInt()]
        val fileName = "${universalSubjectId}_${activityTypeName}_${sensorPosition}_${sensorSide}_${System.currentTimeMillis()}.csv"
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
                // the header columns in here
                writer.append("# Sensor position: $sensorPosition").append("\n")
                writer.append("# Sensor side: $sensorSide").append("\n")
                writer.append("# Activity type: $activityTypeName").append("\n")
                writer.append("# Activity code: $activityType").append("\n")
                writer.append("# Subject id: $universalSubjectId").append("\n")
                writer.append(Constants.RECORDING_CSV_HEADER).append("\n")
                writer.flush()
                writer.flush()
            } catch (e: IOException){
                Log.e("recording", "error while writing to the file")
            }
        }

    }

    private fun appendToFile(x: Float, y: Float, z: Float, ts: Long) {
        val now = System.currentTimeMillis()

        frequenciesAppend.add(now)
        var currentProcessedMinute = TimeUnit.MILLISECONDS.toMinutes(now)

        if (lastProcessedMinuteAppend != currentProcessedMinute && lastProcessedMinuteAppend != -1L) {
            var freq = calculateRecordingFrequencyInAppend()
            Log.i("Debug", "Recording freq in append = " + freq)
        }

        lastProcessedMinuteAppend = currentProcessedMinute

        val outputString = ts.toString() + "," +
                seq + "," + x.toString() + "," + y.toString() + "," +
                z.toString() + "\n"
        outputData.append(outputString)
        seq++

        if (x == last_x_rec && y == last_y_rec && z == last_z_rec) {
            Log.e("Debug", "DUPLICATE VALUES in rec")
        }

        last_x_rec = x
        last_y_rec = y
        last_z_rec = z

        Log.i("Debug-rec", "appending " + outputString)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(respeckReceiver)
        looper.quit()
    }

    fun calculateRecordingFrequency(): Float {
        var num_freq = frequencies.size

        if (num_freq <= 1) {
            return 0f
        }

        var first_ts = frequencies[0]
        var last_ts = frequencies[num_freq - 1]
        var samplingFreq = num_freq * 1f / (last_ts - first_ts) * 1000f

        frequencies.clear()

        return samplingFreq
    }

    fun calculateRecordingFrequencyInAppend(): Float {
        var num_freq = frequenciesAppend.size

        if (num_freq <= 1) {
            return 0f
        }

        var first_ts = frequenciesAppend[0]
        var last_ts = frequenciesAppend[num_freq - 1]
        var samplingFreq = num_freq * 1f / (last_ts - first_ts) * 1000f

        frequenciesAppend.clear()

        return samplingFreq
    }

}