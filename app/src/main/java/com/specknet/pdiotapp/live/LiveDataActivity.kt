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
import com.specknet.pdiotapp.utils.DataQueueNew
import com.specknet.pdiotapp.utils.Inference
import com.specknet.pdiotapp.utils.RESpeckLiveData
import com.specknet.pdiotapp.utils.ThingyLiveData


class LiveDataActivity : AppCompatActivity() {

    // global graph variables
    lateinit var dataSet_res_accel_x: LineDataSet
    lateinit var dataSet_res_accel_y: LineDataSet
    lateinit var dataSet_res_accel_z: LineDataSet

    lateinit var dataSet_thingy_accel_x: LineDataSet
    lateinit var dataSet_thingy_accel_y: LineDataSet
    lateinit var dataSet_thingy_accel_z: LineDataSet

    var time = 0f
    lateinit var allRespeckData: LineData

    lateinit var allThingyData: LineData

    lateinit var respeckChart: LineChart
    lateinit var thingyChart: LineChart

    // global broadcast receiver so we can unregister it
    lateinit var respeckLiveUpdateReceiver: BroadcastReceiver
    lateinit var thingyLiveUpdateReceiver: BroadcastReceiver
    lateinit var looperRespeck: Looper
    lateinit var looperThingy: Looper

    // Data Queue of all Respeck stats
    /////////////////////////////////////////////////////////////////////////////////////////
    lateinit var accelXDataQueue: DataQueue
    lateinit var accelYDataQueue: DataQueue
    lateinit var accelZDataQueue: DataQueue

    lateinit var gyroXDataQueue: DataQueue
    lateinit var gyroYDataQueue: DataQueue
    lateinit var gyroZDataQueue: DataQueue

    lateinit var inferenceClass : Inference
    /////////////////////////////////////////////////////////////////////////////////////////
    // New DataQueue
    lateinit var respeckDataQueue : DataQueueNew

    // update with new model inference classes
    lateinit var motionInference : Inference
    lateinit var stationaryInference : Inference
    lateinit var dynamicInference : Inference
    lateinit var breathingInference : Inference

    var timeBetweenPrediction = 10;

    lateinit var lastPrediction : String

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

        var queueLimit = 25

        /////////////////////////////////////////////////////////////////////////////////////////

        // Old version
        accelXDataQueue = DataQueue(queueLimit);
        accelYDataQueue = DataQueue(queueLimit);
        accelZDataQueue = DataQueue(queueLimit);
        gyroXDataQueue = DataQueue(queueLimit);
        gyroYDataQueue = DataQueue(queueLimit);
        gyroZDataQueue = DataQueue(queueLimit);

        inferenceClass = Inference(this)
        inferenceClass.loadModel();

        /////////////////////////////////////////////////////////////////////////////////////////
        // new version
        respeckDataQueue = DataQueueNew(queueLimit)

        motionInference = Inference(this)
        motionInference.loadModel();

        stationaryInference = Inference(this)
        stationaryInference.loadModel();

        dynamicInference = Inference(this)
        dynamicInference.loadModel();

        breathingInference = Inference(this)
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

                    /////////////////////////////////////////////////////////////////////////////////////////
                    // old version
                    accelXDataQueue.add(accelX)
                    accelYDataQueue.add(accelY)
                    accelZDataQueue.add(accelZ)

                    gyroXDataQueue.add(gyroX)
                    gyroYDataQueue.add(gyroY)
                    gyroZDataQueue.add(gyroZ)

                    time += 1

                    updateGraph("respeck", accelX, accelY, accelZ)

                    if ((time.toInt() % timeBetweenPrediction) == 0) {
                        updateActivityView(time)
                    }
                    /////////////////////////////////////////////////////////////////////////////////////////
                    // new version

                    respeckDataQueue.add(
                        accelX, accelY, accelZ,
                        gyroX, gyroY, gyroZ)

                    time += 1

                    updateGraph("respeck", accelX, accelY, accelZ)

                    if ((time.toInt() % timeBetweenPrediction) == 0) {
                        updateActivityView(time)
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
                    updateGraph("thingy", x, y, z)
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
        thingyChart = findViewById(R.id.thingy_chart)

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

        // Thingy

        time = 0f
        val entries_thingy_accel_x = ArrayList<Entry>()
        val entries_thingy_accel_y = ArrayList<Entry>()
        val entries_thingy_accel_z = ArrayList<Entry>()

        dataSet_thingy_accel_x = LineDataSet(entries_thingy_accel_x, "Accel X")
        dataSet_thingy_accel_y = LineDataSet(entries_thingy_accel_y, "Accel Y")
        dataSet_thingy_accel_z = LineDataSet(entries_thingy_accel_z, "Accel Z")

        dataSet_thingy_accel_x.setDrawCircles(false)
        dataSet_thingy_accel_y.setDrawCircles(false)
        dataSet_thingy_accel_z.setDrawCircles(false)

        dataSet_thingy_accel_x.setColor(
            ContextCompat.getColor(
                this,
                R.color.red
            )
        )
        dataSet_thingy_accel_y.setColor(
            ContextCompat.getColor(
                this,
                R.color.green
            )
        )
        dataSet_thingy_accel_z.setColor(
            ContextCompat.getColor(
                this,
                R.color.blue
            )
        )

        val dataSetsThingy = ArrayList<ILineDataSet>()
        dataSetsThingy.add(dataSet_thingy_accel_x)
        dataSetsThingy.add(dataSet_thingy_accel_y)
        dataSetsThingy.add(dataSet_thingy_accel_z)

        allThingyData = LineData(dataSetsThingy)
        thingyChart.data = allThingyData
        thingyChart.invalidate()
    }

    fun setupActivity() {
        time = 0f
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
        } else if (graph == "thingy") {
            dataSet_thingy_accel_x.addEntry(Entry(time, x))
            dataSet_thingy_accel_y.addEntry(Entry(time, y))
            dataSet_thingy_accel_z.addEntry(Entry(time, z))

            runOnUiThread {
                allThingyData.notifyDataChanged()
                thingyChart.notifyDataSetChanged()
                thingyChart.invalidate()
                thingyChart.setVisibleXRangeMaximum(150f)
                thingyChart.moveViewToX(thingyChart.lowestVisibleX + 40)
            }
        }


    }
    //////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////////
    fun updateActivityView(time: Float) {
        // Display the new value in the text view.

        accelXDataQueue.calculateMeanAndStd();
        accelYDataQueue.calculateMeanAndStd();
        accelZDataQueue.calculateMeanAndStd();

        gyroXDataQueue.calculateMeanAndStd();
        gyroYDataQueue.calculateMeanAndStd();
        gyroZDataQueue.calculateMeanAndStd();

        val inputData: FloatArray = floatArrayOf(
            accelXDataQueue.std,
            accelYDataQueue.std,
            accelZDataQueue.std,
            gyroXDataQueue.std,
            gyroYDataQueue.std,
            gyroZDataQueue.std,

            accelXDataQueue.mean,
            accelYDataQueue.mean,
            accelZDataQueue.mean,
            gyroXDataQueue.mean,
            gyroYDataQueue.mean,
            gyroZDataQueue.mean
        )
        val outputData = inferenceClass.runInference(inputData);

        val metadata = "time: $time - \n" +
                "Prediction: $outputData"

        runOnUiThread {
            showActivityTextView.text = metadata
        }
    }

    fun updateActivityViewNew(time: Float) {
        // Display the new value in the text view.

        val motionOutput = motionInference.runInference(respeckDataQueue.respeckQueue);
        if (motionOutput=="Stationary"){
            val stationaryOutput = stationaryInference.runInference(respeckDataQueue.respeckQueue);
            val breathingOutput = breathingInference.runInference(respeckDataQueue.respeckQueue);

            lastPrediction = "$stationaryOutput $breathingOutput"
        }
        else {
            val dynamicOutput = dynamicInference.runInference((respeckDataQueue.respeckQueue))

            lastPrediction = "$dynamicOutput"
        }

        runOnUiThread {
            showActivityTextView.text = lastPrediction
        }
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
