package com.specknet.pdiot.live

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
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager
import com.google.firebase.ml.custom.FirebaseCustomRemoteModel
import com.specknet.pdiot.MovePoint
import com.specknet.pdiot.MovementQueue
import com.specknet.pdiot.R
import com.specknet.pdiot.utils.Constants
import com.specknet.pdiot.utils.DelayRespeck
import com.specknet.pdiot.utils.RespeckData
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.BlockingQueue
import java.util.concurrent.DelayQueue
import kotlin.math.sqrt


class LiveDataActivity : AppCompatActivity() {

    // display queue to update the graph smoothly
    private var mDelayRespeckQueue: BlockingQueue<DelayRespeck> = DelayQueue<DelayRespeck>()
    private var interpreter: Interpreter? = null
    private lateinit var classLabels: Array<String?>
    private var lastActivity: String=""
    private lateinit var currentActionText:TextView

    // global graph variables
    lateinit var dataSet_x: LineDataSet
    lateinit var dataSet_y: LineDataSet
    lateinit var dataSet_z: LineDataSet
    lateinit var dataSet_mag: LineDataSet
    var time = 0f
    lateinit var allAccelData: LineData
    lateinit var chart: LineChart
    lateinit var movementQueue:MovementQueue

    // global broadcast receiver so we can unregister it
    lateinit var respeckLiveUpdateReceiver: BroadcastReceiver
    lateinit var looper: Looper

    val filterTest = IntentFilter(Constants.ACTION_INNER_RESPECK_BROADCAST)

    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_data)

        classLabels=getClassLabels("model_class.txt",6)
        movementQueue=MovementQueue(36)
        currentActionText=findViewById(R.id.current_action_text)

        // get the accel fields
        var accel_x = findViewById<TextView>(R.id.breathing_rate_sec)
        var accel_y = findViewById<TextView>(R.id.breathing_rate_min)
        var accel_z = findViewById<TextView>(R.id.breathing_signal)


        setupChart()


        val remoteModel =
            FirebaseCustomRemoteModel.Builder("Movement_Classifier").build()
        val conditions = FirebaseModelDownloadConditions.Builder()
            .requireWifi()
            .build()
        FirebaseModelManager.getInstance().download(remoteModel, conditions)
            .addOnSuccessListener {
                // Download complete. Depending on your app, you could enable
                // the ML feature, or switch from the local model to the remote
                // model, etc.
            }
        FirebaseModelManager.getInstance().getLatestModelFile(remoteModel)
            .addOnCompleteListener { task ->
                val modelFile = task.result
                if (modelFile != null) {
                    interpreter = Interpreter(modelFile)
                    startDataReciever(accel_x,accel_y,accel_z)
                }
            }




        // register receiver on another thread


    }


    fun setupChart() {
        chart = findViewById<LineChart>(R.id.chart)

        time = 0f
        var entries_x = ArrayList<Entry>()
        var entries_y = ArrayList<Entry>()
        var entries_z = ArrayList<Entry>()
        var entries_mag = ArrayList<Entry>()

        dataSet_x = LineDataSet(entries_x, "Accel X")
        dataSet_y = LineDataSet(entries_y, "Accel Y")
        dataSet_z = LineDataSet(entries_z, "Accel Z")
        dataSet_mag = LineDataSet(entries_mag, "Magnitude")

        dataSet_x.setDrawCircles(false)
        dataSet_y.setDrawCircles(false)
        dataSet_z.setDrawCircles(false)
        dataSet_mag.setDrawCircles(false)

        dataSet_x.setColor(ContextCompat.getColor(this,
            R.color.red
        ))
        dataSet_y.setColor(ContextCompat.getColor(this,
            R.color.green
        ))
        dataSet_z.setColor(ContextCompat.getColor(this,
            R.color.blue
        ))
        dataSet_mag.setColor(ContextCompat.getColor(this,
            R.color.yellow
        ))

        var dataSets = ArrayList<ILineDataSet>()
        dataSets.add(dataSet_x)
        dataSets.add(dataSet_y)
        dataSets.add(dataSet_z)
        dataSets.add(dataSet_mag)

        allAccelData = LineData(dataSets)
        chart.data = allAccelData
        chart.invalidate()
    }

    fun updateGraph() {
        // take the first element from the queue
        // and update the graph with it
        val respeckData = mDelayRespeckQueue.take().getData()

        dataSet_x.addEntry(Entry(time, respeckData.accel_x))
        dataSet_y.addEntry(Entry(time, respeckData.accel_y))
        dataSet_z.addEntry(Entry(time, respeckData.accel_z))
        dataSet_mag.addEntry(Entry(time, respeckData.accel_mag))

        runOnUiThread {
            allAccelData.notifyDataChanged()
            chart.notifyDataSetChanged()
            chart.invalidate()
            chart.setVisibleXRangeMaximum(150f)
            Log.i("Chart", "Lowest X = " + chart.lowestVisibleX.toString())
            chart.moveViewToX(chart.lowestVisibleX + 40)
            Log.i("Chart", "Lowest X after = " + chart.lowestVisibleX.toString())
        }

    }

    private fun startDataReciever(accel_x:TextView,accel_y:TextView,accel_z:TextView)
    {
        // set up the broadcast receiver
        respeckLiveUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                Log.i("thread", "I am running on thread = " + Thread.currentThread().name)

                val action = intent.action

                if (action == Constants.ACTION_INNER_RESPECK_BROADCAST) {

                    // get all relevant intent contents
                    val x = intent.getFloatExtra(Constants.EXTRA_RESPECK_LIVE_X, 0f)
                    val y = intent.getFloatExtra(Constants.EXTRA_RESPECK_LIVE_Y, 0f)
                    val z = intent.getFloatExtra(Constants.EXTRA_RESPECK_LIVE_Z, 0f)
                    movementQueue.AddMove(MovePoint(x,y,z))
                    val curLabel:String?=getCurLabel(movementQueue)
                        Log.i(
                            "Label:",
                            String.format("Current activity: %s", curLabel)
                        )



                        // File not found?


                    val mag = sqrt((x*x + y*y + z*z).toDouble())

                    val data =
                        RespeckData(
                            timestamp = 0L,
                            accel_x = x,
                            accel_y = y,
                            accel_z = z,
                            accel_mag = mag.toFloat(),
                            breathingSignal = 0f
                        )
                    val delayRespeck =
                        DelayRespeck(
                            data,
                            79
                        )
                    mDelayRespeckQueue.add(delayRespeck)


                    runOnUiThread {
                        accel_x.text = "accel_x = " + x.toString()
                        accel_y.text = "accel_y = " + y.toString()
                        accel_z.text = "accel_z = " + z.toString()
                        updateCurrentActivity(curLabel)
                    }

                    time += 1
                    updateGraph()

                }
            }
        }

        val handlerThread = HandlerThread("bgThread")
        handlerThread.start()
        looper = handlerThread.looper
        val handler = Handler(looper)
        this.registerReceiver(respeckLiveUpdateReceiver, filterTest, null, handler)
    }

    private fun getCurLabel(movementQueue: MovementQueue): String? {

        if (movementQueue.isFull()) {
            val input: ByteBuffer = movementQueue.ConvertDataToBuffer()
            val bufferSize = 12 * java.lang.Float.SIZE / java.lang.Byte.SIZE
            val modelOutput =
                ByteBuffer.allocateDirect(bufferSize)
                    .order(ByteOrder.nativeOrder())
            interpreter!!.run(input, modelOutput)
            modelOutput.rewind()
            val probabilities = modelOutput.asFloatBuffer()
            return FindLabel(probabilities)}
        return null

    }

    private fun updateCurrentActivity(curLabel:String?) {
        if (curLabel !== lastActivity) {
            if (curLabel != null) {
                lastActivity = curLabel
                currentActionText.setText(lastActivity)
            }
        }

    }

    private fun FindLabel(probabilities: FloatBuffer): String? {
        var maxprob = 0.0f
        var maxIndex = -1
        for (i in 0 until probabilities.capacity()) {
            if (probabilities[i] > maxprob) {
                maxprob = probabilities[i]
                maxIndex = i
            }
        }
        return classLabels.get(maxIndex)
    }
    private fun getClassLabels(
        filename: String,
        numLabels: Int
    ): Array<String?> {
        val classes = arrayOfNulls<String>(numLabels)
        try {
            val reader = BufferedReader(
                InputStreamReader(assets.open(filename))
            )
            for (i in 0 until numLabels) {
                classes[i] = reader.readLine()
            }
        } catch (e: IOException) {
        }
        return classes
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(respeckLiveUpdateReceiver)
        looper.quit()
    }
}
