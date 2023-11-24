package com.specknet.pdiotapp.detect

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.android.gms.internal.zzhu.runOnUiThread
import com.specknet.pdiotapp.R
import com.specknet.pdiotapp.sql.DBHelper
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.CountUpTimer
import com.specknet.pdiotapp.utils.RESpeckLiveData
import java.text.SimpleDateFormat
import java.util.Date


/**
 * A simple [Fragment] subclass.
 * Use the [DetectFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DetectFragment : Fragment() {

    companion object {
        private const val TAG = "DetectFragment"
    }

    val GENERAL_ACTIVITIES = listOf(
        "Sitting/Standing",
        "Lying down on left",
        "Lying down on right",
        "Lying down on back",
        "Lying Down on Stomach",
        "Walking normally",
        "Running",
        "Descending stairs",
        "Ascending stairs",
        "Shuffle walking",
        "Miscellaneous movements"
    )

    val STATIONARY_ACTIVITIES = listOf(
        "Sitting/standing and breathing normally",
        "Lying down left and breathing normally",
        "Lying down right and breathing normally",
        "Lying down on back and breathing normally",
        "Lying down on stomach and breathing normally",
        "Sitting/standing and coughing",
        "Lying down on left and coughing",
        "Lying down on right and coughing",
        "Lying down on back and coughing",
        "Lying down on stomach and coughing",
        "Sitting/standing and hyperventilating",
        "Lying down on left and hyperventilating",
        "Lying down on right and hyperventilating",
        "Lying down on back and hyperventilating",
        "Lyging down on stomach and hyperventilating"
    )

    val TASK3_ACTIVITIES = listOf(
        "sitting_standing breathingNormal",
        "lyingLeft breathingNormal",
        "lyingRight breathingNormal",
        "lyingBack breathingNormal",
        "lyingStomach breathingNormal",
        "sitting_standing coughing",
        "lyingLeft coughing",
        "lyingRight coughing",
        "lyingBack coughing",
        "lyingStomach coughing",
        "sitting_standing hyperventilating",
        "lyingLeft hyperventilating",
        "lyingRight hyperventilating",
        "lyingBack hyperventilating",
        "lyingStomach hyperventilating",
        "sitting_standing singing",
        "lyingLeft singing",
        "lyingRight singing",
        "lyingBack singing",
        "lyingStomach singing",
        "sitting_standing laughing",
        "lyingLeft laughing",
        "lyingRight laughing",
        "lyingBack laughing",
        "lyingStomach laughing",
        "sitting_standing talking",
        "lyingLeft talking",
        "lyingRight talking",
        "lyingBack talking",
        "lyingStomach talking",
        "sitting/standing eating",
    )


    // global graph variables
    val GENERAL_CLASSIFIER = Classifier(
        "ten_sec.tflite",
        50,
        3,
        10,
        11,
        GENERAL_ACTIVITIES
    )

    val STATIONARY_CLASSIFIER = Classifier(
        "conv_model_task2_50_3.tflite",
        50,
        3,
        10,
        15,
        STATIONARY_ACTIVITIES
    )

    val TASK3_CLASSIFIER = Classifier(
        "cnn_model_task3_100_20.tflite",
        100,
        3,
        20,
        TASK3_ACTIVITIES.size,
        TASK3_ACTIVITIES
    )

    var currentClassifier = GENERAL_CLASSIFIER


    lateinit var dataSet_res_accel_x: LineDataSet
    lateinit var dataSet_res_accel_y: LineDataSet
    lateinit var dataSet_res_accel_z: LineDataSet
    lateinit var classifierSpinner: Spinner

    var time = 0f
    lateinit var allRespeckData: LineData

    lateinit var respeckChart: LineChart
    lateinit var detectedActivity: TextView

    // global broadcast receiver so we can unregister it
    lateinit var respeckLiveUpdateReceiver: BroadcastReceiver
    lateinit var looperRespeck: Looper

    val filterTestRespeck = IntentFilter(Constants.ACTION_RESPECK_LIVE_BROADCAST)
    private var mIsRespeckRecording = false
    private var respeckOn = false

    lateinit var startRecordingButton: Button
    lateinit var cancelRecordingButton: Button
    lateinit var stopRecordingButton: Button

    lateinit var timer: TextView
    lateinit var countUpTimer: CountUpTimer

    private lateinit var dbHelper: DBHelper

    private lateinit var username: String

    private var activityRecordList = mutableListOf<String>()
    private var activityRecordTimeList = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_detect, container, false)
        setupButtons(view)
        setupClassifierSpinner(view)
        setupCharts(view)
        setupViews(view)

        countUpTimer = object : CountUpTimer(1000) {
            override fun onTick(elapsedTime: Long) {
                val date = Date(elapsedTime)
                val formatter = SimpleDateFormat("mm:ss")
                val dateFormatted = formatter.format(date)
                val timerText = "Time elapsed: $dateFormatted"
                runOnUiThread {
                    timer.text = timerText
                }
            }
        }
        dbHelper = DBHelper(requireContext())

        username = arguments?.getString("username")!!

        return view
    }

    private fun setupViews(view: View) {
        detectedActivity = view.findViewById(R.id.detected_activity_text)
        timer = view.findViewById(R.id.time_elapsed_text)
        timer.visibility = View.INVISIBLE
    }

    private fun enableView(view: View) {
        view.isClickable = true
        view.isEnabled = true
    }

    private fun disableView(view: View) {
        view.isClickable = false
        view.isEnabled = false
    }

    private fun setupButtons(view: View) {
        startRecordingButton = view.findViewById(R.id.start_rec_detect_button)
        cancelRecordingButton = view.findViewById(R.id.cancel_rec_detect_button)
        stopRecordingButton = view.findViewById(R.id.stop_rec_detect_button)

        disableView(cancelRecordingButton)
        disableView(stopRecordingButton)

        startRecordingButton.setOnClickListener {
            if (!respeckOn) {
                Toast.makeText(
                    requireContext(),
                    "Respeck not connected. Please connect to Respeck first.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            Toast.makeText(
                requireContext(),
                "Recording started. Please perform the activity.",
                Toast.LENGTH_SHORT
            ).show()

            disableView(startRecordingButton)
            enableView(cancelRecordingButton)
            enableView(stopRecordingButton)

            disableView(classifierSpinner)

            startRecording()
        }

        cancelRecordingButton.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Recording cancelled.",
                Toast.LENGTH_SHORT
            ).show()

            enableView(startRecordingButton)
            disableView(cancelRecordingButton)
            disableView(stopRecordingButton)

            enableView(classifierSpinner)

            cancelRecording()
        }

        stopRecordingButton.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Recording stopped.",
                Toast.LENGTH_SHORT
            ).show()

            enableView(startRecordingButton)
            disableView(cancelRecordingButton)
            disableView(stopRecordingButton)

            enableView(classifierSpinner)

            stopRecording()
        }
    }

    private fun startRecording() {
        timer.visibility = View.VISIBLE
        countUpTimer.start()
        mIsRespeckRecording = true
    }

    private fun cancelRecording() {
        countUpTimer.stop()
        countUpTimer.reset()
        timer.text = "Time elapsed: 00:00"
        activityRecordList.clear()
        activityRecordTimeList.clear()
        mIsRespeckRecording = false
    }

    private fun stopRecording() {
        countUpTimer.stop()
        countUpTimer.reset()
        timer.text = "Time elapsed: 00:00"

        Log.d(TAG, "stopRecording")
        mIsRespeckRecording = false

        for (i in activityRecordList.indices) {
            dbHelper.addActivity(username, activityRecordList[i], activityRecordTimeList[i])
        }
    }

    override fun onResume() {
        super.onResume()

        // set up the broadcast receiver
        respeckLiveUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                Log.i("thread", "I am running on thread = " + Thread.currentThread().name)

                val action = intent.action

                if (action == Constants.ACTION_RESPECK_LIVE_BROADCAST) {

                    val liveData =
                        intent.getSerializableExtra(Constants.RESPECK_LIVE_DATA) as RESpeckLiveData
                    Log.d("Live", "onReceive: liveData = $liveData")

                    // get all relevant intent contents
                    val accelX = liveData.accelX
                    val accelY = liveData.accelY
                    val accelZ = liveData.accelZ
                    val gyroX = liveData.gyro.x
                    val gyroY = liveData.gyro.y
                    val gyroZ = liveData.gyro.z

                    time += 1

                    currentClassifier.addData(accelX, accelY, accelZ, gyroX, gyroY, gyroZ)
                    if (currentClassifier.index == currentClassifier.windowSize) {
                        val activity = currentClassifier.classifyData(context)
                        runOnUiThread {
                            val activityText = "Detected activity: $activity"
                            detectedActivity.text = activityText
                        }
                        if (mIsRespeckRecording) {
                            activityRecordList.add(activity)
                            activityRecordTimeList.add(dbHelper.getCurrentTimestamp())
                        }
                    }

                    updateGraph("respeck", accelX, accelY, accelZ)
                    respeckOn = true
                }
            }
        }

        // register receiver on another thread
        val handlerThreadRespeck = HandlerThread("bgThreadRespeckLive")
        handlerThreadRespeck.start()
        looperRespeck = handlerThreadRespeck.looper
        val handlerRespeck = Handler(looperRespeck)
        requireContext().registerReceiver(
            respeckLiveUpdateReceiver,
            filterTestRespeck,
            null,
            handlerRespeck
        )

    }

    override fun onPause() {
        // stop recording if it's recording
        if (mIsRespeckRecording) {
            cancelRecording()
        }
        super.onPause()
        // unregister the broadcast receiver if it's registered
        if (this::respeckLiveUpdateReceiver.isInitialized) {
            requireContext().unregisterReceiver(respeckLiveUpdateReceiver)
            looperRespeck.quit()
        }
    }

    private fun setupClassifierSpinner(view: View) {
        val classifiers = resources.getStringArray(R.array.respeckClassifiers)
        classifierSpinner = view.findViewById(R.id.classifierSpinner)
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, classifiers)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // make the spinner text colour white
        classifierSpinner.adapter = adapter
        classifierSpinner.setSelection(0, false)

        classifierSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {

                val newClassifier = when (parent.getItemAtPosition(position).toString()) {
                    "General activities" -> GENERAL_CLASSIFIER
                    "Stationary activities" -> STATIONARY_CLASSIFIER
                    else -> TASK3_CLASSIFIER
                }

                if (newClassifier != currentClassifier) {
                    currentClassifier.clearInputBuffer()
                    currentClassifier = newClassifier
                }

            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                classifierSpinner.setSelection(0, false)
                currentClassifier = GENERAL_CLASSIFIER
            }
        }

    }

    private fun setupCharts(view: View) {
        respeckChart = view.findViewById(R.id.respeck_chart_detect)

        // Respeck
        time = 0f
        val entries_res_accel_x = ArrayList<Entry>()
        val entries_res_accel_y = ArrayList<Entry>()
        val entries_res_accel_z = ArrayList<Entry>()

        dataSet_res_accel_x = LineDataSet(entries_res_accel_x, "Accel X")
        dataSet_res_accel_y = LineDataSet(entries_res_accel_y, "Accel Y")
        dataSet_res_accel_z = LineDataSet(entries_res_accel_z, "Accel Z")

        dataSet_res_accel_x.setDrawCircles(false)
        dataSet_res_accel_y.setDrawCircles(false)
        dataSet_res_accel_z.setDrawCircles(false)

        dataSet_res_accel_x.color = ContextCompat.getColor(
            requireContext(),
            R.color.red
        )
        dataSet_res_accel_y.color = ContextCompat.getColor(
            requireContext(),
            R.color.green
        )
        dataSet_res_accel_z.color = ContextCompat.getColor(
            requireContext(),
            R.color.blue
        )

        val dataSetsRes = ArrayList<ILineDataSet>()
        dataSetsRes.add(dataSet_res_accel_x)
        dataSetsRes.add(dataSet_res_accel_y)
        dataSetsRes.add(dataSet_res_accel_z)

        allRespeckData = LineData(dataSetsRes)
        respeckChart.data = allRespeckData
        respeckChart.invalidate()

    }

    fun updateGraph(graph: String, x: Float, y: Float, z: Float) {
        // take the first element from the queue
        // and update the graph with it
        if (graph == "respeck") {
            dataSet_res_accel_x.addEntry(Entry(time, x))
            dataSet_res_accel_y.addEntry(Entry(time, y))
            dataSet_res_accel_z.addEntry(Entry(time, z))

            runOnUiThread {
                allRespeckData.notifyDataChanged()
                respeckChart.notifyDataSetChanged()
                respeckChart.invalidate()
                respeckChart.setVisibleXRangeMaximum(150f)
                respeckChart.moveViewToX(respeckChart.lowestVisibleX + 40)
            }
        }
    }

}