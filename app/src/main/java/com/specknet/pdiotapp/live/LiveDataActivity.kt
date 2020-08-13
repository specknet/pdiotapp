package com.specknet.pdiotapp.live

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
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.specknet.pdiotapp.utils.DelayRespeck
import com.specknet.pdiotapp.R
import com.specknet.pdiotapp.utils.RespeckData
import com.specknet.pdiotapp.utils.Constants
import java.util.concurrent.BlockingQueue
import java.util.concurrent.DelayQueue
import kotlin.collections.ArrayList
import kotlin.math.sqrt



class LiveDataActivity : AppCompatActivity() {

    private lateinit var prediction: TextView
    private lateinit var confidence: TextView

    // display queue to update the graph smoothly
    private var mDelayRespeckQueue: BlockingQueue<DelayRespeck> = DelayQueue<DelayRespeck>()

    // global graph variables
    lateinit var dataSet_x: LineDataSet
    lateinit var dataSet_y: LineDataSet
    lateinit var dataSet_z: LineDataSet
    lateinit var dataSet_mag: LineDataSet
    var time = 0f
    lateinit var allAccelData: LineData
    lateinit var chart: LineChart

    // global broadcast receiver so we can unregister it
    lateinit var respeckLiveUpdateReceiver: BroadcastReceiver
    lateinit var looper: Looper

    val filterTest = IntentFilter(Constants.ACTION_INNER_RESPECK_BROADCAST)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_data)

        // get the accel fields
        var accel_x = findViewById<TextView>(R.id.breathing_rate_sec)
        var accel_y = findViewById<TextView>(R.id.breathing_rate_min)
        var accel_z = findViewById<TextView>(R.id.breathing_signal)

        prediction = findViewById(R.id.breath_count)
        confidence = findViewById(R.id.confidence)

        setupChart()

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
                    }

                    time += 1
                    updateGraph()

                }
            }
        }

        // register receiver on another thread
        val handlerThread = HandlerThread("bgThread")
        handlerThread.start()
        looper = handlerThread.looper
        val handler = Handler(looper)
        this.registerReceiver(respeckLiveUpdateReceiver, filterTest, null, handler)

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


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(respeckLiveUpdateReceiver)
        looper.quit()
    }
}
