package com.specknet.pdiotapp

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.widget.DatePicker
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.specknet.pdiotapp.utils.GlobalVars
import java.util.*


class ViewHistoricData : AppCompatActivity() {

    lateinit var datepickerObj : DatePicker
    lateinit var pieChart : PieChart
    lateinit var piedataSet : PieDataSet
    var emptyMap = mapOf<String, Int>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_historic_data)

        // setup calendar, set it to current date and add a listener to any state changes
        val cal = Calendar.getInstance()
        datepickerObj = findViewById(R.id.historic_date_picker)

        // setup datepicker so on date change, we ask the database if the user has recorded any data
        // for that day yet
        datepickerObj.init(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ) { _, year, month, day ->
            // setup onDateChange listener
            // if user isn't logged in there's no point asking the db for anything
            if(GlobalVars.loggedIn) {
                var selectedDate = day.toString() + (month + 1).toString() + year.toString().takeLast(2)

                // add listener to the database, if that changes then update the pie chart
                val postlistener = object : ValueEventListener {
                    override fun onDataChange(dSnap : DataSnapshot) {

                        // god only knows what's happening in here - look it was 3:44am, have mercy
                        var userHistory = (dSnap.child(GlobalVars.accId).value as Map<String, *>).filter {
                                snapshot -> snapshot.key == "historicData"
                        }.values.toList()[0] as Map<String, List<Int>>
0
                        var userDataOnDate = userHistory.get(selectedDate)

                        // if user hasn't recorded any data for selected date, break out of listener
                        if(userDataOnDate == null || userDataOnDate.all { e -> e == 0 } || userDataOnDate.isEmpty()) {
                            Log.i("beans why no crash?", userDataOnDate.toString())
                            setupPieChartDataSet(emptyMap)
                            return
                        }

                        val meeple = mutableMapOf<String, Int>();
                        GlobalVars.complexActivities.forEachIndexed{ index, value ->
                            var activityCount = userDataOnDate[index]

                            if(activityCount > 0 && value.equals("Movement") == false){
                                meeple[value] = userDataOnDate[index];
                            }
                        }


                        Log.i("beans livetest", meeple.toString())
                        setupPieChartDataSet(meeple)


                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.i("beans historic", "cancelled")
                    }
                }

                GlobalVars.dbRef.addValueEventListener(postlistener)
            }

        }

        setupPieChart()

        var set = false;
        if(GlobalVars.loggedIn){
            GlobalVars.dbRef.child(GlobalVars.accId).get().addOnSuccessListener {
                var historicData = (it.children.filter { snapshot -> snapshot.key == "historicData"}[0].value as MutableMap<String, List<Int>>)
                if(historicData.keys.contains(GlobalVars.curDateStr)) {
                    var userDataOnDate = historicData[GlobalVars.curDateStr]
                    if(userDataOnDate != null){
                        val meeple = mutableMapOf<String, Int>();
                        GlobalVars.complexActivities.forEachIndexed{ index, value ->
                            var activityCount = userDataOnDate[index]

                            if(activityCount > 0 && value.equals("Movement") == false){
                                meeple[value] = userDataOnDate[index];
                            }
                        }
                        setupPieChartDataSet(meeple)
                        set = true;
                    }
                }
            }.addOnFailureListener{
                setupPieChartDataSet(null)
            }
        } else{
            setupPieChartDataSet(null)
        }




        handleSignedInStateUI(GlobalVars.loggedIn)
    }

    private fun setupPieChartDataSet(piechartEntries: Map<String, Int>?) {

        if(piechartEntries == null){
            return
        }

        if(piechartEntries.isEmpty()){
            pieChart.invalidate();
            pieChart.clear();
            return;
        }

        val pieChartData: ArrayList<PieEntry> = ArrayList()

        piechartEntries.forEach { e ->
            pieChartData.add(PieEntry(e.value.toFloat(), e.key))
        }

        val pieDataSet = PieDataSet(pieChartData, "Basic activities")
        pieDataSet.sliceSpace = 2f


        // add a lot of colors to list
        val colours: ArrayList<Int> = ArrayList()
        colours.add(resources.getColor(R.color.red))
        colours.add(resources.getColor(R.color.light_green))
        colours.add(resources.getColor(R.color.light_blue))
        colours.add(resources.getColor(R.color.grey))
        colours.add(resources.getColor(R.color.purple))
        colours.add(resources.getColor(R.color.indigo))
        colours.add(resources.getColor(R.color.lime))
        colours.add(resources.getColor(R.color.pink))
        colours.add(resources.getColor(R.color.amber))
        colours.add(resources.getColor(R.color.blue_grey))
        colours.add(resources.getColor(R.color.green))
        colours.add(resources.getColor(R.color.blue))
        colours.add(resources.getColor(R.color.cyan))
        colours.add(resources.getColor(R.color.orange))
        colours.add(resources.getColor(R.color.deep_orange))
        colours.add(resources.getColor(R.color.yellow))
        colours.add(resources.getColor(R.color.deep_purple))
        colours.add(resources.getColor(R.color.teal))
        colours.add(resources.getColor(R.color.colorAccent))

        pieDataSet.colors = colours

        // on below line we are setting pie data set
        val data = PieData(pieDataSet)
        data.setValueTextSize(15f)
        data.setValueTypeface(Typeface.DEFAULT_BOLD)
        data.setValueTextColor(Color.WHITE)

        pieChart.setData(data)
        pieChart.invalidate();
    }

    private fun setupPieChart(){
        pieChart = findViewById(R.id.pieChart)
        pieChart.description.isEnabled = false
        pieChart.setExtraOffsets(5f, 10f, 5f, 5f)

        pieChart.setDragDecelerationFrictionCoef(0.90f)

        pieChart.setDrawHoleEnabled(false)
        pieChart.setHoleColor(Color.WHITE)

        pieChart.setTransparentCircleColor(Color.WHITE)
        pieChart.setTransparentCircleAlpha(110)

        // on  below line we are setting hole radius
        //pieChart.setHoleRadius(58f)
        //pieChart.setTransparentCircleRadius(61f)

        // on below line we are setting center text
        pieChart.setDrawCenterText(true)

        // on below line we are setting
        // rotation for our pie chart
        pieChart.setRotationAngle(0f)

        // enable rotation of the pieChart by touch
        pieChart.setRotationEnabled(true)
        pieChart.setHighlightPerTapEnabled(true)


        // on below line we are disabling our legend for pie chart
        pieChart.legend.isEnabled = true
        pieChart.setEntryLabelColor(Color.WHITE)
        pieChart.setEntryLabelTextSize(12f)

        val l = pieChart.legend
        l.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        l.horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
        l.orientation = Legend.LegendOrientation.HORIZONTAL
        l.setDrawInside(false)
        l.xEntrySpace = 4f
        l.yEntrySpace = 0f
        l.isWordWrapEnabled = true
    }

    // state = true -> logged in
    // state = false -> logged out
    private fun handleSignedInStateUI(state: Boolean){
        runOnUiThread{
            (findViewById(R.id.login_required_warning) as TextView).isEnabled = !state;
            (findViewById(R.id.login_required_warning) as TextView).isVisible = !state;



        }
    }
}