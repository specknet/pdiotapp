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
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.specknet.pdiotapp.R
import com.specknet.pdiotapp.UserData
import com.specknet.pdiotapp.ml.*
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.RESpeckLiveData
import com.specknet.pdiotapp.utils.ThingyLiveData
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import kotlin.collections.ArrayList
import com.specknet.pdiotapp.utils.GlobalVars
import org.w3c.dom.Text
import java.sql.Time
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.random.Random


class LiveDataActivity : AppCompatActivity() {

    // global graph variables
    lateinit var dataSet_res_accel_x: LineDataSet
    lateinit var dataSet_res_accel_y: LineDataSet
    lateinit var dataSet_res_accel_z: LineDataSet

    lateinit var dataSet_thingy_accel_x: LineDataSet
    lateinit var dataSet_thingy_accel_y: LineDataSet
    lateinit var dataSet_thingy_accel_z: LineDataSet

    lateinit var startButton : Button;
    lateinit var stopButton : Button;
    lateinit var welcomeText : TextView;
    lateinit var curEssential : TextView;
    lateinit var curSpecActivity : TextView;
    lateinit var respecknetConnectedTxt : TextView;
    lateinit var thingyConnectedTxt : TextView;


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


    val filterTestRespeck = IntentFilter(Constants.ACTION_RESPECK_LIVE_BROADCAST)
    val filterTestThingy = IntentFilter(Constants.ACTION_THINGY_BROADCAST)

    val ROWS = 20
    val VALUES = 12
    var classy = -1

    var isRecording = false;
    var isThingyReady = false;
    var isRespeckReady = false;
    var isReadyToClassify = true;

    var respeckData : MutableList<MutableList<Float>> = mutableListOf();
    var thingyData : MutableList<MutableList<Float>> = mutableListOf();

    var mostRecentRespeck : MutableList<MutableList<Float>> = mutableListOf();
    var mostRecentThingy : MutableList<MutableList<Float>> = mutableListOf();

    var respeckLastReceiveTime : Long = System.currentTimeMillis() - 50000
    var thingyLastReceiveTime : Long = System.currentTimeMillis() - 50000

    // 0 - state unknown
    // 1 - connected
    // 2 - not connected
    var respeckConnected : Int = 0;
    var thingyConnected : Int = 0;

    val funtimer : Timer = Timer()

    //var liveDataChunk = ArrayDeque<Float>(ROWS * VALUES)

    fun classify(): String {
        val model = Subset95320x12Cnn.newInstance(applicationContext)

        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 20, 12), DataType.FLOAT32)

        var liveDataChunk = mutableListOf<Float>()

        for(i in 0 until 20){
            val x = mostRecentThingy.toList().get(i) + mostRecentRespeck.toList().get(i)
            for(j in 0 until 12){
                liveDataChunk.add(x[j])
            }
        }

        inputFeature0.loadArray(liveDataChunk.toFloatArray(), intArrayOf(1, 20, 12))

        val outputs = model.process(inputFeature0).outputFeature0AsTensorBuffer
        val outputFeature0 = outputs.floatArray

        val maxIdx = outputFeature0.max()?.let { outputFeature0.indexOf(it) }
        var text = "UH OH BROKEN"
        when(maxIdx) {
            0 -> text = "Sitting"
            1 -> text = "Lying Down"
            2 -> text = "Walking"
            3 -> text = "Running"
            4 -> text = "Movement"
        }

        curEssential.setText(text)

        var backupText = text
        var specificActivity = text

        if(maxIdx == 0){
            val subModel = Sitting78720x12Cnn.newInstance(applicationContext)
            inputFeature0.loadArray(liveDataChunk.toFloatArray(), intArrayOf(1, 20, 12))
            val outputs = subModel.process(inputFeature0).outputFeature0AsTensorBuffer
            val outputFeature0 = outputs.floatArray
            val maxIdx = outputFeature0.max()?.let { outputFeature0.indexOf(it) }
            when(maxIdx) {
                0 -> specificActivity = "Sitting"
                1 -> specificActivity = "Sitting bent forward"
                2 -> specificActivity = "Sitting bent backward"
                3 -> specificActivity = "Standing"
                4 -> specificActivity = "Desk work"
            }
            subModel.close()
        }
        else if(maxIdx == 1){
            val subModel = Lying99320x12Cnn.newInstance(applicationContext)
            inputFeature0.loadArray(liveDataChunk.toFloatArray(), intArrayOf(1, 20, 12))
            val outputs = subModel.process(inputFeature0).outputFeature0AsTensorBuffer
            val outputFeature0 = outputs.floatArray
            val maxIdx = outputFeature0.max()?.let { outputFeature0.indexOf(it) }
            when(maxIdx) {
                0 -> specificActivity = "Lying down on back"
                1 -> specificActivity = "Lying down right"
                2 -> specificActivity = "Lying down left"
                3 -> specificActivity = "Lying down on stomach"
            }
            subModel.close()
        }
        else if(maxIdx == 2){
            val subModel = Walking81520x12Cnn.newInstance(applicationContext)
            inputFeature0.loadArray(liveDataChunk.toFloatArray(), intArrayOf(1, 20, 12))
            val outputs = subModel.process(inputFeature0).outputFeature0AsTensorBuffer
            val outputFeature0 = outputs.floatArray
            val maxIdx = outputFeature0.max()?.let { outputFeature0.indexOf(it) }
            when(maxIdx) {
                0 -> specificActivity = "Walking"
                1 -> specificActivity = "Climbing stairs"
                2 -> specificActivity = "Descending stairs"
            }
        }

        curSpecActivity.setText(specificActivity)

        var predClass = -1
        when(specificActivity) {
            "Sitting" -> predClass = 0
            "Sitting bent forward" -> predClass = 1
            "Sitting bent backward" -> predClass = 2
            "Standing" -> predClass = 3
            "Lying down on back" -> predClass = 4
            "Lying down right" -> predClass = 5
            "Lying down left" -> predClass = 6
            "Lying down on stomach" -> predClass = 7
            "Walking" -> predClass = 8
            "Running" -> predClass = 9
            "Climbing stairs" -> predClass = 10
            "Descending stairs" -> predClass = 11
            "Desk work" -> predClass = 12
            "Movement" -> predClass = 13
        }



        // send classification to user id
        // if logged in, wait to upload before classifying again
        // else let classifications happen again
        if(GlobalVars.loggedIn){
            GlobalVars.dbRef.child(GlobalVars.accId).get().addOnSuccessListener {
                var historicData = (it.children.filter { snapshot -> snapshot.key == "historicData"}[0].value as MutableMap<String, List<Int>>)
                //Log.i("bean-man-BIGTEST", historicData[GlobalVars.curDateStr].toString())
                var todaysData = historicData[GlobalVars.curDateStr]?.toMutableList()
                if (todaysData != null) { // this should *never* happens, if it does - find god and start stabbing
                    todaysData.set(predClass, todaysData[predClass] + 1)
                    historicData[GlobalVars.curDateStr] = todaysData.toList()
                };

                var updatedUser = UserData(GlobalVars.accId, GlobalVars.accName, historicData)
                var updatedUserValues = updatedUser.toMap()

                GlobalVars.dbRef.child(GlobalVars.accId).updateChildren(updatedUserValues)
                    .addOnCompleteListener{
                        isReadyToClassify = true;
                        Log.i("DATABASE", "inserted!")
                    }.addOnFailureListener{
                        isReadyToClassify = true;
                        Log.i("DATABASE", "failed to insert >:(")
                    }
                Log.i("bean-man-BIGTEST", todaysData.toString())
            }
        } else {
            Log.i("bean-man-BIGTEST", "YOU ARENT LOGGED IN IDOT")
            isReadyToClassify = true;
        }
        Log.i("beans man class", backupText + " -> " + specificActivity)


        model.close()
        return specificActivity;
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("logged3", GlobalVars.accName)
        setContentView(R.layout.activity_live_data)
        setupCharts()
        (findViewById(R.id.textView2) as TextView).setText("abcd")
        supportActionBar?.hide()


        startButton = findViewById(R.id.record_button)
        stopButton = findViewById(R.id.stop_record_button)


        startButton.setOnClickListener {
            if(respeckConnected == 1 && thingyConnected == 1){
                isRecording = true;
                runOnUiThread{
                    stopButton.isEnabled = true;
                    stopButton.isVisible = true;
                    startButton.isEnabled = false;
                    startButton.isVisible = false;
                }
            }
        }


        welcomeText = findViewById(R.id.welcome_user)
        if(GlobalVars.loggedIn){
            welcomeText.setText("Hi, " + GlobalVars.accName+"!")
        }

        stopButton.setOnClickListener {
            isRecording = false;
            runOnUiThread{
                startButton.isEnabled = true;
                startButton.isVisible = true;
                stopButton.isVisible = false;
                stopButton.isEnabled = false;
            }
        }


        curEssential = findViewById(R.id.currentActivity)
        curSpecActivity = findViewById(R.id.currentDetailedActivity)
        respecknetConnectedTxt = findViewById(R.id.RespeckStatus)
        thingyConnectedTxt = findViewById(R.id.thingyStatus)



        // set up the broadcast receiver
        respeckLiveUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                respeckLastReceiveTime = System.currentTimeMillis()

                Log.i("beans count", respeckData.size.toString() + " : " + thingyData.size.toString())
                Log.i("thread", "I am running on thread = " + Thread.currentThread().name)

                val action = intent.action

                if (action == Constants.ACTION_RESPECK_LIVE_BROADCAST) {

                    val liveData =
                        intent.getSerializableExtra(Constants.RESPECK_LIVE_DATA) as RESpeckLiveData
                    Log.d("Live", "onReceive: liveData = " + liveData)

                    // if respeck is ready...
                    if(respeckData.size == ROWS){

                        // save 20 values
                        mostRecentRespeck = respeckData.toMutableList()
                        isRespeckReady = true;

                        // remove oldest value
                        respeckData.removeFirst()
                        Log.i("beans test", "should be ready")
                        if(isThingyReady && isReadyToClassify && isRecording){
                            isRespeckReady = false;
                            isThingyReady = false;
                            isReadyToClassify = false;

                            val txt = classify()
                        }
                    }

                    // get all relevant intent contents
                    val x = liveData.accelX
                    val y = liveData.accelY
                    val z = liveData.accelZ
                    val gyro_x = liveData.gyro.x
                    val gyro_y = liveData.gyro.y
                    val gyro_z = liveData.gyro.z

                    val tempRow = mutableListOf<Float>()
                    tempRow.add(x)
                    tempRow.add(y)
                    tempRow.add(z)
                    tempRow.add(gyro_x)
                    tempRow.add(gyro_y)
                    tempRow.add(gyro_z)

                    respeckData.add(tempRow)

                    time += 1
                    updateGraph("respeck", x, y, z)
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
                thingyLastReceiveTime = System.currentTimeMillis();
                Log.i("thread", "I am running on thread = " + Thread.currentThread().name)

                val action = intent.action

                if (action == Constants.ACTION_THINGY_BROADCAST) {

                    val liveData =
                        intent.getSerializableExtra(Constants.THINGY_LIVE_DATA) as ThingyLiveData
                    Log.d("Live", "onReceive: liveData = " + liveData)

                    if(thingyData.size == 20){
                        mostRecentThingy = thingyData.toMutableList()
                        isThingyReady = true;
                        thingyData.removeFirst()
                    }

                    // get all relevant intent contents
                    val x = liveData.accelX
                    val y = liveData.accelY
                    val z = liveData.accelZ
                    val xg = liveData.gyro.x
                    val yg = liveData.gyro.y
                    val zg = liveData.gyro.z

                    val tempRow = mutableListOf<Float>()
                    tempRow.add(x)
                    tempRow.add(y)
                    tempRow.add(z)
                    tempRow.add(xg)
                    tempRow.add(yg)
                    tempRow.add(zg)

                    thingyData.add(tempRow)


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



        funtimer.scheduleAtFixedRate(
            timerTask(){
                val curTime = System.currentTimeMillis();
                val prevRespeckState = respeckConnected
                val prevThingyState = thingyConnected

                if(curTime - respeckLastReceiveTime <= 2000){
                    respeckConnected = 1
                } else {
                    respeckConnected = 2
                }

                if(curTime - thingyLastReceiveTime <= 2000){
                    thingyConnected = 1
                } else {
                    thingyConnected = 2
                }

                if(prevRespeckState != respeckConnected){
                    if(respeckConnected == 2){
                        isRecording = false;
                        runOnUiThread{
                            respecknetConnectedTxt.setText("Disconnected")
                            respecknetConnectedTxt.setTextColor(resources.getColor(R.color.red))
                            startButton.isEnabled = true;
                            startButton.isVisible = true;
                            stopButton.isVisible = false;
                            stopButton.isEnabled = false;
                        }
                    } else{
                        runOnUiThread{
                            respecknetConnectedTxt.setText("Connected")
                            respecknetConnectedTxt.setTextColor(resources.getColor(R.color.green))
                        }
                    }
                }

                if(prevThingyState != thingyConnected){
                    if(thingyConnected == 2){
                        isRecording = false;
                        runOnUiThread{
                            thingyConnectedTxt.setText("Disconnected")
                            thingyConnectedTxt.setTextColor(resources.getColor(R.color.red))
                            startButton.isEnabled = true;
                            startButton.isVisible = true;
                            stopButton.isVisible = false;
                            stopButton.isEnabled = false;
                        }
                    } else{
                        runOnUiThread{
                            thingyConnectedTxt.setText("Connected")
                            thingyConnectedTxt.setTextColor(resources.getColor(R.color.green))
                        }
                    }
                }

                Log.i("connections state", "respeck: " + respeckConnected.toString() + ", thingy: " + thingyConnected.toString())
            },
            100,
            100)
    }

    fun setupCharts() {
        respeckChart = findViewById(R.id.respeck_chart)
        thingyChart = findViewById(R.id.thingy_chart)

        respeckChart.isVisible = false;
        thingyChart.isVisible = false;


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

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(respeckLiveUpdateReceiver)
        unregisterReceiver(thingyLiveUpdateReceiver)
        looperRespeck.quit()
        looperThingy.quit()
        funtimer.cancel();
    }
}
