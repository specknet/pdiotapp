package com.specknet.pdiotapp.live

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.specknet.pdiotapp.R
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.DataQueue
import com.specknet.pdiotapp.utils.MotionInference
import com.specknet.pdiotapp.utils.StaticInference
import com.specknet.pdiotapp.utils.DynamicInference
import com.specknet.pdiotapp.utils.BreathingInference
import com.specknet.pdiotapp.utils.RESpeckLiveData
import com.specknet.pdiotapp.utils.ThingyLiveData
import kotlinx.android.synthetic.main.fragment_on_boarding.view.description_text


class LiveDataActivity : AppCompatActivity() {

    // global graph variables
    lateinit var dataSet_res_accel_x: LineDataSet
    lateinit var dataSet_res_accel_y: LineDataSet
    lateinit var dataSet_res_accel_z: LineDataSet

    lateinit var dataSet_res_gyro_x: LineDataSet
    lateinit var dataSet_res_gyro_y: LineDataSet
    lateinit var dataSet_res_gyro_z: LineDataSet

    lateinit var dataSet_thingy_accel_x: LineDataSet
    lateinit var dataSet_thingy_accel_y: LineDataSet
    lateinit var dataSet_thingy_accel_z: LineDataSet

    var time = 0f
    lateinit var allRespeckData: LineData
    lateinit var allRespeckDataGyro: LineData


    lateinit var allThingyData: LineData

    lateinit var respeckChart: LineChart
    lateinit var respeckGyroChart: LineChart
    lateinit var thingyChart: LineChart

    // global broadcast receiver so we can unregister it
    lateinit var respeckLiveUpdateReceiver: BroadcastReceiver
    lateinit var thingyLiveUpdateReceiver: BroadcastReceiver
    lateinit var looperRespeck: Looper
    lateinit var looperThingy: Looper

    /////////////////////////////////////////////////////////////////////////////////////////

    lateinit var btnPlay: Button
    lateinit var btnPause: Button
    lateinit var btnReset: Button

    var isPaused = false

    // Data Queue of all Respeck stats
    lateinit var respeckDataQueue : DataQueue

    // Inference classes for each model
    lateinit var motionInference : MotionInference
    lateinit var staticInference : StaticInference
    lateinit var dynamicInference : DynamicInference
    lateinit var breathingInference : BreathingInference

    var timeBetweenPrediction = 25;
    lateinit var lastPrediction : String

    var queueLimit =15


    /////////////////////////////////////////////////////////////////////////////////////////

    val filterTestRespeck = IntentFilter(Constants.ACTION_RESPECK_LIVE_BROADCAST)
    val filterTestThingy = IntentFilter(Constants.ACTION_THINGY_BROADCAST)

    // activity
    lateinit var showActivityTextView : TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_data)

        showActivityTextView = findViewById(R.id.activity_pred_text)

        setupCharts()
        /////////////////////////////////////////////////////////////////////////////////////////

        btnPlay = findViewById(R.id.btnPlay)
        btnPause = findViewById(R.id.btnPause)
        btnReset = findViewById(R.id.btnReset)

        btnPlay.setOnClickListener {
            // Handle play action
            isPaused = false
        }

        btnPause.setOnClickListener {
            // Handle pause action
            isPaused = true
        }

        btnReset.setOnClickListener {
            // Handle reset action
            resetGraph()
        }

        respeckDataQueue = DataQueue(queueLimit)

        motionInference = MotionInference(this)
        motionInference.loadModel();

        staticInference = StaticInference(this)
        staticInference.loadModel();

        dynamicInference = DynamicInference(this)
        dynamicInference.loadModel();

        breathingInference = BreathingInference(this)
        breathingInference.loadModel();

        /////////////////////////////////////////////////////////////////////////////////////////


        // set up the broadcast receiver
        respeckLiveUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                Log.i("thread", "I am running on thread = " + Thread.currentThread().name)

                val action = intent.action

                if (action == Constants.ACTION_RESPECK_LIVE_BROADCAST) {

                    val liveData =
                        intent.getSerializableExtra(Constants.RESPECK_LIVE_DATA) as RESpeckLiveData
                    Log.d("Live", "onReceive: liveData = " + liveData)

                    // get all relevant intent contents
                    val accelX = liveData.accelX
                    val accelY = liveData.accelY
                    val accelZ = liveData.accelZ

                    val gyroX = liveData.gyro.x
                    val gyroY = liveData.gyro.y
                    val gyroZ = liveData.gyro.z

                    respeckDataQueue.add(
                        accelX, accelY, accelZ,
                        gyroX, gyroY, gyroZ)

                    time+=1
                    updateGraph("respeck", accelX, accelY, accelZ,
                        gyroX, gyroY, gyroZ)

                    if ((time.toInt() % timeBetweenPrediction) == 0) {
                        updateActivityView()
                    }

                    /////////////////////////////////////////////////////////////////////////////////////////
                }
            }
        }


        // register receiver on another thread
        val handlerThreadRespeck = HandlerThread("bgThreadRespeckLive")
        handlerThreadRespeck.start()
        looperRespeck = handlerThreadRespeck.looper
        val handlerRespeck = Handler(looperRespeck)
        this.registerReceiver(respeckLiveUpdateReceiver, filterTestRespeck, null, handlerRespeck)

        // set up the broadcast receiver
        thingyLiveUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                Log.i("thread", "I am running on thread = " + Thread.currentThread().name)

                val action = intent.action

                if (action == Constants.ACTION_THINGY_BROADCAST) {

                    val liveData =
                        intent.getSerializableExtra(Constants.THINGY_LIVE_DATA) as ThingyLiveData
                    Log.d("Live", "onReceive: liveData = " + liveData)

                    // get all relevant intent contents
                    val x = liveData.accelX
                    val y = liveData.accelY
                    val z = liveData.accelZ

                    time += 1
                    //updateGraph("thingy", x, y, z)
                return
                }
            }
        }

        // register receiver on another thread
        val handlerThreadThingy = HandlerThread("bgThreadThingyLive")
        handlerThreadThingy.start()
        looperThingy = handlerThreadThingy.looper
        val handlerThingy = Handler(looperThingy)
        this.registerReceiver(thingyLiveUpdateReceiver, filterTestThingy, null, handlerThingy)

    }

    fun setupCharts() {
        respeckChart = findViewById(R.id.respeck_chart)
        respeckGyroChart = findViewById(R.id.respeck_chart_gyro)

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

        dataSet_res_accel_x.setDrawValues(false)
        dataSet_res_accel_y.setDrawValues(false)
        dataSet_res_accel_z.setDrawValues(false)

        dataSet_res_accel_x.setColor(
            ContextCompat.getColor(
                this,
                R.color.red
            )
        )
        dataSet_res_accel_y.setColor(
            ContextCompat.getColor(
                this,
                R.color.green
            )
        )
        dataSet_res_accel_z.setColor(
            ContextCompat.getColor(
                this,
                R.color.blue
            )
        )

        val dataSetsRes = ArrayList<ILineDataSet>()
        dataSetsRes.add(dataSet_res_accel_x)
        dataSetsRes.add(dataSet_res_accel_y)
        dataSetsRes.add(dataSet_res_accel_z)

        allRespeckData = LineData(dataSetsRes)
        respeckChart.data = allRespeckData
        respeckChart.invalidate()

        // RespeckGyro

        val entries_res_gyro_x = ArrayList<Entry>()
        val entries_res_gyro_y = ArrayList<Entry>()
        val entries_res_gyro_z = ArrayList<Entry>()

        dataSet_res_gyro_x = LineDataSet(entries_res_gyro_x, "Gyro X")
        dataSet_res_gyro_y = LineDataSet(entries_res_gyro_y, "Gyro Y")
        dataSet_res_gyro_z = LineDataSet(entries_res_gyro_z, "Gyro Z")

        dataSet_res_gyro_x.setDrawCircles(false)
        dataSet_res_gyro_y.setDrawCircles(false)
        dataSet_res_gyro_z.setDrawCircles(false)

        dataSet_res_gyro_x.setDrawValues(false)
        dataSet_res_gyro_y.setDrawValues(false)
        dataSet_res_gyro_z.setDrawValues(false)

        dataSet_res_gyro_x.setColor(
            ContextCompat.getColor(
                this,
                R.color.red
            )
        )
        dataSet_res_gyro_y.setColor(
            ContextCompat.getColor(
                this,
                R.color.green
            )
        )
        dataSet_res_gyro_z.setColor(
            ContextCompat.getColor(
                this,
                R.color.blue
            )
        )

        val dataSetsResGyro = ArrayList<ILineDataSet>()
        dataSetsResGyro.add(dataSet_res_gyro_x)
        dataSetsResGyro.add(dataSet_res_gyro_y)
        dataSetsResGyro.add(dataSet_res_gyro_z)

        allRespeckDataGyro = LineData(dataSetsResGyro)
        respeckGyroChart.data = allRespeckDataGyro
        respeckGyroChart.invalidate()
    }

    fun setupActivity() {
        time = 0f
    }

    fun updateGraph(graph: String, x: Float, y: Float, z: Float, gyroX: Float, gyroY: Float, gyroZ: Float) {

        if (isPaused) {
            // Do nothing if paused
            return
        }

        // take the first element from the queue
        // and update the graph with it
        if (graph == "respeck") {
            dataSet_res_accel_x.addEntry(Entry(time, x))
            dataSet_res_accel_y.addEntry(Entry(time, y))
            dataSet_res_accel_z.addEntry(Entry(time, z))

            dataSet_res_gyro_x.addEntry(Entry(time, gyroX))
            dataSet_res_gyro_y.addEntry(Entry(time, gyroY))
            dataSet_res_gyro_z.addEntry(Entry(time, gyroZ))

            runOnUiThread {
                allRespeckData.notifyDataChanged()
                respeckChart.notifyDataSetChanged()
                respeckChart.invalidate()
                respeckChart.setVisibleXRangeMaximum(150f)
                respeckChart.moveViewToX(time-80)

                allRespeckDataGyro.notifyDataChanged()
                respeckGyroChart.notifyDataSetChanged()
                respeckGyroChart.invalidate()
                respeckGyroChart.setVisibleXRangeMaximum(150f)
                respeckGyroChart.moveViewToX(time-80)
            }
        }
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////////
    fun updateActivityView() {

        if (isPaused) {
            // Do nothing if paused
            return
        }

        // Doesnt do anything if queue isn't of correct length
        if (respeckDataQueue.length<queueLimit){
            runOnUiThread {
                showActivityTextView.text = ""
            }
            return
        }

        val motionOutput = motionInference.runInference(respeckDataQueue.list);

        // if static, infers static position and breathing type
        if (motionOutput=="Static"){
            val staticOutput = staticInference.runInference(respeckDataQueue.list);
            val breathingOutput = breathingInference.runInference(respeckDataQueue.list);
            lastPrediction = "$staticOutput $breathingOutput"
        }
        // if dynamic, infers dynamic motion
        else {
            val dynamicOutput = dynamicInference.runInference(respeckDataQueue.list)
            lastPrediction = "$dynamicOutput"
        }

        // displays full prediction
        runOnUiThread {
            showActivityTextView.text = lastPrediction
        }
    }

    fun resetGraph() {
        // Clear data and reset the graph

        // Clear data in your queues or any other data structures
        //respeckDataQueue.clear()

        // Clear entries in your LineDataSet
        dataSet_res_accel_x.clear()
        dataSet_res_accel_y.clear()
        dataSet_res_accel_z.clear()

        // Notify the chart data changed
        allRespeckData.notifyDataChanged()
        respeckChart.notifyDataSetChanged()
        respeckChart.invalidate()

        respeckChart.moveViewToX(0f)

        // Clear entries in your Gyro LineDataSet
        dataSet_res_gyro_x.clear()
        dataSet_res_gyro_y.clear()
        dataSet_res_gyro_z.clear()

        // Notify the chart data changed
        allRespeckDataGyro.notifyDataChanged()
        respeckGyroChart.notifyDataSetChanged()
        respeckGyroChart.invalidate()

        respeckGyroChart.moveViewToX(0f)

        // Reset time if needed
        time = 0f
    }

//////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(respeckLiveUpdateReceiver)
        unregisterReceiver(thingyLiveUpdateReceiver)
        looperRespeck.quit()
        looperThingy.quit()
    }
}
