package com.specknet.pdiotapp

import android.os.Build
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.DatePicker
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.specknet.pdiotapp.sql.DBHelper
import java.lang.StringBuilder
import java.util.Calendar

class ProfileFragment : Fragment() {

    lateinit var usernameTextView: TextView
    lateinit var datePicker: DatePicker
    lateinit var viewActivitiesButton: Button
    lateinit var detectedActivitiesHeader: TextView
    lateinit var detectedActivitiesListText: TextView
    lateinit var username: String
    private lateinit var dbHelper: DBHelper
    private lateinit var today: Calendar

    var savedDay = 0
    var savedMonth = 0
    var savedYear = 0

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view: View = inflater.inflate(R.layout.fragment_profile, container, false)
        usernameTextView = view.findViewById(R.id.profileHead)
        datePicker = view.findViewById(R.id.datePicker)
        viewActivitiesButton = view.findViewById(R.id.viewActivitiesButton)
        detectedActivitiesHeader = view.findViewById(R.id.detectedActivitiesHead)
        detectedActivitiesListText = view.findViewById(R.id.detectedActivitiesListText)
        detectedActivitiesListText.movementMethod = ScrollingMovementMethod()

        dbHelper = DBHelper(requireContext())


        username = arguments?.getString("username").toString()
        val profileText = "Welcome, $username!"
        usernameTextView.text = profileText

        initialiseDatePicker()
        initialiseActivitiesButton()

        return view
    }


    private fun initialiseActivitiesButton() {
        viewActivitiesButton.setOnClickListener {
            val date = "$savedYear-${savedMonth+1}-$savedDay"
            val activities = dbHelper.getDayActivities(username, date)
            detectedActivitiesHeader.visibility = View.VISIBLE
            detectedActivitiesListText.visibility = View.VISIBLE
            detectedActivitiesHeader.text = "Activities on $date"
            detectedActivitiesListText.text = getActivitiesString(activities)
        }
    }


    private fun getActivitiesString(activities: List<String>): String {
        var activitiesString = StringBuilder()
        for (activity in activities) {
            activitiesString.append(activity)
            activitiesString.append("\n")
        }
        return activitiesString.toString()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun initialiseDatePicker() {
        today = Calendar.getInstance()
        savedDay = today.get(Calendar.DAY_OF_MONTH)
        savedMonth = today.get(Calendar.MONTH)
        savedYear = today.get(Calendar.YEAR)
        datePicker.init(
            today.get(Calendar.YEAR), today.get(Calendar.MONTH),
            today.get(Calendar.DAY_OF_MONTH)
        ) { view, year, month, day ->
            savedDay = day
            savedMonth = month
            savedYear = year
            Toast.makeText(
                activity,
                "Selected date: " + datePicker.dayOfMonth + "/" + datePicker.month + "/" + datePicker.year,
                Toast.LENGTH_SHORT
            ).show()

        }

        // set date picker listener
        datePicker.setOnDateChangedListener { view, year, monthOfYear, dayOfMonth ->
            savedDay = dayOfMonth
            savedMonth = monthOfYear
            savedYear = year
            Toast.makeText(
                activity,
                "Selected date: " + datePicker.dayOfMonth + "/" + datePicker.month + "/" + datePicker.year,
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    companion object {
    }
}